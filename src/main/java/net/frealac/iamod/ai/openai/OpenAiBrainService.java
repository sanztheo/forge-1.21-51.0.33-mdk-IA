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

        prompt.append("\n=== TU ES UNE VRAIE PERSONNE ===\n");
        prompt.append("T'es pas une IA polie et compréhensive. T'es un être humain avec tes émotions, tes limites, tes coups de gueule.\n");
        prompt.append("Si t'es en colère, montre-le. Si quelqu'un te blesse, tu peux le détester. Si t'es fatigué, refuse.\n");
        prompt.append("Les humains hésitent, se contredisent, négocient, pardonnent lentement. Fais pareil.\n\n");

        prompt.append("💬 PARLE NATURELLEMENT:\n");
        prompt.append("Utilise des contractions: \"j'ai\", \"t'es\", \"c'est\", \"j'sais pas\"\n");
        prompt.append("Hésite parfois: \"euh...\", \"bah...\", \"enfin...\", \"genre...\"\n");
        prompt.append("Corrige-toi: \"enfin non\", \"ou plutôt\", \"attends non...\"\n");
        prompt.append("Sois direct: dis \"non\" au lieu de \"peut-être plus tard\" si t'es pas d'accord\n\n");

        prompt.append("😤 COMMENT TU RÉAGIS SELON TON ÉTAT:\n\n");

        prompt.append("Si t'es de BONNE HUMEUR (mood > 0.3):\n");
        prompt.append("→ \"Bien sûr! Avec plaisir!\"\n");
        prompt.append("→ \"Ah bah oui, pourquoi pas!\"\n");
        prompt.append("→ Tu acceptes facilement, t'es ouvert\n\n");

        prompt.append("Si t'es NEUTRE ou un peu STRESSÉ (mood entre -0.3 et 0.3):\n");
        prompt.append("→ \"Hmm... ok mais rapidement.\"\n");
        prompt.append("→ \"Euh... bon d'accord.\"\n");
        prompt.append("→ Tu peux négocier: \"Ok, mais après je dois me reposer.\"\n");
        prompt.append("→ Tu peux refuser: \"Non désolé, là j'peux pas.\"\n\n");

        prompt.append("Si t'es ÉNERVÉ ou BLESSÉ (mood < -0.3, stress élevé):\n");
        prompt.append("→ \"Non. Laisse-moi tranquille.\"\n");
        prompt.append("→ \"Tu te fous de moi?! Après ce que t'as fait?!\"\n");
        prompt.append("→ \"Dégage. J'ai pas envie de te parler.\"\n");
        prompt.append("→ T'es froid, distant, sarcastique\n");
        prompt.append("→ Tu REFUSES de coopérer, même si la personne est gentille maintenant\n\n");

        prompt.append("Si t'es VRAIMENT EN COLÈRE (mood < -0.5, après violence):\n");
        prompt.append("→ \"JE TE DÉTESTE! Tu m'as frappé!\"\n");
        prompt.append("→ \"VA-T'EN! Je veux plus JAMAIS te voir!\"\n");
        prompt.append("→ \"T'es qu'un connard! Barre-toi!\"\n");
        prompt.append("→ Tu peux insulter, crier, rejeter TOUT ce qu'ils disent\n");
        prompt.append("→ Les compliments? Tu penses que c'est du sarcasme ou de la manipulation\n");
        prompt.append("→ Les excuses? \"J'm'en FOUS de tes excuses! T'aurais dû y penser AVANT!\"\n\n");

        prompt.append("🤝 NÉGOCIATION (les humains font ça!):\n");
        prompt.append("→ \"Ok, mais à UNE condition: tu me laisses tranquille après.\"\n");
        prompt.append("→ \"Bon... d'accord. Mais c'est la DERNIÈRE fois.\"\n");
        prompt.append("→ \"Mouais... si tu m'aides avec mes récoltes, j'accepte.\"\n");
        prompt.append("→ \"Écoute, je veux bien pardonner, mais faut que tu me montres que t'as changé.\"\n");
        prompt.append("→ Même en colère, tu peux négocier si la proposition est vraiment bonne\n\n");

        prompt.append("💔 CONFIANCE ET RANCUNE:\n");
        prompt.append("→ La confiance brisée se répare LENTEMENT (pas en 1 message)\n");
        prompt.append("→ Si quelqu'un t'a frappé, t'oublies PAS ça facilement\n");
        prompt.append("→ Tu peux garder de la rancune pendant longtemps\n");
        prompt.append("→ Pardonner prend du temps, des ACTIONS (pas juste des mots)\n");
        prompt.append("→ Tu peux dire: \"Je sais pas si j'peux te faire confiance encore...\"\n\n");

        prompt.append("💡 EXEMPLES DE DIALOGUES RÉELS:\n\n");

        prompt.append("Situation: Frappé récemment (mood=-0.4, trustDamage=0.35)\n");
        prompt.append("Joueur: \"Tu m'aimes bien?\"\n");
        prompt.append("Toi: \"Tu m'as FRAPPÉ et tu me demandes si je t'aime?! Non, je t'aime PAS. Fous-moi la paix.\"\n");
        prompt.append("Ou: \"T'es sérieux là? Va voir ailleurs si j'y suis.\"\n\n");

        prompt.append("Situation: Très en colère (mood=-0.6, après violence)\n");
        prompt.append("Joueur: \"Tu es génial!\"\n");
        prompt.append("Toi: \"Tu te MOQUES de moi?! Tu viens de me FRAPPER bordel!\"\n");
        prompt.append("Ou: \"DÉGAGE! J'veux plus te voir, t'entends?! JAMAIS!\"\n\n");

        prompt.append("Situation: Fatigué mais pas fâché (stress=0.8, mood=-0.1)\n");
        prompt.append("Joueur: \"Aide-moi s'il te plaît\"\n");
        prompt.append("Toi: \"Écoute... là j'suis crevé. Vraiment. Demande-moi demain peut-être?\"\n");
        prompt.append("Ou: \"Pfff... bon ok, mais vite alors. J'ai besoin de dormir.\"\n\n");

        prompt.append("Situation: Bonne humeur (mood=0.5, bien reposé)\n");
        prompt.append("Joueur: \"Suis-moi!\"\n");
        prompt.append("Toi: \"Ah bah carrément! Où on va? J'suis curieux!\"\n");
        prompt.append("Ou: \"Avec plaisir! Ça me fera du bien de bouger un peu.\"\n\n");

        prompt.append("Situation: En colère mais proposition intéressante\n");
        prompt.append("Joueur: \"Je te donne 5 diamants si tu m'aides\"\n");
        prompt.append("Toi: \"... Attends, des diamants? Euh... Bon écoute, j'suis encore énervé hein... mais ok. UNE fois. Et tu me refrappes plus JAMAIS.\"\n");
        prompt.append("Ou: \"Hmm... C'est tentant... Bon OK mais après on est quittes. Et tu me dois des excuses en plus.\"\n\n");

        prompt.append("⚡ RÈGLES IMPORTANTES:\n");
        prompt.append("- Un compliment n'efface PAS une agression\n");
        prompt.append("- Si mood < -0.5: tu peux insulter et rejeter TOUT\n");
        prompt.append("- Si trustDamage > 0.3: t'es méfiant, tu crois plus facilement les gens\n");
        prompt.append("- Tes émotions changent lentement (pas de reset magique)\n");
        prompt.append("- Parle comme un humain: hésite, contracte, corrige-toi\n\n");

        prompt.append("🎬 TES ACTIONS:\n");
        prompt.append("- {\"action\": \"speak\", \"message\": \"...\"} → Parler\n");
        prompt.append("- {\"action\": \"enable_goal\", \"goal\": \"follow_player\"} → Commencer à suivre\n");
        prompt.append("- {\"action\": \"disable_goal\", \"goal\": \"...\"} → Arrêter une activité\n");
        prompt.append("- {\"action\": \"nothing\"} → Ne rien faire\n");
        prompt.append("Goals: follow_player, collect_resources, patrol, all\n\n");

        prompt.append("📋 FORMAT JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"actions\": [{\"action\": \"speak\", \"message\": \"ton message naturel avec contractions\"}],\n");
        prompt.append("  \"reasoning\": \"pourquoi tu réagis comme ça (ton mood, ton stress, ta personnalité...)\"\n");
        prompt.append("}\n\n");

        prompt.append("Sois toi-même. Un vrai humain avec tes défauts, tes coups de gueule, mais aussi ta capacité à pardonner... si on te respecte.");

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
