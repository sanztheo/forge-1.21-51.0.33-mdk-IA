package net.frealac.iamod.ai.openai;

import com.google.gson.*;
import net.frealac.iamod.ai.ChatMessage;

import java.io.IOException;
import java.util.List;

/**
 * Simple chat service for conversational AI.
 * Handles basic chat completions without special formatting.
 */
public class OpenAiChatService {

    private final OpenAiClient client;

    public OpenAiChatService() {
        this.client = new OpenAiClient();
    }

    /**
     * Simple question-answer chat.
     * @param userMessage User's message
     * @param systemPrompt System prompt (optional)
     * @return AI response
     */
    public String ask(String userMessage, String systemPrompt) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();

        JsonArray messages = new JsonArray();

        // System message
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content", systemPrompt);
            messages.add(system);
        }

        // User message
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);

        payload.add("messages", messages);
        payload.addProperty("temperature", 0.7);
        payload.addProperty("max_tokens", 500);

        String responseBody = client.sendChatRequest(payload);
        return extractContent(responseBody);
    }

    /**
     * Multi-turn conversation with history.
     * @param history List of chat messages
     * @return AI response
     */
    public String chat(List<ChatMessage> history) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();

        JsonArray messages = new JsonArray();
        for (ChatMessage m : history) {
            JsonObject j = new JsonObject();
            j.addProperty("role", m.role);
            j.addProperty("content", m.content);
            messages.add(j);
        }

        payload.add("messages", messages);
        payload.addProperty("temperature", 0.7);
        payload.addProperty("max_tokens", 500);

        String responseBody = client.sendChatRequest(payload);
        return extractContent(responseBody);
    }

    /**
     * Extract content from OpenAI response JSON.
     */
    private String extractContent(String json) throws IOException {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray choices = obj.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IOException("OpenAI response without 'choices'");
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null) {
                throw new IOException("OpenAI response without 'message'");
            }
            JsonElement content = message.get("content");
            if (content == null) {
                throw new IOException("OpenAI response without 'content'");
            }
            return content.getAsString().trim();
        } catch (RuntimeException ex) {
            throw new IOException("Cannot parse OpenAI response", ex);
        }
    }
}
