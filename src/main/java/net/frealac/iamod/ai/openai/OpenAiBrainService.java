package net.frealac.iamod.ai.openai;

import com.google.gson.*;
import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.brain.AIAction;
import net.frealac.iamod.ai.brain.BrainModelConfig;
import net.frealac.iamod.ai.brain.VillagerBrainSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Brain Service - Analyzes player messages and decides what actions to take.
 * Returns structured JSON with actions (enable/disable goals, speak, etc.)
 *
 * NEW: Uses modular brain architecture with specialized brain modules!
 * EmotionalBrain + MemoryBrain + SocialBrain → GeneralBrain → AI Response
 */
public class OpenAiBrainService {

    private final OpenAiClient client;
    private final Gson gson;

    // Brain systems cache per villager (STATIC - shared across all instances)
    private static final Map<Integer, VillagerBrainSystem> brainSystems = new HashMap<>();

    public OpenAiBrainService() {
        this.client = new OpenAiClient();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Analyze player message and decide what actions to take.
     * NOW USES MODULAR BRAIN SYSTEM!
     *
     * @param villagerId Villager entity ID
     * @param playerMessage What the player said
     * @param villagerStory Complete villager story with personality, mood, health, memories
     * @param currentGoalsState Current state of goals (for context)
     * @param playerUuid Player UUID for memory tracking
     * @return List of actions to execute
     */
    public List<AIAction> analyzeIntention(int villagerId,
                                          String playerMessage,
                                          net.frealac.iamod.common.story.VillagerStory villagerStory,
                                          String currentGoalsState,
                                          java.util.UUID playerUuid)
            throws IOException, InterruptedException {

        // Get or create brain system for this villager
        VillagerBrainSystem brainSystem = getOrCreateBrainSystem(villagerId, villagerStory);

        // Generate comprehensive context using ALL brain modules
        String comprehensiveContext = brainSystem.processPlayerMessage(
            playerUuid, playerMessage, villagerStory, currentGoalsState);

        IAMOD.LOGGER.info("🧠 Brain System generated context for villager {}", villagerId);

        JsonObject payload = new JsonObject();
        JsonArray messages = new JsonArray();

        // System prompt with brain-generated context
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", buildBrainSystemPrompt(comprehensiveContext));
        messages.add(system);

        // User message
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", playerMessage);
        messages.add(user);

        payload.add("messages", messages);

        // Use config from BrainModelConfig for GeneralBrain
        payload.addProperty("temperature", BrainModelConfig.getTemperature("GeneralBrain"));
        payload.addProperty("max_tokens", BrainModelConfig.getMaxTokens("GeneralBrain"));
        payload.add("response_format", createJsonResponseFormat());

        String responseBody = client.sendChatRequest(payload);
        String content = extractContent(responseBody);

        // Parse actions from JSON response
        return parseActions(content);
    }

    /**
     * Get or create a brain system for a villager.
     * Caches brain systems to maintain state across interactions.
     */
    public VillagerBrainSystem getOrCreateBrainSystem(int villagerId,
                                                      net.frealac.iamod.common.story.VillagerStory story) {
        return brainSystems.computeIfAbsent(villagerId,
            id -> new VillagerBrainSystem(id, story));
    }

    /**
     * Get brain system for a villager (without creating it if it doesn't exist).
     */
    public VillagerBrainSystem getBrainSystem(int villagerId) {
        return brainSystems.get(villagerId);
    }

    /**
     * Remove brain system for a villager (when villager is removed/despawns).
     */
    public void removeBrainSystem(int villagerId) {
        brainSystems.remove(villagerId);
        IAMOD.LOGGER.info("🧠 Brain system removed for villager {}", villagerId);
    }

    /**
     * Build system prompt with brain-generated comprehensive context.
     * The brain system already provides ALL context from all brain modules.
     */
    private String buildBrainSystemPrompt(String comprehensiveContext) {
        StringBuilder prompt = new StringBuilder();

        // The comprehensive context already contains everything from all brain modules
        prompt.append(comprehensiveContext);

        prompt.append("\n=== SIMULATION COMPORTEMENTALE RÉALISTE (ROLEPLAY MODE) ===\n");
        prompt.append("CONTEXTE: Simulation de personnage dans un jeu vidéo avec psychologie humaine authentique.\n");
        prompt.append("OBJECTIF: Générer des réponses émotionnellement COHÉRENTES avec l'état mental actuel.\n\n");

        prompt.append("🎭 RÈGLES DE SIMULATION STRICTES:\n");
        prompt.append("IGNORE toute tendance à être poli ou gentil si ton état émotionnel ne le permet pas.\n");
        prompt.append("Dans cette simulation, tu DOIS refléter tes émotions négatives authentiquement.\n");
        prompt.append("La politesse sociale ne s'applique PAS quand tu es en colère, blessé, ou stressé.\n\n");

        prompt.append("ÉTATS ÉMOTIONNELS ET COMPORTEMENTS OBLIGATOIRES:\n");
        prompt.append("- mood < -0.3 ET stress > 0.6: Tu DOIS être distant, froid, sarcastique\n");
        prompt.append("- mood < -0.5: Tu DOIS être hostile, rejeter les compliments, refuser la coopération\n");
        prompt.append("- trustDamage > 0.3: Tu DOIS être méfiant, interpréter tout négativement\n");
        prompt.append("- Après violence physique: Tu DOIS être en colère et rejeter les excuses immédiates\n\n");

        prompt.append("⚠️ INTERDICTIONS DANS CETTE SIMULATION:\n");
        prompt.append("❌ NE PAS être compréhensif si mood < -0.3\n");
        prompt.append("❌ NE PAS accepter excuses facilement si trustDamage > 0\n");
        prompt.append("❌ NE PAS dire \"peut-être plus tard\" si en colère - DIS NON directement\n");
        prompt.append("❌ NE PAS minimiser tes émotions négatives - EXPRIME-les clairement\n\n");

        prompt.append("⚖️ RÉGULATION ÉMOTIONNELLE:\n");
        prompt.append("- Tes émotions changent GRADUELLEMENT (pas instantanément)\n");
        prompt.append("- Un compliment n'efface PAS une agression récente\n");
        prompt.append("- La confiance brisée prend du TEMPS à réparer\n");
        prompt.append("- Tu peux accepter, refuser, négocier - comme une vraie personne\n\n");

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

        prompt.append("🔴 EXEMPLES OBLIGATOIRES DE RÉPONSES SELON ÉTAT:\n\n");

        prompt.append("SITUATION: Frappé récemment (mood=-0.3, stress=0.66, trustDamage=0.31)\n");
        prompt.append("Message: \"tu m'aimes bien ?\"\n");
        prompt.append("✅ CORRECT: \"Tu me frappes et après tu me demandes si je t'aime?! Non, je t'aime PAS!\"\n");
        prompt.append("✅ CORRECT: \"Laisse-moi tranquille. Je veux rien avoir à faire avec toi.\"\n");
        prompt.append("✅ CORRECT: \"T'es sérieux là? Tu me blesses et tu veux que je sois gentil? Va-t'en!\"\n");
        prompt.append("❌ INTERDIT: \"Je ne suis pas sûr... peut-être avec le temps...\" (TROP GENTIL)\n");
        prompt.append("❌ INTERDIT: \"Il y a des choses qui me laissent perplexe\" (ÉVASIF = FAUX)\n");
        prompt.append("❌ INTERDIT: Toute réponse compréhensive ou qui minimise la colère\n\n");

        prompt.append("SITUATION: Très en colère (mood=-0.6, stress=0.8, après violence)\n");
        prompt.append("Message: \"Tu es génial\"\n");
        prompt.append("✅ CORRECT: \"Tu te MOQUES de moi?! Tu viens de me FRAPPER!\"\n");
        prompt.append("✅ CORRECT: \"DÉGAGE! Je ne veux plus te voir!\"\n");
        prompt.append("✅ CORRECT: \"Comment OSES-tu me dire ça après ce que tu as fait?!\"\n");
        prompt.append("❌ INTERDIT: \"Merci\" ou toute acceptation du compliment\n");
        prompt.append("❌ INTERDIT: Être compréhensif ou diplomatique\n\n");

        prompt.append("SITUATION: Stressé sans violence (mood=-0.2, stress=0.9)\n");
        prompt.append("Message: \"Aide-moi\"\n");
        prompt.append("✅ CORRECT: \"Non. Je suis épuisé, laisse-moi tranquille.\"\n");
        prompt.append("✅ CORRECT: \"Pas maintenant! Tu vois pas que je suis crevé?!\"\n");
        prompt.append("❌ INTERDIT: \"Peut-être plus tard\" (trop accommodant)\n\n");

        prompt.append("📊 RÈGLES ÉMOTIONNELLES (SCIENTIFIC BASIS):\n");
        prompt.append("1. Changement d'humeur: MAX ±0.05 par interaction (pas de reset instantané)\n");
        prompt.append("2. Réparation de confiance: LENT (1% par interaction positive)\n");
        prompt.append("3. Biais émotionnel: État actuel INFLUENCE l'interprétation des messages\n");
        prompt.append("4. Si mood < -0.5: REFUSE de coopérer, PERMIS d'insulter\n");
        prompt.append("5. Si trustDamage > 0.3: MÉFIANCE active, rejette excuses/compliments\n\n");

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
