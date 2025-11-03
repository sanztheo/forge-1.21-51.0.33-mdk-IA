package net.frealac.iamod.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.frealac.iamod.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class OpenAiService {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final URI CHAT_URI = URI.create("https://api.openai.com/v1/chat/completions");

    private final HttpClient httpClient;

    public OpenAiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String ask(String userMessage, String who) throws IOException, InterruptedException {
        final String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY manquant: définissez-le dans run/config/iamod-common.toml ou comme variable d'environnement.");
        }

        final String model = (Config.openAiModel == null || Config.openAiModel.isBlank()) ? DEFAULT_MODEL : Config.openAiModel;

        JsonObject root = new JsonObject();
        root.addProperty("model", model);

        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "Tu es un assistant Minecraft amical et concis. Réponds en français si possible.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prefixWho(who) + userMessage);
        messages.add(user);

        root.add("messages", messages);
        root.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder(CHAT_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI HTTP " + response.statusCode() + ": " + trim(response.body(), 300));
        }

        return extractContent(response.body());
    }

    public String chat(List<ChatMessage> history) throws IOException, InterruptedException {
        final String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY manquant: définissez-le dans run/config/iamod-common.toml ou comme variable d'environnement.");
        }
        final String model = (Config.openAiModel == null || Config.openAiModel.isBlank()) ? DEFAULT_MODEL : Config.openAiModel;

        JsonObject root = new JsonObject();
        root.addProperty("model", model);

        JsonArray messages = new JsonArray();
        for (ChatMessage m : history) {
            JsonObject j = new JsonObject();
            j.addProperty("role", m.role);
            j.addProperty("content", m.content);
            messages.add(j);
        }
        root.add("messages", messages);
        root.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder(CHAT_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI HTTP " + response.statusCode() + ": " + trim(response.body(), 300));
        }
        return extractContent(response.body());
    }

    private static String getApiKey() {
        // Priorité au fichier de config Forge (run/config/iamod-common.toml), sinon variable d'environnement
        String fromConfig = Config.openAiApiKey;
        if (fromConfig != null && !fromConfig.isBlank()) return fromConfig.trim();
        String fromEnv = System.getenv("OPENAI_API_KEY");
        return fromEnv != null ? fromEnv.trim() : null;
    }

    private static String extractContent(String json) throws IOException {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray choices = obj.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) throw new IOException("Réponse OpenAI sans 'choices'.");
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null) throw new IOException("Réponse OpenAI sans 'message'.");
            JsonElement content = message.get("content");
            if (content == null) throw new IOException("Réponse OpenAI sans 'content'.");
            String text = content.getAsString();
            return trim(text, 512);
        } catch (RuntimeException ex) {
            throw new IOException("Impossible d'analyser la réponse OpenAI.", ex);
        }
    }

    private static String prefixWho(String who) {
        if (who == null || who.isBlank()) return "";
        return "[" + who + "] ";
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max)) + "…";
    }
}
