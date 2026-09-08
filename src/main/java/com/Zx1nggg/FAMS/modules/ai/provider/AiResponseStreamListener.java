package com.Zx1nggg.FAMS.modules.ai.provider;

@FunctionalInterface
public interface AiResponseStreamListener {
    AiResponseStreamListener NOOP = delta -> { };

    void onTextDelta(String delta);
}
