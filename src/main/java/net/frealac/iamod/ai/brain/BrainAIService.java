package net.frealac.iamod.ai.brain;

import com.google.gson.*;
import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.openai.OpenAiClient;

import java.io.IOException;

/**
 * AI Service for individual brain modules.
 * Each brain can use AI independently with optimized models for their specific tasks.
 *
 * BRAIN-SPECIFIC MODELS:
 * - EmotionalBrain: gpt-4o-mini (fast emotion analysis)
 * - MemoryBrain: gpt-4o-mini (fast memory search)
 * - SocialBrain: gpt-4o-mini (fast relationship analysis)
 * - GeneralBrain: gpt-4o-mini (comprehensive synthesis)
 *
 * All use gpt-4o-mini for optimal speed/cost in a game context.
 */
public class BrainAIService {

    private final OpenAiClient client;
    private final Gson gson;

    public BrainAIService() {
        this.client = new OpenAiClient();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Ask AI a simple question with a specific context.
     * Used by individual brain modules for specialized analysis.
     *
     * @param brainModuleName Name of the brain module making the request
     * @param question The question to ask
     * @param context Context information for the question
     * @param modelOverride Optional model override (null = use config)
     * @return AI response
     */
    public String askBrain(String brainModuleName, String question, String context, String modelOverride)
            throws IOException, InterruptedException {

        // Check if AI is enabled for this brain
        if (!BrainModelConfig.isAiEnabled(brainModuleName)) {
            IAMOD.LOGGER.warn("🧠 {} has AI disabled, returning default response", brainModuleName);
            return "AI processing disabled for " + brainModuleName;
        }

        JsonObject payload = new JsonObject();

        // Use configured model for this brain (or override)
        String model = modelOverride != null ? modelOverride : BrainModelConfig.getModel(brainModuleName);
        payload.addProperty("model", model);

        JsonArray messages = new JsonArray();

        // System prompt
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", buildBrainSpecificPrompt(brainModuleName, context));
        messages.add(system);

        // User question
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", question);
        messages.add(user);

        payload.add("messages", messages);
        payload.addProperty("temperature", BrainModelConfig.getTemperature(brainModuleName));
        payload.addProperty("max_tokens", BrainModelConfig.getMaxTokens(brainModuleName));

        IAMOD.LOGGER.debug("🧠 {} calling AI with model {} (temp={}, tokens={})",
            brainModuleName, model, BrainModelConfig.getTemperature(brainModuleName),
            BrainModelConfig.getMaxTokens(brainModuleName));

        String responseBody = client.sendChatRequest(payload);
        return extractContent(responseBody);
    }

    /**
     * Build brain-specific system prompt.
     */
    private String buildBrainSpecificPrompt(String brainModuleName, String context) {
        StringBuilder prompt = new StringBuilder();

        switch (brainModuleName) {
            case "EmotionalBrain":
                prompt.append("Tu es le cerveau ÉMOTIONNEL d'un villageois.\n");
                prompt.append("Tu analyses les ÉMOTIONS, l'HUMEUR et le STRESS.\n");
                prompt.append("Tu dois donner une réponse courte et directe sur l'état émotionnel.\n\n");
                prompt.append("Contexte émotionnel:\n").append(context);
                break;

            case "MemoryBrain":
                prompt.append("Tu es le cerveau MÉMOIRE d'un villageois.\n");
                prompt.append("Tu recherches dans les SOUVENIRS pour répondre aux questions.\n");
                prompt.append("Tu dois donner une réponse précise basée sur les souvenirs.\n\n");
                prompt.append("Souvenirs disponibles:\n").append(context);
                break;

            case "SocialBrain":
                prompt.append("Tu es le cerveau SOCIAL d'un villageois.\n");
                prompt.append("Tu analyses les RELATIONS et la CONFIANCE avec les joueurs.\n");
                prompt.append("Tu dois donner une réponse courte sur la relation.\n\n");
                prompt.append("Contexte social:\n").append(context);
                break;

            case "GeneralBrain":
                prompt.append("Tu es le cerveau GÉNÉRAL d'un villageois.\n");
                prompt.append("Tu COORDONNES tous les autres cerveaux (émotionnel, mémoire, social).\n");
                prompt.append("Tu SYNTHÉTISES leurs informations pour prendre la meilleure décision.\n\n");
                prompt.append("Contexte complet:\n").append(context);
                break;

            default:
                prompt.append("Tu es un cerveau spécialisé: ").append(brainModuleName).append("\n\n");
                prompt.append("Contexte:\n").append(context);
        }

        return prompt.toString();
    }


    /**
     * Extract content from OpenAI response.
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

    /**
     * Quick emotion analysis using AI.
     * Used by EmotionalBrain for complex emotion understanding.
     */
    public String analyzeEmotion(String situation, String currentMood, String currentStress)
            throws IOException, InterruptedException {
        String context = String.format(
            "Humeur actuelle: %s\nStress actuel: %s",
            currentMood, currentStress
        );

        return askBrain("EmotionalBrain",
            "Comment devrais-je me sentir face à cette situation: " + situation,
            context,
            null
        );
    }

    /**
     * Quick memory search using AI.
     * Used by MemoryBrain to find relevant memories.
     */
    public String searchMemory(String query, String allMemories)
            throws IOException, InterruptedException {
        return askBrain("MemoryBrain",
            "Recherche dans mes souvenirs: " + query,
            allMemories,
            null
        );
    }

    /**
     * Quick relationship analysis using AI.
     * Used by SocialBrain to understand relationships.
     */
    public String analyzeRelationship(String playerName, String interactionHistory)
            throws IOException, InterruptedException {
        return askBrain("SocialBrain",
            "Comment devrais-je percevoir " + playerName + "?",
            interactionHistory,
            null
        );
    }
}
