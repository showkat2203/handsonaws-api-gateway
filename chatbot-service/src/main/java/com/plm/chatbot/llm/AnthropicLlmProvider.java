package com.plm.chatbot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the Anthropic Messages API (POST /v1/messages) directly over HTTP, per the plumbing
 * this project's chatbot layer is specified to use (java.net.http.HttpClient, not the Anthropic
 * SDK). Model and API key are injected via env/config so the provider is swappable — see
 * {@link LlmProvider}.
 */
public class AnthropicLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmProvider.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AnthropicLlmProvider(String apiKey, String model, int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LlmResult complete(String systemPrompt, List<LlmMessage> messages, List<ToolSpec> tools) {
        try {
            String requestBody = buildRequestBody(systemPrompt, messages, tools);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Anthropic API error: status={} body={}", response.statusCode(), response.body());
                throw new LlmException("Anthropic API returned status " + response.statusCode() + ": " + response.body());
            }
            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new LlmException("Failed to call Anthropic API", e);
        }
    }

    String buildRequestBody(String systemPrompt, List<LlmMessage> messages, List<ToolSpec> tools) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            root.put("system", systemPrompt);
        }

        ArrayNode messagesNode = root.putArray("messages");
        for (LlmMessage message : messages) {
            ObjectNode messageNode = messagesNode.addObject();
            messageNode.put("role", message.role());
            ArrayNode contentNode = messageNode.putArray("content");
            for (ContentBlock block : message.content()) {
                contentNode.add(toJson(block));
            }
        }

        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsNode = root.putArray("tools");
            for (ToolSpec tool : tools) {
                ObjectNode toolNode = toolsNode.addObject();
                toolNode.put("name", tool.name());
                toolNode.put("description", tool.description());
                toolNode.set("input_schema", objectMapper.valueToTree(tool.inputSchema()));
            }
        }

        return objectMapper.writeValueAsString(root);
    }

    private ObjectNode toJson(ContentBlock block) {
        ObjectNode node = objectMapper.createObjectNode();
        switch (block) {
            case ContentBlock.Text t -> {
                node.put("type", "text");
                node.put("text", t.text());
            }
            case ContentBlock.ToolUse tu -> {
                node.put("type", "tool_use");
                node.put("id", tu.id());
                node.put("name", tu.name());
                node.set("input", objectMapper.valueToTree(tu.input()));
            }
            case ContentBlock.ToolResult tr -> {
                node.put("type", "tool_result");
                node.put("tool_use_id", tr.toolUseId());
                node.put("content", tr.content());
                if (tr.isError()) {
                    node.put("is_error", true);
                }
            }
        }
        return node;
    }

    LlmResult parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        String stopReason = root.path("stop_reason").asText(null);
        List<ContentBlock> blocks = new ArrayList<>();
        for (JsonNode blockNode : root.path("content")) {
            String type = blockNode.path("type").asText();
            switch (type) {
                case "text" -> blocks.add(new ContentBlock.Text(blockNode.path("text").asText()));
                case "tool_use" -> blocks.add(new ContentBlock.ToolUse(
                        blockNode.path("id").asText(),
                        blockNode.path("name").asText(),
                        toMap(blockNode.path("input"))));
                default -> log.debug("Ignoring unsupported content block type: {}", type);
            }
        }
        return new LlmResult(blocks, stopReason);
    }

    private Map<String, Object> toMap(JsonNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            map.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class));
        }
        return map;
    }
}
