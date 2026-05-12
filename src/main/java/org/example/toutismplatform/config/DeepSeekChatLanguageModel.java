package org.example.toutismplatform.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DeepSeekChatLanguageModel implements ChatLanguageModel {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEFAULT_MODEL_NAME = "deepseek-chat";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_MAX_TOKENS = 4096;
    private static final double DEFAULT_TOP_P = 1.0;

    private final String apiKey;
    private final URI chatCompletionsUri;
    private final String modelName;
    private final double temperature;
    private final Duration timeout;
    private final Integer maxTokens;
    private final Double topP;
    private final HttpClient httpClient;

    DeepSeekChatLanguageModel(String apiKey, String baseUrl, String modelName, double temperature, Duration timeout, Integer maxTokens, Double topP) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("DeepSeek API key is not configured. Set deepseek.api-key or DEEPSEEK_API_KEY.");
        }
        this.apiKey = apiKey.trim();
        this.chatCompletionsUri = buildChatCompletionsUri(baseUrl);
        this.modelName = StringUtils.hasText(modelName) ? modelName.trim() : DEFAULT_MODEL_NAME;
        this.temperature = temperature;
        this.timeout = validTimeout(timeout);
        this.maxTokens = maxTokens != null && maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS;
        this.topP = topP != null && topP > 0 ? topP : DEFAULT_TOP_P;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(buildRequestBody(messages));
            HttpRequest request = HttpRequest.newBuilder(chatCompletionsUri)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("DeepSeek API request failed: HTTP "
                        + response.statusCode() + " " + extractErrorMessage(response.body()));
            }

            return Response.from(AiMessage.from(extractAssistantContent(response.body())));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call DeepSeek API.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DeepSeek API request was interrupted.", e);
        }
    }

    private Map<String, Object> buildRequestBody(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("At least one chat message is required.");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("top_p", topP);
        requestBody.put("messages", toDeepSeekMessages(messages));
        return requestBody;
    }

    private List<Map<String, String>> toDeepSeekMessages(List<ChatMessage> messages) {
        List<Map<String, String>> deepSeekMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message == null) {
                continue;
            }
            Map<String, String> deepSeekMessage = new LinkedHashMap<>();
            deepSeekMessage.put("role", toDeepSeekRole(message.type()));
            deepSeekMessage.put("content", message.text() == null ? "" : message.text());
            deepSeekMessages.add(deepSeekMessage);
        }
        if (deepSeekMessages.isEmpty()) {
            throw new IllegalArgumentException("At least one non-null chat message is required.");
        }
        return deepSeekMessages;
    }

    private String toDeepSeekRole(ChatMessageType messageType) {
        if (messageType == ChatMessageType.SYSTEM) {
            return "system";
        }
        if (messageType == ChatMessageType.AI) {
            return "assistant";
        }
        return "user";
    }

    private String extractAssistantContent(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("DeepSeek API response did not contain choices.");
        }

        JsonNode content = choices.get(0).path("message").path("content");
        if (!content.isTextual()) {
            throw new IllegalStateException("DeepSeek API response did not contain assistant content.");
        }
        return content.asText();
    }

    private static URI buildChatCompletionsUri(String baseUrl) {
        String normalizedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_BASE_URL;
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        if (!normalizedBaseUrl.endsWith("/chat/completions")) {
            normalizedBaseUrl += "/chat/completions";
        }
        return URI.create(normalizedBaseUrl);
    }

    private static Duration validTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return DEFAULT_TIMEOUT;
        }
        return timeout;
    }

    private String extractErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "";
        }
        try {
            JsonNode errorMessage = OBJECT_MAPPER.readTree(responseBody).path("error").path("message");
            if (errorMessage.isTextual()) {
                return trimForException(errorMessage.asText());
            }
        } catch (Exception ignored) {
            // Fall through to the raw response preview below.
        }
        return trimForException(responseBody);
    }

    private String trimForException(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300);
    }
}