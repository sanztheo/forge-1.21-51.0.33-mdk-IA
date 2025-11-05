package net.frealac.iamod.ai.openai;

import com.google.gson.JsonObject;
import net.frealac.iamod.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Low-level HTTP client for OpenAI API.
 * Handles authentication and request/response.
 */
public class OpenAiClient {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final URI CHAT_URI = URI.create("https://api.openai.com/v1/chat/completions");

    private final HttpClient httpClient;

    public OpenAiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Send a chat completion request to OpenAI.
     * @param payload JSON payload to send
     * @return Response body as string
     */
    public String sendChatRequest(JsonObject payload) throws IOException, InterruptedException {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY missing: set it in run/config/iamod-common.toml or as environment variable");
        }

        // Ensure model is set
        if (!payload.has("model") || payload.get("model").getAsString().isBlank()) {
            String model = (Config.openAiModel == null || Config.openAiModel.isBlank()) ? DEFAULT_MODEL : Config.openAiModel;
            payload.addProperty("model", model);
        }

        HttpRequest request = HttpRequest.newBuilder(CHAT_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI HTTP " + response.statusCode() + ": " + trim(response.body(), 300));
        }

        return response.body();
    }

    /**
     * Get API key from config or environment variable.
     */
    public static String getApiKey() {
        // Priority: config file, then environment variable
        String fromConfig = Config.openAiApiKey;
        if (fromConfig != null && !fromConfig.isBlank()) {
            return fromConfig.trim();
        }
        String fromEnv = System.getenv("OPENAI_API_KEY");
        return fromEnv != null ? fromEnv.trim() : null;
    }

    /**
     * Get configured model name.
     */
    public static String getModel() {
        return (Config.openAiModel == null || Config.openAiModel.isBlank()) ? DEFAULT_MODEL : Config.openAiModel;
    }

    /**
     * Trim string to max length.
     */
    public static String trim(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max)) + "…";
    }
}
