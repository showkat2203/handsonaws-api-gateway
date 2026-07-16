package com.plm.chatbot.executor;

import com.plm.chatbot.llm.ToolSpec;
import com.plm.chatbot.tools.ToolDefinition;
import com.plm.chatbot.tools.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolExecutorTest {

    @Mock
    private ToolRegistry toolRegistry;

    @Test
    void execute_returnsSuccessTraceWithResult() {
        ToolDefinition tool = new ToolDefinition(
                new ToolSpec("get_part", "desc", Map.of()),
                args -> "PART-RESULT");
        when(toolRegistry.get("get_part")).thenReturn(tool);

        ToolExecutor executor = new ToolExecutor(toolRegistry);
        ToolCallTrace trace = executor.execute("get_part", Map.of("id", "PART-1"));

        assertThat(trace.error()).isNull();
        assertThat(trace.result()).isEqualTo("PART-RESULT");
        assertThat(trace.tool()).isEqualTo("get_part");
        assertThat(trace.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void execute_returnsFailureTraceWhenToolThrows() {
        ToolDefinition tool = new ToolDefinition(
                new ToolSpec("get_part", "desc", Map.of()),
                args -> { throw new RuntimeException("boom"); });
        when(toolRegistry.get("get_part")).thenReturn(tool);

        ToolExecutor executor = new ToolExecutor(toolRegistry);
        ToolCallTrace trace = executor.execute("get_part", Map.of());

        assertThat(trace.error()).isEqualTo("boom");
        assertThat(trace.result()).isNull();
    }

    @Test
    void execute_returnsFailureTraceForUnknownTool() {
        when(toolRegistry.get("nonexistent")).thenReturn(null);

        ToolExecutor executor = new ToolExecutor(toolRegistry);
        ToolCallTrace trace = executor.execute("nonexistent", Map.of());

        assertThat(trace.error()).contains("Unknown tool");
    }
}
