package net.frealac.iamod.ai.brain;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.common.story.VillagerStory;

import java.util.UUID;

/**
 * General Brain Module - Coordinates all other brain modules and generates responses.
 * Like the prefrontal cortex in humans (executive function, decision making).
 *
 * This is the "CEO" of the brain system that:
 * - Receives input from all specialized brains
 * - Coordinates their activities
 * - Makes final decisions
 * - Generates the actual response to the player
 *
 * ARCHITECTURE:
 * Player talks → GeneralBrain receives message
 * → GeneralBrain asks EmotionalBrain: "How do I feel?"
 * → GeneralBrain asks MemoryBrain: "Do I remember this player?"
 * → GeneralBrain asks SocialBrain: "What's my relationship?"
 * → GeneralBrain synthesizes everything → Generates response
 */
public class GeneralBrain extends BrainModule {

    private final EmotionalBrain emotionalBrain;
    private final MemoryBrain memoryBrain;
    private final SocialBrain socialBrain;

    private String lastDecision;
    private String lastReasoningProcess;

    public GeneralBrain(EmotionalBrain emotionalBrain, MemoryBrain memoryBrain, SocialBrain socialBrain) {
        super("GeneralBrain");
        this.emotionalBrain = emotionalBrain;
        this.memoryBrain = memoryBrain;
        this.socialBrain = socialBrain;

        IAMOD.LOGGER.info("🧠 GeneralBrain initialized - Coordinating {} specialized brains",
            emotionalBrain != null && memoryBrain != null && socialBrain != null ? 3 : 0);
    }

    @Override
    public void receiveSignal(BrainSignal signal) {
        // Le cerveau général écoute tous les signaux pour coordination
        switch (signal.getType()) {
            case EMOTION_CHANGE:
                // Une émotion a changé - en tenir compte dans les décisions futures
                IAMOD.LOGGER.debug("🧠 GeneralBrain: Emotion changed from {} to {}",
                    signal.getData("oldValue"), signal.getData("newValue"));
                break;

            case MEMORY_RECALLED:
                // Des souvenirs ont été rappelés
                Integer memoryCount = (Integer) signal.getData("memoryCount");
                IAMOD.LOGGER.debug("🧠 GeneralBrain: {} memories recalled", memoryCount);
                break;

            case RELATIONSHIP_UPDATE:
                // Une relation a été mise à jour
                Double trust = (Double) signal.getData("trust");
                IAMOD.LOGGER.debug("🧠 GeneralBrain: Relationship trust level is now {}", trust);
                break;

            default:
                // Le cerveau général observe mais ne réagit pas directement aux autres signaux
                break;
        }
    }

    /**
     * Generate a comprehensive context for AI decision making.
     * This is what the AI will use to generate responses.
     *
     * @param playerUuid UUID of the player
     * @param story Villager's complete story
     * @param playerMessage What the player said
     * @param currentGoalsState Current goals/actions
     * @return Complete context string for AI prompt
     */
    public String generateComprehensiveContext(UUID playerUuid, VillagerStory story,
                                               String playerMessage, String currentGoalsState) {

        IAMOD.LOGGER.info("🧠 GeneralBrain: Generating comprehensive context for player {}", playerUuid);

        StringBuilder context = new StringBuilder();

        // 1. IDENTITY (qui suis-je ?)
        context.append("=== QUI JE SUIS ===\n");
        String name = (story.nameGiven != null ? story.nameGiven : "Villageois") +
                     (story.nameFamily != null ? (" " + story.nameFamily) : "");
        context.append("Nom: ").append(name).append("\n");
        context.append("Âge: ").append(story.ageYears > 0 ? (story.ageYears + " ans") : "adulte").append("\n");
        context.append("Profession: ").append(story.profession != null ? story.profession : "habitant").append("\n");

        if (story.traits != null && !story.traits.isEmpty()) {
            String traits = String.join(", ", story.traits.subList(0, Math.min(5, story.traits.size())));
            context.append("Traits de personnalité: ").append(traits).append("\n");
        }

        context.append("\n");

        // 2. EMOTIONAL STATE (comment je me sens ?)
        context.append("=== COMMENT JE ME SENS ===\n");
        if (emotionalBrain != null) {
            context.append(emotionalBrain.getEmotionalStateForPrompt()).append("\n");
        } else {
            context.append("État émotionnel normal.\n");
        }
        context.append("\n");

        // 3. MEMORIES (qu'est-ce que je me souviens ?)
        context.append("=== MES SOUVENIRS AVEC CE JOUEUR ===\n");
        if (memoryBrain != null) {
            context.append(memoryBrain.getMemorySummaryForPrompt(playerUuid)).append("\n");
            context.append("\nDétails des souvenirs:\n");
            context.append(memoryBrain.getMemoriesForPrompt(playerUuid)).append("\n");
        } else {
            context.append("Aucun souvenir particulier.\n");
        }
        context.append("\n");

        // 4. RELATIONSHIP (quelle est ma relation avec ce joueur ?)
        context.append("=== MA RELATION AVEC CE JOUEUR ===\n");
        if (socialBrain != null) {
            context.append(socialBrain.getSocialContextForPrompt(playerUuid)).append("\n");

            SocialBrain.RelationshipData rel = socialBrain.getRelationship(playerUuid);
            context.append(String.format("Niveau de confiance: %.0f%%, Familiarité: %.0f%%\n",
                rel.trustLevel * 100, rel.intimacy * 100));
        } else {
            context.append("Première rencontre.\n");
        }
        context.append("\n");

        // 5. CURRENT SITUATION (que suis-je en train de faire ?)
        context.append("=== MA SITUATION ACTUELLE ===\n");
        context.append(currentGoalsState).append("\n");
        context.append("\n");

        // 6. PLAYER MESSAGE (qu'est-ce que le joueur a dit ?)
        context.append("=== CE QUE LE JOUEUR VIENT DE ME DIRE ===\n");
        context.append("\"").append(playerMessage).append("\"\n");
        context.append("\n");

        // 7. INSTRUCTIONS (comment dois-je répondre ?)
        context.append("=== COMMENT JE DOIS RÉPONDRE ===\n");
        context.append("Je suis une PERSONNE RÉELLE avec des émotions, des souvenirs et des relations.\n");
        context.append("Ma réponse doit être basée sur:\n");
        context.append("1. Mon état émotionnel actuel (humeur, stress)\n");
        context.append("2. Mes souvenirs de cette personne\n");
        context.append("3. Ma relation avec cette personne (confiance, familiarité)\n");
        context.append("4. Ma personnalité et mes valeurs\n");
        context.append("\nJe peux accepter, refuser, négocier - comme une vraie personne.\n");
        context.append("Je ne suis PAS un robot qui obéit toujours.\n");

        String fullContext = context.toString();
        IAMOD.LOGGER.debug("🧠 GeneralBrain: Generated context ({} chars)", fullContext.length());

        return fullContext;
    }

