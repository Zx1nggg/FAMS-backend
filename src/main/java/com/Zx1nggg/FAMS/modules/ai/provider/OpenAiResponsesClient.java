package com.Zx1nggg.FAMS.modules.ai.provider;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.ai.config.AiProperties;
import com.Zx1nggg.FAMS.modules.ai.model.AiCitation;
import com.Zx1nggg.FAMS.modules.ai.model.AiFunctionCall;
import com.Zx1nggg.FAMS.modules.ai.model.AiModelTurn;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiResponsesClient implements AiModelClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiResponsesClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiModelTurn createResponse(String instructions, List<Object> input,
                                      List<Map<String, Object>> tools, String safetyIdentifier) {
        if (!properties.isReady()) {
            throw new BusinessException(503, "AI 服务尚未配置，请设置 AI_ENABLED、AI_API_KEY 和 AI_MODEL");
        }

        try {
            JsonNode root = client(false).post().uri("/responses")
                    .body(requestBody(instructions, input, tools, safetyIdentifier, false))
                    .retrieve().body(JsonNode.class);
            if (root == null) throw new BusinessException(502, "模型服务返回了空响应");
            return parse(root);
        } catch (RestClientResponseException e) {
            throw upstreamError(e.getStatusCode().value());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(502, "AI 服务连接失败，请稍后重试");
        }
    }

    @Override
    public AiModelTurn streamResponse(String instructions, List<Object> input,
                                      List<Map<String, Object>> tools, String safetyIdentifier,
                                      AiResponseStreamListener listener) {
        if (!properties.isReady()) {
            throw new BusinessException(503, "AI 服务尚未配置，请设置 AI_ENABLED、AI_API_KEY 和 AI_MODEL");
        }
        try {
            return client(true).post().uri("/responses")
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .body(requestBody(instructions, input, tools, safetyIdentifier, true))
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status >= 400) throw upstreamError(status);
                        JsonNode completedResponse = null;
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                                response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) continue;
                                String data = line.substring(5).stripLeading();
                                if (data.isBlank() || "[DONE]".equals(data)) continue;
                                JsonNode event = objectMapper.readTree(data);
                                String type = event.path("type").asText();
                                if ("response.output_text.delta".equals(type)) {
                                    String delta = event.path("delta").asText();
                                    if (!delta.isEmpty()) listener.onTextDelta(delta);
                                } else if ("response.completed".equals(type)) {
                                    completedResponse = event.path("response").deepCopy();
                                } else if ("response.failed".equals(type) || "error".equals(type)) {
                                    throw new BusinessException(502, "AI 上游流式响应失败");
                                } else if ("response.incomplete".equals(type)) {
                                    throw new BusinessException(502, "AI 上游响应未完整生成");
                                }
                            }
                        }
                        if (completedResponse == null || completedResponse.isMissingNode()) {
                            throw new BusinessException(502, "AI 流式响应意外中断");
                        }
                        return parse(completedResponse);
                    });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(502, "AI 流式服务连接失败，请稍后重试");
        }
    }

    private RestClient client(boolean stream) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(5,
                stream ? properties.getStreamTimeoutSeconds() : properties.getRequestTimeoutSeconds()));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    private Map<String, Object> requestBody(String instructions, List<Object> input,
                                            List<Map<String, Object>> tools,
                                            String safetyIdentifier, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("instructions", instructions);
        body.put("input", input);
        body.put("tools", tools);
        body.put("tool_choice", "auto");
        body.put("parallel_tool_calls", false);
        body.put("max_output_tokens", properties.getMaxOutputTokens());
        body.put("store", false);
        body.put("stream", stream);
        body.put("safety_identifier", safetyIdentifier);
        List<String> include = new ArrayList<>();
        include.add("reasoning.encrypted_content");
        if (tools.stream().anyMatch(tool -> "file_search".equals(tool.get("type")))) {
            include.add("file_search_call.results");
        }
        body.put("include", include);
        return body;
    }

    private BusinessException upstreamError(int status) {
        if (status == 401 || status == 403) {
            return new BusinessException(503, "AI 服务凭证无效或无权访问所选模型");
        }
        if (status == 429) return new BusinessException(429, "AI 服务请求过多，请稍后重试");
        return new BusinessException(502, "AI 上游服务异常（HTTP " + status + "）");
    }

    private AiModelTurn parse(JsonNode root) {
        List<JsonNode> outputItems = new ArrayList<>();
        List<AiFunctionCall> calls = new ArrayList<>();
        List<AiCitation> citations = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        JsonNode output = root.path("output");
        if (output.isArray()) {
            output.forEach(item -> {
                outputItems.add(item.deepCopy());
                if ("function_call".equals(item.path("type").asText())) {
                    calls.add(new AiFunctionCall(
                            item.path("call_id").asText(),
                            item.path("name").asText(),
                            item.path("arguments").asText("{}")));
                }
                if ("message".equals(item.path("type").asText())) {
                    JsonNode content = item.path("content");
                    if (content.isArray()) content.forEach(part -> {
                        if ("output_text".equals(part.path("type").asText())) {
                            if (!text.isEmpty()) text.append('\n');
                            text.append(part.path("text").asText());
                            JsonNode annotations = part.path("annotations");
                            if (annotations.isArray()) annotations.forEach(annotation -> {
                                if ("file_citation".equals(annotation.path("type").asText())) {
                                    AiCitation citation = new AiCitation(
                                            annotation.path("file_id").asText(null),
                                            annotation.path("filename").asText("知识库文档"));
                                    if (!citations.contains(citation)) citations.add(citation);
                                }
                            });
                        }
                    });
                }
            });
        }

        JsonNode usage = root.path("usage");
        return new AiModelTurn(
                root.path("id").asText(null), text.toString(), outputItems, calls, citations,
                integerOrNull(usage.get("input_tokens")),
                integerOrNull(usage.get("output_tokens")),
                integerOrNull(usage.get("total_tokens")));
    }

    private Integer integerOrNull(JsonNode node) {
        return node == null || node.isNull() || !node.canConvertToInt() ? null : node.intValue();
    }
}
