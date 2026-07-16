package com.plm.chatbot.controller;

import com.plm.chatbot.dto.ChatMessageDto;
import com.plm.chatbot.dto.ChatRequest;
import com.plm.chatbot.dto.ChatResponse;
import com.plm.chatbot.executor.ToolCallTrace;
import com.plm.chatbot.executor.ToolExecutor;
import com.plm.chatbot.llm.ContentBlock;
import com.plm.chatbot.llm.LlmMessage;
import com.plm.chatbot.llm.LlmProvider;
import com.plm.chatbot.llm.LlmResult;
import com.plm.chatbot.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the tool-calling conversation loop: sends the conversation + available tools to
 * the LLM, executes any tool calls it requests against the internal service layer, feeds the
 * results back, and repeats until the model produces a final text answer (or the iteration cap
 * is hit). This is the chatbot's entry point into "intent routing" — the LLM itself decides,
 * per turn, whether a question needs a graph-backed tool (BOM/where-used) or a
 * DynamoDB-backed tool (attribute/status lookups), simply by picking from the tool list.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_TOOL_ITERATIONS = 6;

    private static final String SYSTEM_PROMPT = """
            You are the assistant for a Product Lifecycle Management (PLM) system that tracks \
            data center hardware: parts (servers, racks, PSUs, NICs, cables), their Bill-of-Materials \
            (BOM) hierarchy, suppliers, lifecycle state (design -> active -> eol), and Engineering \
            Change Orders (ECOs).

            Answer questions using the provided tools rather than prior knowledge -- the tools are the \
            only source of truth about this system's current data. Use get_bom / where_used for \
            structural questions ("what does rack R-14 contain?", "what uses PSU-2200?"). Use \
            search_parts / get_part / supplier_parts / list_suppliers for attribute and status lookups \
            ("which parts are EOL?", "which parts from supplier Acme are EOL?" -- look up the supplier's \
            parts, then filter by lifecycleState yourself). Use get_change_order / list_change_orders \
            for ECO questions. If a tool call fails or returns nothing, say so plainly rather than \
            guessing. Keep answers concise and reference specific part/supplier/ECO ids.
            """;

    private final LlmProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;

    public ChatService(LlmProvider llmProvider, ToolRegistry toolRegistry, ToolExecutor toolExecutor) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
    }

    public ChatResponse chat(ChatRequest request) {
        List<LlmMessage> messages = new ArrayList<>();
        for (ChatMessageDto turn : request.history()) {
            messages.add(new LlmMessage(turn.role(), List.of(new ContentBlock.Text(turn.content()))));
        }
        messages.add(LlmMessage.userText(request.message()));

        List<ToolCallTrace> traces = new ArrayList<>();

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            LlmResult result = llmProvider.complete(SYSTEM_PROMPT, messages, toolRegistry.specs());
            messages.add(LlmMessage.assistant(result.content()));

            if (!result.isToolUse()) {
                return new ChatResponse(result.textOrEmpty(), traces);
            }

            List<ContentBlock> toolResults = new ArrayList<>();
            for (ContentBlock block : result.content()) {
                if (block instanceof ContentBlock.ToolUse toolUse) {
                    log.info("Chatbot invoking tool={} params={}", toolUse.name(), toolUse.input());
                    ToolCallTrace trace = toolExecutor.execute(toolUse.name(), toolUse.input());
                    traces.add(trace);
                    boolean failed = trace.error() != null;
                    String content = failed ? "Error: " + trace.error() : String.valueOf(trace.result());
                    toolResults.add(new ContentBlock.ToolResult(toolUse.id(), content, failed));
                }
            }
            messages.add(LlmMessage.userToolResults(toolResults));
        }

        log.warn("Tool-call loop exceeded {} iterations without a final answer", MAX_TOOL_ITERATIONS);
        return new ChatResponse(
                "I wasn't able to finish answering that within the allotted tool-call budget. Please try a more specific question.",
                traces);
    }
}
