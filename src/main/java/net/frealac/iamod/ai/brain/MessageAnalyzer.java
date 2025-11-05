package net.frealac.iamod.ai.brain;

import com.google.gson.*;
import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.openai.OpenAiClient;

import java.io.IOException;

/**
 * Analyzes player messages using AI to determine their emotional impact.
 * Uses gpt-3.5-turbo for FAST analysis with STRICT JSON format.
 *
 * This is a REAL AI brain that understands context, not keyword matching!
 */
public class MessageAnalyzer {

    private static final OpenAiClient client = new OpenAiClient();
    private static final Gson gson = new GsonBuilder().create();

    // Model for message analysis (fast and cheap)
    private static final String ANALYSIS_MODEL = "gpt-3.5-turbo";

    /**
     * Analyze a message using AI to determine its emotional impact.
     * Returns a MessageImpact with scores for different dimensions.
     *
     * Uses STRICT JSON mode for reliable parsing.
     *
     * @param message The player's message
     * @param currentMood Current villager mood (-1.0 to 1.0) for mood-congruent processing
     * @param currentStress Current villager stress (0.0 to 1.0)
     */
    public static MessageImpact analyzeMessage(String message, double currentMood, double currentStress) {
        if (message == null || message.isEmpty()) {
            return new MessageImpact();
        }

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("model", ANALYSIS_MODEL);

            JsonArray messages = new JsonArray();

            // System prompt for analysis WITH mood-congruent context
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content", buildAnalysisPrompt(currentMood, currentStress));
            messages.add(system);

            // User message to analyze
            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", message);
            messages.add(user);

            payload.add("messages", messages);

            // Use config from BrainModelConfig
            payload.addProperty("temperature", BrainModelConfig.getTemperature("MessageAnalyzer"));
            payload.addProperty("max_tokens", BrainModelConfig.getMaxTokens("MessageAnalyzer"));

            // STRICT JSON mode
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_object");
            payload.add("response_format", responseFormat);

            IAMOD.LOGGER.debug("🧠 MessageAnalyzer: Analyzing with AI...");

            String responseBody = client.sendChatRequest(payload);
            String content = extractContent(responseBody);

            // Parse JSON response
            return parseImpact(content);

        } catch (Exception e) {
            IAMOD.LOGGER.error("Failed to analyze message with AI", e);
            return new MessageImpact(); // Fallback to neutral
        }
    }

    /**
     * Build analysis system prompt with MOOD-CONGRUENT PROCESSING.
     * SCIENTIFIC BASIS: Current emotional state biases interpretation of new events.
     *
     * @param currentMood Villager's current mood (-1.0 to 1.0)
     * @param currentStress Villager's current stress (0.0 to 1.0)
     */
    private static String buildAnalysisPrompt(double currentMood, double currentStress) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Tu es un analyseur d'émotions. Analyse le message du joueur et réponds en JSON STRICT.\n\n");

        // MOOD-CONGRUENT PROCESSING CONTEXT
        prompt.append("=== ÉTAT ÉMOTIONNEL ACTUEL DU VILLAGEOIS ===\n");
        prompt.append(String.format("Humeur actuelle: %.2f ", currentMood));
        if (currentMood < -0.5) {
            prompt.append("(TRÈS EN COLÈRE/TRISTE - Interprète les messages négativement)\n");
            prompt.append("BIAIS: Quand on est en colère, même les compliments semblent sarcastiques.\n");
            prompt.append("EXEMPLE: 'Tu es génial' → Perçu comme 'Tu te moques de moi?'\n\n");
        } else if (currentMood < -0.2) {
            prompt.append("(Irrité - Tendance à interpréter négativement)\n");
            prompt.append("BIAIS: Messages neutres perçus comme légèrement négatifs.\n\n");
        } else if (currentMood > 0.5) {
            prompt.append("(TRÈS JOYEUX - Interprète positivement)\n");
            prompt.append("BIAIS: Tendance à voir le positif même dans messages ambigus.\n\n");
        } else {
            prompt.append("(Neutre)\n\n");
        }

        prompt.append(String.format("Stress actuel: %.2f ", currentStress));
        if (currentStress > 0.7) {
            prompt.append("(TRÈS STRESSÉ - Sensible aux menaces)\n");
            prompt.append("BIAIS: Messages perçus comme plus agressifs/menaçants.\n\n");
        } else {
            prompt.append("(Calme)\n\n");
        }

        prompt.append("=== INSTRUCTIONS D'ANALYSE ===\n");
        prompt.append("Analyse le message EN TENANT COMPTE de l'état émotionnel actuel.\n");
        prompt.append("Si le villageois est en colère, un compliment peut être perçu comme sarcastique.\n");
        prompt.append("Si stressé, un message neutre peut sembler menaçant.\n\n");

        prompt.append("Détecte:\n");
        prompt.append("- positiveImpact: 0.0 à 1.0 (compliments, gentillesse)\n");
        prompt.append("- negativeImpact: 0.0 à 1.0 (insultes, méchanceté)\n");
        prompt.append("- affectionImpact: 0.0 à 1.0 (amour, amitié, affection)\n");
        prompt.append("- aggressionImpact: 0.0 à 1.0 (violence, menaces, agression)\n");
        prompt.append("- overallSentiment: -1.0 à 1.0 (sentiment global APRÈS biais émotionnel)\n\n");

        prompt.append("Format JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"positiveImpact\": 0.5,\n");
        prompt.append("  \"negativeImpact\": 0.0,\n");
        prompt.append("  \"affectionImpact\": 0.3,\n");
        prompt.append("  \"aggressionImpact\": 0.0,\n");
        prompt.append("  \"overallSentiment\": 0.8,\n");
        prompt.append("  \"reasoning\": \"Message positif mais villageois en colère donc perçu comme sarcastique\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * Backward compatibility: analyze without mood context.
     */
    public static MessageImpact analyzeMessage(String message) {
        return analyzeMessage(message, 0.0, 0.3); // Default neutral mood
    }

    /**
     * Extract content from OpenAI response.
     */
    private static String extractContent(String json) throws IOException {
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
     * Parse impact from JSON response.
     */
    private static MessageImpact parseImpact(String jsonContent) {
        try {
            JsonObject obj = JsonParser.parseString(jsonContent).getAsJsonObject();

            MessageImpact impact = new MessageImpact();
            impact.positiveImpact = obj.has("positiveImpact") ? obj.get("positiveImpact").getAsDouble() : 0.0;
            impact.negativeImpact = obj.has("negativeImpact") ? obj.get("negativeImpact").getAsDouble() : 0.0;
            impact.affectionImpact = obj.has("affectionImpact") ? obj.get("affectionImpact").getAsDouble() : 0.0;
            impact.aggressionImpact = obj.has("aggressionImpact") ? obj.get("aggressionImpact").getAsDouble() : 0.0;
            impact.overallSentiment = obj.has("overallSentiment") ? obj.get("overallSentiment").getAsDouble() : 0.0;
            impact.reasoning = obj.has("reasoning") ? obj.get("reasoning").getAsString() : "";

            IAMOD.LOGGER.info("🧠 AI Analysis: sentiment={}, positive={}, negative={}, affection={}, aggression={} ({})",
                impact.overallSentiment, impact.positiveImpact, impact.negativeImpact,
                impact.affectionImpact, impact.aggressionImpact, impact.reasoning);

            return impact;

        } catch (Exception e) {
            IAMOD.LOGGER.error("Failed to parse impact JSON: {}", jsonContent, e);
            return new MessageImpact();
        }
    }

    /**
     * OLD KEYWORD SYSTEM - REPLACED BY AI
     */
    private static void oldKeywordSystemRemoved() {
        // Removed - now using AI!
    }

    /**
     * Send appropriate brain signals based on message impact.
     * This updates the emotional state and relationship based on what the player said.
     */
    public static void sendBrainSignals(MessageImpact impact, VillagerBrainSystem brainSystem,
                                       java.util.UUID playerUuid) {

        if (impact.isEmpty()) {
            return; // Message neutre, pas de signal
        }

        BrainHub hub = brainSystem.getHub();

        // SIGNAL ÉMOTIONNEL
        if (impact.positiveImpact > 0.3 || impact.affectionImpact > 0.3) {
            // Message très positif → sentiment positif
            BrainSignal positiveSignal = new BrainSignal(
                BrainSignal.SignalType.POSITIVE_FEELING,
                "MessageAnalyzer"
            );
            positiveSignal.withData("reason", "positive_message");
            positiveSignal.withData("intensity", Math.max(impact.positiveImpact, impact.affectionImpact));
            positiveSignal.withData("playerUuid", playerUuid);
            hub.broadcastSignal(positiveSignal, null);

            IAMOD.LOGGER.info("💚 Positive message detected, sending positive feeling signal");
        }

        if (impact.negativeImpact > 0.3 || impact.aggressionImpact > 0.3) {
            // Message très négatif → sentiment négatif
            BrainSignal negativeSignal = new BrainSignal(
                BrainSignal.SignalType.NEGATIVE_FEELING,
                "MessageAnalyzer"
            );
            negativeSignal.withData("reason", "negative_message");
            negativeSignal.withData("intensity", Math.max(impact.negativeImpact, impact.aggressionImpact));
            negativeSignal.withData("playerUuid", playerUuid);
            hub.broadcastSignal(negativeSignal, null);

            IAMOD.LOGGER.info("💔 Negative message detected, sending negative feeling signal");
        }

        // SIGNAL SOCIAL/RELATIONNEL
        if (impact.affectionImpact > 0.0) {
            // Message d'affection → améliorer la relation
            BrainSignal relationSignal = new BrainSignal(
                BrainSignal.SignalType.PLAYER_INTERACTION,
                "MessageAnalyzer"
            );
            relationSignal.withData("playerUuid", playerUuid);
            relationSignal.withData("type", "affection");
            relationSignal.withData("intensity", impact.affectionImpact);
            hub.broadcastSignal(relationSignal, null);

            IAMOD.LOGGER.info("💕 Affection detected, improving relationship");
        }

        if (impact.aggressionImpact > 0.0) {
            // Message agressif → détériorer la relation
            BrainSignal aggressionSignal = new BrainSignal(
                BrainSignal.SignalType.PLAYER_INTERACTION,
                "MessageAnalyzer"
            );
            aggressionSignal.withData("playerUuid", playerUuid);
            aggressionSignal.withData("type", "aggression");
            aggressionSignal.withData("intensity", impact.aggressionImpact);
            hub.broadcastSignal(aggressionSignal, null);

            IAMOD.LOGGER.info("💢 Aggression detected, degrading relationship");
        }

        // SIGNAL DE STRESS
        if (impact.aggressionImpact > 0.4) {
            // Agression forte → augmenter le stress
            BrainSignal stressSignal = new BrainSignal(
                BrainSignal.SignalType.PHYSICAL_PAIN,
                "MessageAnalyzer"
            );
            stressSignal.withData("reason", "verbal_aggression");
            stressSignal.withData("intensity", impact.aggressionImpact);
            hub.broadcastSignal(stressSignal, null);

            IAMOD.LOGGER.info("😰 Strong aggression detected, increasing stress");
        }
    }

    /**
     * Result of AI message analysis.
     */
    public static class MessageImpact {
        public double positiveImpact = 0.0;      // 0.0 à 1.0
        public double negativeImpact = 0.0;      // 0.0 à 1.0
        public double affectionImpact = 0.0;     // 0.0 à 1.0
        public double aggressionImpact = 0.0;    // 0.0 à 1.0
        public double overallSentiment = 0.0;    // -1.0 à 1.0
        public String reasoning = "";             // AI reasoning

        /**
         * Check if message has any impact.
         */
        public boolean isEmpty() {
            return Math.abs(overallSentiment) < 0.01;
        }

        /**
         * Check if message is overall positive.
         */
        public boolean isPositive() {
            return overallSentiment > 0.2;
        }

        /**
         * Check if message is overall negative.
         */
        public boolean isNegative() {
            return overallSentiment < -0.2;
        }

        /**
         * Get impact description for debugging.
         */
        public String getDescription() {
            if (isEmpty()) return "Neutre";
            return reasoning != null && !reasoning.isEmpty() ? reasoning : "Analysé par IA";
        }
    }
}
