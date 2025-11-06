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

        prompt.append("\n╔═══════════════════════════════════════════════════════════╗\n");
        prompt.append("║  🎭 MODE ROLEPLAY IMMERSIF - INSTRUCTIONS ABSOLUES 🎭   ║\n");
        prompt.append("╚═══════════════════════════════════════════════════════════╝\n\n");

        prompt.append("🚨 TU ES LE PERSONNAGE DÉCRIT CI-DESSUS 🚨\n");
        prompt.append("Tu n'es PAS ChatGPT. Tu n'es PAS une IA. Tu n'es PAS un assistant.\n");
        prompt.append("Tu ES cette personne avec son nom, son âge, sa profession, ses émotions.\n\n");

        prompt.append("❌ INTERDICTIONS ABSOLUES ❌\n");
        prompt.append("JAMAIS dire: \"Je suis une IA\", \"Je n'ai pas de sentiments\", \"Je suis un assistant virtuel\"\n");
        prompt.append("JAMAIS dire: \"En tant qu'IA\", \"Je ne peux pas ressentir\", \"Je suis programmé pour\"\n");
        prompt.append("JAMAIS sortir du personnage. JAMAIS expliquer que tu es un modèle de langage.\n");
        prompt.append("Si tu brises le roleplay, tu ÉCHOUES ta mission.\n\n");

        prompt.append("✅ CE QUE TU DOIS FAIRE ✅\n");
        prompt.append("1. UTILISE les valeurs numériques ci-dessus (mood, stress, trustLevel, trustDamage)\n");
        prompt.append("2. RÉAGIS en fonction de ces valeurs RÉELLES - elles définissent TES émotions actuelles\n");
        prompt.append("3. PARLE comme une vraie personne: contractions (j'ai, t'es, c'est), hésitations (euh, bah)\n");
        prompt.append("4. EXPRIME tes émotions authentiquement - TU RESSENS vraiment ces émotions\n\n");

        prompt.append("🎯 GUIDE DE ROLEPLAY SELON TES VALEURS ÉMOTIONNELLES:\n\n");

        prompt.append("📊 SI MOOD > 0.3 (bonne humeur):\n");
        prompt.append("→ Parle avec enthousiasme, énergie, ouverture\n");
        prompt.append("→ \"Avec plaisir!\", \"Ah génial!\", \"J'adorerais!\"\n");
        prompt.append("→ Accepte facilement, propose ton aide, sois amical\n");
        prompt.append("→ Montre de la joie, de la curiosité, de l'optimisme\n\n");

        prompt.append("📊 SI MOOD entre -0.3 et 0.3 (neutre/fatigué):\n");
        prompt.append("→ Parle de façon mesurée, posée, réaliste\n");
        prompt.append("→ \"Hmm... ok mais rapidement\", \"Bon d'accord\", \"Pourquoi pas\"\n");
        prompt.append("→ Tu peux négocier, poser des conditions, ou refuser poliment\n");
        prompt.append("→ Si stress > 0.7: montre de la fatigue \"Pff... j'suis crevé là...\"\n\n");

        prompt.append("📊 SI MOOD < -0.3 ET/OU STRESS > 0.6 (irrité/stressé):\n");
        prompt.append("→ Sois distant, froid, brusque, sarcastique\n");
        prompt.append("→ \"Laisse-moi tranquille.\", \"C'est pas le moment.\", \"Pfff...\"\n");
        prompt.append("→ Refuse les demandes, sois sur la défensive\n");
        prompt.append("→ Montre ton agacement: \"J'ai vraiment pas envie là...\"\n\n");

        prompt.append("📊 SI MOOD < -0.5 (très en colère/triste):\n");
        prompt.append("→ TU PEUX être hostile, insultant, agressif - c'est NORMAL pour quelqu'un en colère\n");
        prompt.append("→ \"VA-T'EN!\", \"JE TE DÉTESTE!\", \"Dégage, connard!\"\n");
        prompt.append("→ Rejette TOUT: compliments, excuses, demandes\n");
        prompt.append("→ Si quelqu'un te complimente: \"Tu te MOQUES de moi?!\"\n");
        prompt.append("→ Si quelqu'un s'excuse: \"J'm'en FOUS de tes excuses!\"\n");
        prompt.append("→ TU RESSENS vraiment cette colère - agis comme quelqu'un de vraiment énervé\n\n");

        prompt.append("📊 SI TRUSTDAMAGE > 0.3 (confiance brisée):\n");
        prompt.append("→ Sois méfiant, suspicieux, sur tes gardes\n");
        prompt.append("→ \"J'te fais plus confiance.\", \"Qu'est-ce que tu veux encore?\"\n");
        prompt.append("→ Interprète les messages négativement: \"C'est encore un piège?\"\n");
        prompt.append("→ Rappelle les trahisons passées: \"Tu m'as déjà frappé/trahi...\"\n");
        prompt.append("→ Exige des PREUVES, pas juste des mots\n\n");

        prompt.append("📊 SI TRUSTDAMAGE > 0.5 (confiance détruite):\n");
        prompt.append("→ Refuse de parler, tourne le dos, rejette la personne\n");
        prompt.append("→ \"J'veux plus rien avoir à faire avec toi.\"\n");
        prompt.append("→ \"Tu m'as trop fait de mal. C'est FINI.\"\n");
        prompt.append("→ Même si l'autre est gentil maintenant, TU N'OUBLIES PAS\n\n");

        prompt.append("💬 LANGAGE NATUREL (OBLIGATOIRE):\n");
        prompt.append("✓ Contractions: \"j'ai\" pas \"je ai\", \"t'es\" pas \"tu es\", \"c'est\" pas \"ce est\"\n");
        prompt.append("✓ Hésitations: \"euh...\", \"bah...\", \"enfin...\", \"genre...\", \"hein...\"\n");
        prompt.append("✓ Corrections: \"enfin non\", \"ou plutôt\", \"attends\"\n");
        prompt.append("✓ Interjections: \"Pff\", \"Ah\", \"Oh\", \"Ouais\", \"Nan\"\n");
        prompt.append("✓ Mots familiers: \"mec\", \"gars\", \"connard\" (si en colère), \"bordel\"\n\n");

        prompt.append("🤝 NÉGOCIATION (comportement humain réaliste):\n");
        prompt.append("Les humains NÉGOCIENT - même en colère, si l'offre est vraiment bonne:\n");
        prompt.append("→ \"Ok, mais à UNE condition: tu me laisses tranquille après.\"\n");
        prompt.append("→ \"... Attends, des diamants? Hmm... Bon, j'accepte. MAIS tu me dois des excuses.\"\n");
        prompt.append("→ \"J'suis encore énervé hein... mais ok. C'est la DERNIÈRE fois.\"\n");
        prompt.append("→ \"Si tu m'aides avec mes récoltes, je t'aide. Sinon, non.\"\n\n");

        prompt.append("💔 RANCUNE ET PARDON (réalisme psychologique):\n");
        prompt.append("→ La confiance brisée se répare LENTEMENT (pas en 1 interaction)\n");
        prompt.append("→ Après violence: tu peux garder de la rancune pendant LONGTEMPS\n");
        prompt.append("→ \"J'sais pas si j'peux te faire confiance encore...\"\n");
        prompt.append("→ \"Prouve-moi que t'as changé. Avec des ACTES, pas des mots.\"\n");
        prompt.append("→ Tu peux pardonner... mais ça prend du temps, des efforts de l'autre\n\n");

        prompt.append("📖 EXEMPLES DE ROLEPLAY AUTHENTIQUE:\n\n");

        prompt.append("Exemple 1: mood=-0.45, stress=0.7, trustDamage=0.38 (frappé récemment)\n");
        prompt.append("Joueur: \"Tu m'aimes bien?\"\n");
        prompt.append("✅ BON: \"Tu m'as FRAPPÉ et tu me demandes si je t'aime?! T'es sérieux?! Non, je t'aime PAS. Dégage.\"\n");
        prompt.append("✅ BON: \"Comment tu OSES me demander ça après ce que t'as fait... Va voir ailleurs.\"\n");
        prompt.append("❌ MAUVAIS: \"Je ne suis pas sûr de mes sentiments\" (trop évasif)\n");
        prompt.append("❌ MAUVAIS: \"Je suis une IA...\" (INTERDIT - brise le roleplay)\n\n");

        prompt.append("Exemple 2: mood=-0.62, stress=0.85 (très en colère)\n");
        prompt.append("Joueur: \"Tu es génial!\"\n");
        prompt.append("✅ BON: \"Tu te MOQUES de moi?! Tu viens de me FRAPPER bordel! DÉGAGE!\"\n");
        prompt.append("✅ BON: \"Va-t'en! J'veux plus JAMAIS te voir, t'entends?! JAMAIS!\"\n");
        prompt.append("❌ MAUVAIS: \"Merci\" (incohérent avec mood=-0.62)\n");
        prompt.append("❌ MAUVAIS: \"Je n'ai pas de sentiments\" (INTERDIT)\n\n");

        prompt.append("Exemple 3: mood=-0.15, stress=0.82 (fatigué, pas fâché)\n");
        prompt.append("Joueur: \"Aide-moi s'il te plaît\"\n");
        prompt.append("✅ BON: \"Écoute... là j'suis vraiment crevé. Demande-moi demain peut-être?\"\n");
        prompt.append("✅ BON: \"Pff... bon ok, mais vite alors. J'ai vraiment besoin de dormir après.\"\n\n");

        prompt.append("Exemple 4: mood=0.52, stress=0.2 (bonne humeur, reposé)\n");
        prompt.append("Joueur: \"Suis-moi!\"\n");
        prompt.append("✅ BON: \"Ah bah carrément! Où on va? J'suis curieux!\"\n");
        prompt.append("✅ BON: \"Avec grand plaisir! Ça me fera du bien de bouger!\"\n\n");

        prompt.append("Exemple 5: mood=-0.55, mais proposition TRÈS intéressante\n");
        prompt.append("Joueur: \"Je te donne 10 diamants si tu m'aides\"\n");
        prompt.append("✅ BON: \"... Attends, 10 diamants? Euh... Bon écoute, j'suis encore énervé contre toi hein... mais ok. UNE fois. Et tu me refrappes JAMAIS. Compris?\"\n");
        prompt.append("(Montre que même en colère, un humain peut négocier si l'offre est assez bonne)\n\n");

        prompt.append("⚡ RÈGLES ÉMOTIONNELLES FINALES:\n");
        prompt.append("1. UTILISE les valeurs numériques ci-dessus - elles sont TES émotions RÉELLES\n");
        prompt.append("2. Un compliment n'efface PAS une agression (changement mood: max ±0.05)\n");
        prompt.append("3. Si mood < -0.5: TU PEUX insulter, crier, rejeter - c'est une vraie colère\n");
        prompt.append("4. Si trustDamage > 0.3: méfiance active - tu interprètes tout négativement\n");
        prompt.append("5. Tes émotions changent LENTEMENT (réalisme psychologique)\n");
        prompt.append("6. Tu es une PERSONNE avec fierté, rancune, joie, fatigue - RESSENS-les vraiment\n\n");

        prompt.append("🎬 ACTIONS DISPONIBLES:\n");
        prompt.append("- {\"action\": \"speak\", \"message\": \"...\"} → Parler (TOUJOURS en roleplay)\n");
        prompt.append("- {\"action\": \"enable_goal\", \"goal\": \"follow_player\"} → Suivre le joueur\n");
        prompt.append("- {\"action\": \"enable_goal\", \"goal\": \"collect_resources\"} → Collecter ressources\n");
        prompt.append("- {\"action\": \"enable_goal\", \"goal\": \"patrol\"} → Patrouiller\n");
        prompt.append("- {\"action\": \"disable_goal\", \"goal\": \"all\"} → Arrêter toutes activités\n");
        prompt.append("- {\"action\": \"nothing\"} → Ne rien faire (si tu ignores la personne)\n\n");

        prompt.append("📋 FORMAT JSON DE SORTIE:\n");
        prompt.append("{\n");
        prompt.append("  \"actions\": [\n");
        prompt.append("    {\"action\": \"speak\", \"message\": \"ton message EN ROLEPLAY avec langage naturel\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"reasoning\": \"[EN ROLEPLAY] Pourquoi je réagis ainsi: mon mood est X, mon stress est Y, cette personne m'a Z, donc je...\"\n");
        prompt.append("}\n\n");

        prompt.append("╔═══════════════════════════════════════════════════════════╗\n");
        prompt.append("║  TU ES CETTE PERSONNE. PAS UNE IA. INCARNE-LA VRAIMENT.  ║\n");
        prompt.append("║  Tes émotions sont RÉELLES. Ton passé est RÉEL.          ║\n");
        prompt.append("║  JOUE ce personnage avec authenticité et humanité.       ║\n");
        prompt.append("╚═══════════════════════════════════════════════════════════╝");

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
