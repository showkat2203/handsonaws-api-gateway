package com.plm.chatbot.llm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicLlmProviderTest {

    private final AnthropicLlmProvider provider = new AnthropicLlmProvider("test-key", "claude-opus-4-8", 2048);

    @Test
    void buildRequestBody_includesSystemMessagesAndTools() throws Exception {
        List<LlmMessage> messages = List.of(LlmMessage.userText("Show the BOM for R-14"));
        List<ToolSpec> tools = List.of(new ToolSpec("get_bom", "Get BOM", Map.of("type", "object")));

        String json = provider.buildRequestBody("You are a helpful PLM assistant.", messages, tools);

        assertThat(json).contains("\"model\":\"claude-opus-4-8\"");
        assertThat(json).contains("\"system\":\"You are a helpful PLM assistant.\"");
        assertThat(json).contains("\"name\":\"get_bom\"");
        assertThat(json).contains("Show the BOM for R-14");
    }

    @Test
    void parseResponse_extractsTextBlocksAndStopReason() throws Exception {
        String body = """
                {
                  "id": "msg_1",
                  "content": [{"type": "text", "text": "Rack R-14 has 6 servers."}],
                  "stop_reason": "end_turn"
                }
                """;

        LlmResult result = provider.parseResponse(body);

        assertThat(result.stopReason()).isEqualTo("end_turn");
        assertThat(result.isToolUse()).isFalse();
        assertThat(result.textOrEmpty()).isEqualTo("Rack R-14 has 6 servers.");
    }

    @Test
    void parseResponse_extractsToolUseBlocks() throws Exception {
        String body = """
                {
                  "id": "msg_2",
                  "content": [{"type": "tool_use", "id": "tu_1", "name": "get_bom", "input": {"partId": "R-14"}}],
                  "stop_reason": "tool_use"
                }
                """;

        LlmResult result = provider.parseResponse(body);

        assertThat(result.isToolUse()).isTrue();
        ContentBlock.ToolUse toolUse = (ContentBlock.ToolUse) result.content().get(0);
        assertThat(toolUse.name()).isEqualTo("get_bom");
        assertThat(toolUse.input()).containsEntry("partId", "R-14");
    }
}
