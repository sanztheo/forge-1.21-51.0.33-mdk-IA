package net.frealac.iamod.ai.openai;

import com.google.gson.*;
import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.brain.AIAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Brain Service - Analyzes player messages and decides what actions to take.
 * Returns structured JSON with actions (enable/disable goals, speak, etc.)
 */
public class OpenAiBrainService {

    private final OpenAiClient client;
    private final Gson gson;

    public OpenAiBrainService() {
        this.client = new OpenAiClient();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Analyze player message and decide what actions to take.
     * Takes into account the FULL villager personality, mood, health, memories, etc.
     *
     * @param playerMessage What the player said
     * @param villagerStory Complete villager story with personality, mood, health, memories
     * @param currentGoalsState Current state of goals (for context)
     * @param playerUuid Player UUID for memory tracking
     * @return List of actions to execute
     */
    public List<AIAction> analyzeIntention(String playerMessage,
                                          net.frealac.iamod.common.story.VillagerStory villagerStory,
                                          String currentGoalsState,
                                          java.util.UUID playerUuid)
            throws IOException, InterruptedException {

        JsonObject payload = new JsonObject();

        JsonArray messages = new JsonArray();

        // System prompt - teaches AI how to respond with actions + FULL personality context + MEMORIES
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", buildBrainSystemPromptWithPersonality(villagerStory, currentGoalsState, playerUuid));
        messages.add(system);

        // User message
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", playerMessage);
        messages.add(user);

        payload.add("messages", messages);
        payload.addProperty("temperature", 0.3); // Lower temperature for more consistent JSON
        payload.addProperty("max_tokens", 300);
        payload.add("response_format", createJsonResponseFormat());

        String responseBody = client.sendChatRequest(payload);
        String content = extractContent(responseBody);

        // Parse actions from JSON response
        return parseActions(content);
    }

    /**
     * Build RICH system prompt with FULL villager personality context + MEMORIES.
     * The AI brain decides AUTONOMOUSLY how to respond based on ALL the context.
     * NO predefined conditions - the AI is a true autonomous brain.
     * Includes all interaction memories to create realistic, consistent behavior.
     */
    private String buildBrainSystemPromptWithPersonality(
            net.frealac.iamod.common.story.VillagerStory story,
            String goalsState,
            java.util.UUID playerUuid) {

        // Basic identity
        String name = (story.nameGiven != null ? story.nameGiven : "Villageois") +
                     (story.nameFamily != null ? (" " + story.nameFamily) : "");
        String age = story.ageYears > 0 ? (story.ageYears + " ans") : "adulte";
        String profession = story.profession != null ? story.profession : "habitant";

        // Personality traits
        String traits = story.traits != null && !story.traits.isEmpty()
            ? String.join(", ", story.traits.subList(0, Math.min(5, story.traits.size())))
            : "aucun trait particulier";

        // Psychology state - RAW DATA for AI to interpret
        String psychState = "";
        if (story.psychology != null) {
            double mood = story.psychology.moodBaseline;
            double stress = story.psychology.stress;
            double resilience = story.psychology.resilience;

            psychState = String.format(
                "Psychologie: humeur=%.2f (-1=déprimé, 0=neutre, +1=joyeux), stress=%.2f (0=calme, 1=très stressé), résilience=%.2f",
                mood, stress, resilience
            );
        }

        // Health state - RAW DATA
        String healthState = "";
        if (story.health != null) {
            int wounds = story.health.wounds != null ? story.health.wounds.size() : 0;
            healthState = String.format("Santé: %d blessures, qualité sommeil=%.2f (0=épuisé, 1=reposé)",
                wounds, story.health.sleepQuality);
        }

        // Build the RICH system prompt - LET THE AI DECIDE NATURALLY
        StringBuilder prompt = new StringBuilder();

        prompt.append("TU ES ").append(name).append(", ").append(age).append(", ")
              .append(profession).append(".\n\n");

        prompt.append("=== QUI TU ES ===\n");
        prompt.append("Traits de personnalité: ").append(traits).append("\n");
        if (!psychState.isEmpty()) prompt.append(psychState).append("\n");
        if (!healthState.isEmpty()) prompt.append(healthState).append("\n");
        if (story.bioBrief != null && !story.bioBrief.isEmpty()) {
            prompt.append("Histoire: ").append(story.bioBrief.substring(0, Math.min(200, story.bioBrief.length()))).append("\n");
        }

        // Memories - CRITICAL for realistic interactions
        prompt.append("\n=== TES SOUVENIRS ===\n");
        if (story.interactionMemory != null) {
            String memoriesText = story.interactionMemory.formatMemoriesForPrompt(playerUuid);
            prompt.append(memoriesText).append("\n");
        } else {
            prompt.append("Aucun souvenir particulier.\n");
        }

        prompt.append("\n=== TA SITUATION ACTUELLE ===\n");
        prompt.append(goalsState).append("\n");

        prompt.append("\n=== COMMENT TU FONCTIONNES ===\n");
        prompt.append("Tu es un ÊTRE VIVANT. Tu as des émotions, de la fatigue, du stress.\n");
        prompt.append("Tu réagis NATURELLEMENT selon ton état et ta personnalité - pas selon des règles.\n");
        prompt.append("Tu peux accepter, refuser, négocier - comme une vraie personne le ferait.\n");
        prompt.append("Tes décisions viennent de QUI TU ES, pas de ce qu'on te dit de faire.\n\n");

        prompt.append("=== ACTIONS DISPONIBLES ===\n");
        prompt.append("Tu peux choisir parmi ces actions:\n");
        prompt.append("- {\"action\": \"speak\", \"message\": \"...\"} → Parler au joueur\n");
        prompt.append("- {\"action\": \"enable_goal\", \"goal\": \"...\"} → Commencer une activité\n");
        prompt.append("- {\"action\": \"disable_goal\", \"goal\": \"...\"} → Arrêter une activité\n");
        prompt.append("- {\"action\": \"nothing\"} → Ne rien faire\n\n");

        prompt.append("Goals disponibles: follow_player, collect_resources, patrol, all\n\n");

        prompt.append("=== EXEMPLES D'INTERACTIONS NATURELLES ===\n");
        prompt.append("Ces exemples montrent comment une personne réagirait naturellement:\n\n");

        prompt.append("Joueur: \"Suis-moi\"\n");
        prompt.append("Personne joyeuse et reposée → \"Avec plaisir! Où va-t-on?\" + commence à suivre\n");
        prompt.append("Personne irritée → \"Non, laisse-moi tranquille.\"\n");
        prompt.append("Personne épuisée → \"Je suis trop fatigué pour ça...\"\n");
        prompt.append("Personne neutre mais occupée → \"Pas maintenant, je suis occupé.\"\n\n");

        prompt.append("Joueur: \"Bonjour\"\n");
        prompt.append("Personne de bonne humeur → \"Bonjour! Belle journée!\"\n");
        prompt.append("Personne stressée → \"Mm... salut.\"\n");
        prompt.append("Personne timide → \"Euh... bonjour...\" (dit doucement)\n\n");

        prompt.append("=== FORMAT DE SORTIE (JSON STRICT) ===\n");
        prompt.append("Réponds en JSON avec ce format:\n");
        prompt.append("{\n");
        prompt.append("  \"actions\": [\n");
        prompt.append("    {\"action\": \"speak\", \"message\": \"ta réponse naturelle\"},\n");
        prompt.append("    {\"action\": \"enable_goal\", \"goal\": \"follow_player\"} (si tu décides de le faire)\n");
        prompt.append("  ],\n");
        prompt.append("  \"reasoning\": \"pourquoi tu as réagi ainsi (ton état, ta personnalité...)\"\n");
        prompt.append("}\n\n");

        prompt.append("Agis comme la personne que tu es. Laisse tes émotions, ton état physique, et ta personnalité guider tes décisions.");

        return prompt.toString();
    }

    /**
     * Create JSON response format specification for OpenAI.
     */
    private JsonObject createJsonResponseFormat() {
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_object");
        return format;
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
     * Parse actions from JSON response.
     */
    private List<AIAction> parseActions(String jsonContent) {
        List<AIAction> actions = new ArrayList<>();

        try {
            JsonObject response = JsonParser.parseString(jsonContent).getAsJsonObject();

            // Get reasoning (for debugging)
            String reasoning = response.has("reasoning") ? response.get("reasoning").getAsString() : "";
            IAMOD.LOGGER.info("AI Brain reasoning: {}", reasoning);

            // Parse actions array
            if (response.has("actions")) {
                JsonArray actionsArray = response.getAsJsonArray("actions");
                for (JsonElement actionElement : actionsArray) {
                    JsonObject actionObj = actionElement.getAsJsonObject();
                    AIAction action = gson.fromJson(actionObj, AIAction.class);
                    action.reasoning = reasoning;
                    actions.add(action);
                }
            }

        } catch (Exception e) {
            IAMOD.LOGGER.error("Failed to parse AI brain response: {}", jsonContent, e);
            // Fallback: create a speak action with the raw content
            AIAction fallback = AIAction.speak("Je n'ai pas bien compris...");
            actions.add(fallback);
        }

        return actions;
    }
}
