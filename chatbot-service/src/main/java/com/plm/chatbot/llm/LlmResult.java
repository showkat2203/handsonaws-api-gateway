package com.plm.chatbot.llm;

import java.util.List;

/** stopReason is "tool_use" when the model wants to invoke one or more tools, "end_turn" otherwise. */
public record LlmResult(List<ContentBlock> content, String stopReason) {

    public boolean isToolUse() {
        return "tool_use".equals(stopReason);
    }

    public String textOrEmpty() {
        return content.stream()
                .filter(ContentBlock.Text.class::isInstance)
                .map(ContentBlock.Text.class::cast)
                .map(ContentBlock.Text::text)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    }
}