    /**
     * Analyze a player interaction and coordinate brain responses.
     */
    public void analyzePlayerInteraction(UUID playerUuid, String message) {
        IAMOD.LOGGER.info("🧠 GeneralBrain: Analyzing player interaction");

        // Send signal to start conversation
        BrainSignal startSignal = new BrainSignal(BrainSignal.SignalType.CONVERSATION_START, moduleName)
            .withData("playerUuid", playerUuid)
            .withData("message", message);
        sendSignal(startSignal);

        // Send signal for player interaction
        BrainSignal interactionSignal = new BrainSignal(BrainSignal.SignalType.PLAYER_INTERACTION, moduleName)
            .withData("playerUuid", playerUuid)
            .withData("message", message);
        sendSignal(interactionSignal);

        // Request decision from all brains
        BrainSignal decisionRequest = new BrainSignal(BrainSignal.SignalType.DECISION_REQUEST, moduleName)
            .withData("playerUuid", playerUuid)
            .withData("context", message);
        sendSignal(decisionRequest);
    }

    /**
     * Finalize a decision after consulting all brain modules.
     */
    public void finalizeDecision(String decision, String reasoning) {
        this.lastDecision = decision;
        this.lastReasoningProcess = reasoning;

        IAMOD.LOGGER.info("🧠 GeneralBrain: Decision made - {}", decision);

        // Notify other brains
        sendSignal(new BrainSignal(BrainSignal.SignalType.DECISION_MADE, moduleName)
            .withData("decision", decision)
            .withData("reasoning", reasoning));
    }

    /**
     * Get summary of all brain states for debugging.
     */
    public String getAllBrainsStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== BRAIN SYSTEM STATUS ===\n\n");

        if (emotionalBrain != null) {
            status.append("EMOTIONAL BRAIN:\n");
            status.append(emotionalBrain.getStateDescription()).append("\n\n");
        }

        if (memoryBrain != null) {
            status.append("MEMORY BRAIN:\n");
            status.append(memoryBrain.getStateDescription()).append("\n\n");
        }

        if (socialBrain != null) {
            status.append("SOCIAL BRAIN:\n");
            status.append(socialBrain.getStateDescription()).append("\n\n");
        }

        status.append("GENERAL BRAIN:\n");
        status.append("Last decision: ").append(lastDecision != null ? lastDecision : "None").append("\n");

        return status.toString();
    }

    @Override
    public String getStateDescription() {
        return String.format(
            "Last decision: %s, Coordinating: Emotional=%s, Memory=%s, Social=%s",
            lastDecision != null ? lastDecision : "None",
            emotionalBrain != null ? "✓" : "✗",
            memoryBrain != null ? "✓" : "✗",
            socialBrain != null ? "✓" : "✗"
        );
    }

    // Getters
    public EmotionalBrain getEmotionalBrain() { return emotionalBrain; }
    public MemoryBrain getMemoryBrain() { return memoryBrain; }
    public SocialBrain getSocialBrain() { return socialBrain; }
    public String getLastDecision() { return lastDecision; }
    public String getLastReasoningProcess() { return lastReasoningProcess; }
}
