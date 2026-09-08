package com.Zx1nggg.FAMS.modules.ai.service;

public interface AiAgentListener {
    AiAgentListener NOOP = new AiAgentListener() { };

    default void onStatus(String message) { }

    default void onTextDelta(String delta) { }

    default void onToolStart(String toolName) { }

    default void onToolEnd(String toolName, boolean success) { }
}
