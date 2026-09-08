package com.Zx1nggg.FAMS.ai;

import com.Zx1nggg.FAMS.modules.ai.config.AiProperties;
import com.Zx1nggg.FAMS.modules.ai.model.AiModelTurn;
import com.Zx1nggg.FAMS.modules.ai.provider.OpenAiResponsesClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiResponsesClientStreamingTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void parsesTextDeltasCompletionUsageAndFileCitations() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String completed = """
                    {"type":"response.completed","response":{"id":"resp_test","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"水质正常","annotations":[{"type":"file_citation","file_id":"file_1","filename":"水质规范.pdf"}]}]}],"usage":{"input_tokens":12,"output_tokens":5,"total_tokens":17}}}
                    """.strip();
            String events = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"水质\"}\n\n"
                    + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"正常\"}\n\n"
                    + "data: " + completed + "\n\n";
            byte[] bytes = events.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        OpenAiResponsesClient client = new OpenAiResponsesClient(properties, new ObjectMapper());
        List<String> deltas = new ArrayList<>();

        AiModelTurn turn = client.streamResponse("instructions", List.of(), List.of(), "safe-user", deltas::add);

        assertThat(deltas).containsExactly("水质", "正常");
        assertThat(turn.text()).isEqualTo("水质正常");
        assertThat(turn.totalTokens()).isEqualTo(17);
        assertThat(turn.citations()).hasSize(1);
        assertThat(turn.citations().getFirst().filename()).isEqualTo("水质规范.pdf");
        assertThat(requestBody.get()).contains("\"stream\":true", "\"store\":false");
    }
}
