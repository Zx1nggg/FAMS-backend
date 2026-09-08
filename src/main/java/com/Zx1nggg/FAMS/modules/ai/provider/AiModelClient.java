package com.Zx1nggg.FAMS.modules.ai.provider;

import com.Zx1nggg.FAMS.modules.ai.model.AiModelTurn;

import java.util.List;
import java.util.Map;

public interface AiModelClient {
    AiModelTurn createResponse(String instructions, List<Object> input,
                               List<Map<String, Object>> tools, String safetyIdentifier);

    default AiModelTurn streamResponse(String instructions, List<Object> input,
                                       List<Map<String, Object>> tools, String safetyIdentifier,
                                       AiResponseStreamListener listener) {
        AiModelTurn turn = createResponse(instructions, input, tools, safetyIdentifier);
        if (turn.text() != null && !turn.text().isEmpty()) listener.onTextDelta(turn.text());
        return turn;
    }
}
