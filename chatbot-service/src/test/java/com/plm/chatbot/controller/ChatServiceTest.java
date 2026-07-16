package com.plm.chatbot.controller;

import com.plm.chatbot.dto.ChatRequest;
import com.plm.chatbot.dto.ChatResponse;
import com.plm.chatbot.executor.ToolCallTrace;
import com.plm.chatbot.executor.ToolExecutor;
import com.plm.chatbot.llm.ContentBlock;
import com.plm.chatbot.llm.LlmMessage;
import com.plm.chatbot.llm.LlmProvider;
import com.plm.chatbot.llm.LlmResult;
import com.plm.chatbot.llm.ToolSpec;
import com.plm.chatbot.tools.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private LlmProvider llmProvider;
    @Mock
    private ToolRegistry toolRegistry;
    @Mock
    private ToolExecutor toolExecutor;

    @Test
    void chat_returnsDirectAnswerWhenNoToolUseRequested() {
        when(toolRegistry.specs()).thenReturn(List.of());
        when(llmProvider.complete(any(), any(), any())).thenReturn(
                new LlmResult(List.of(new ContentBlock.Text("Rack R-14 contains 6 servers.")), "end_turn"));

        ChatService service = new ChatService(llmProvider, toolRegistry, toolExecutor);
        ChatResponse response = service.chat(new ChatRequest("Show the BOM for rack R-14", List.of()));

        assertThat(response.answer()).isEqualTo("Rack R-14 contains 6 servers.");
        assertThat(response.toolCalls()).isEmpty();
        verify(toolExecutor, never()).execute(any(), any());
    }

    @Test
    void chat_executesToolThenReturnsFollowUpAnswer() {
        when(toolRegistry.specs()).thenReturn(List.of(new ToolSpec("get_bom", "desc", Map.of())));

        LlmResult toolUseResult = new LlmResult(
                List.of(new ContentBlock.ToolUse("tu_1", "get_bom", Map.of("partId", "R-14"))), "tool_use");
        LlmResult finalResult = new LlmResult(List.of(new ContentBlock.Text("Rack R-14 has 6 servers.")), "end_turn");
        when(llmProvider.complete(any(), any(), any())).thenReturn(toolUseResult, finalResult);

        ToolCallTrace trace = ToolCallTrace.success("get_bom", Map.of("partId", "R-14"), "bom-tree", 5);
        when(toolExecutor.execute("get_bom", Map.of("partId", "R-14"))).thenReturn(trace);

        ChatService service = new ChatService(llmProvider, toolRegistry, toolExecutor);
        ChatResponse response = service.chat(new ChatRequest("Show the BOM for rack R-14", List.of()));

        assertThat(response.answer()).isEqualTo("Rack R-14 has 6 servers.");
        assertThat(response.toolCalls()).containsExactly(trace);
        verify(llmProvider, times(2)).complete(any(), any(), any());
    }

    @Test
    void chat_stopsAfterMaxIterationsWithoutFinalAnswer() {
        when(toolRegistry.specs()).thenReturn(List.of(new ToolSpec("get_bom", "desc", Map.of())));
        LlmResult toolUseResult = new LlmResult(
                List.of(new ContentBlock.ToolUse("tu_1", "get_bom", Map.of("partId", "R-14"))), "tool_use");
        when(llmProvider.complete(any(), any(), any())).thenReturn(toolUseResult);
        when(toolExecutor.execute(any(), any()))
                .thenReturn(ToolCallTrace.success("get_bom", Map.of(), "x", 1));

        ChatService service = new ChatService(llmProvider, toolRegistry, toolExecutor);
        ChatResponse response = service.chat(new ChatRequest("loop forever", List.of()));

        assertThat(response.answer()).contains("tool-call budget");
        verify(llmProvider, times(6)).complete(any(), any(), any());
    }

    @Test
    void chat_seedsHistoryIntoConversation() {
        when(toolRegistry.specs()).thenReturn(List.of());
        when(llmProvider.complete(any(), any(), any())).thenReturn(
                new LlmResult(List.of(new ContentBlock.Text("ok")), "end_turn"));

        ChatService service = new ChatService(llmProvider, toolRegistry, toolExecutor);
        service.chat(new ChatRequest("follow up",
                List.of(new com.plm.chatbot.dto.ChatMessageDto("user", "earlier question"),
                        new com.plm.chatbot.dto.ChatMessageDto("assistant", "earlier answer"))));

        // The captured list is the same mutable instance ChatService keeps appending to, so by
        // the time we inspect it here it also contains the assistant's final reply appended
        // after the LLM call returned: 2 history turns + current message + final assistant reply.
        org.mockito.ArgumentCaptor<List<LlmMessage>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(llmProvider).complete(any(), captor.capture(), any());
        assertThat(captor.getValue()).hasSize(4);
        assertThat(captor.getValue().get(0).content().get(0))
                .isEqualTo(new ContentBlock.Text("earlier question"));
        assertThat(captor.getValue().get(2).content().get(0))
                .isEqualTo(new ContentBlock.Text("follow up"));
    }
}
