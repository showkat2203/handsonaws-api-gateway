package com.plm.chatbot.executor;

import com.plm.chatbot.tools.ToolDefinition;
import com.plm.chatbot.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Looks up and invokes a tool by name, timing the call and structured-logging every invocation
 * (tool name, params, duration, outcome) regardless of success or failure.
 */
@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolRegistry toolRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public ToolCallTrace execute(String toolName, Map<String, Object> args) {
        long start = System.currentTimeMillis();
        ToolDefinition tool = toolRegistry.get(toolName);

        MDC.put("tool", toolName);
        MDC.put("toolParams", String.valueOf(args));
        try {
            if (tool == null) {
                String error = "Unknown tool: " + toolName;
                log.warn("Tool call failed: {}", error);
                return ToolCallTrace.failure(toolName, args, error, System.currentTimeMillis() - start);
            }

            Object result = tool.execute().apply(args);
            long duration = System.currentTimeMillis() - start;
            MDC.put("durationMs", String.valueOf(duration));
            log.info("Tool call succeeded");
            return ToolCallTrace.success(toolName, args, result, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            MDC.put("durationMs", String.valueOf(duration));
            log.warn("Tool call failed: {}", e.getMessage());
            return ToolCallTrace.failure(toolName, args, e.getMessage(), duration);
        } finally {
            MDC.remove("tool");
            MDC.remove("toolParams");
            MDC.remove("durationMs");
        }
    }
}
