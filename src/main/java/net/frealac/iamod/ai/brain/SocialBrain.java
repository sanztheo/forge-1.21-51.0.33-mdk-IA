package net.frealac.iamod.ai.brain;

import net.frealac.iamod.IAMOD;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Social Brain Module - Manages social interactions and relationships.
 * Like the prefrontal cortex in humans (social cognition).
 *
 * This brain:
 * - Tracks relationships with players
 * - Manages conversation context
 * - Interprets social cues
 * - Decides appropriate social responses
 */
public class SocialBrain extends BrainModule {

    private UUID currentConversationPartner;
    private String currentConversationTopic;
    private int conversationTurnCount;
    private long conversationStartTime;

    // Relationship tracking
    private final Map<UUID, RelationshipData> relationships;

    public static class RelationshipData {
        public double trustLevel;           // 0.0 à 1.0
        public double intimacy;             // 0.0 à 1.0 (familiarité)
        public int totalInteractions;
        public long lastInteractionTime;
        public String lastKnownName;

        // SCIENTIFIC TRUST REPAIR MECHANISM
        public double trustDamage;          // 0.0 to 1.0 - accumulated trust violations
        public int violationCount;          // Number of trust violations (hits, betrayals)
        public long lastViolationTime;      // Timestamp of last violation

        // Trust repair is SLOW and requires consistent positive actions
        private static final double TRUST_REPAIR_RATE = 0.01;     // Per positive interaction
        private static final double TRUST_DAMAGE_DECAY = 0.005;   // Per day without violations
        private static final double VIOLATION_PENALTY = 0.3;       // Trust damage per violation

        public RelationshipData() {
            this.trustLevel = 0.5;          // Neutral au début
            this.intimacy = 0.0;            // Pas familier au début
            this.totalInteractions = 0;
            this.lastInteractionTime = 0;
            this.lastKnownName = "Joueur";
            this.trustDamage = 0.0;
            this.violationCount = 0;
            this.lastViolationTime = 0;
        }

        /**
         * Calculate effective trust level accounting for damage.
         * SCIENTIFIC BASIS: Trust violations create lasting damage that's slow to repair.
         */
        public double getEffectiveTrust() {
            // Trust damage reduces effective trust
            return Math.max(0.0, trustLevel - (trustDamage * 0.5));
        }

        /**
         * Record a trust violation (hit, betrayal, aggression).
         * SCIENTIFIC BASIS: Violations accumulate and persist.
         */
        public void recordViolation() {
            trustDamage = Math.min(1.0, trustDamage + VIOLATION_PENALTY);
            violationCount++;
            lastViolationTime = System.currentTimeMillis();
        }

        /**
         * Attempt to repair trust through positive interaction.
         * SCIENTIFIC BASIS: Trust repair is slow and requires consistency.
         */
        public void repairTrust() {
            if (trustDamage > 0) {
                // Trust repairs slowly with positive interactions
                trustDamage = Math.max(0.0, trustDamage - TRUST_REPAIR_RATE);
            }

            // Natural decay of trust damage over time (forgiveness)
            long daysSinceViolation = (System.currentTimeMillis() - lastViolationTime) / (1000 * 60 * 60 * 24);
            if (daysSinceViolation > 0 && trustDamage > 0) {
                trustDamage = Math.max(0.0, trustDamage - (TRUST_DAMAGE_DECAY * daysSinceViolation));
            }
        }
    }

    public SocialBrain() {
        super("SocialBrain");
        this.relationships = new HashMap<>();
        this.conversationTurnCount = 0;

        IAMOD.LOGGER.info("🧠 SocialBrain initialized");
    }

    @Override
    public void receiveSignal(BrainSignal signal) {
        switch (signal.getType()) {
            case CONVERSATION_START:
                // Début d'une conversation
                Object uuid = signal.getData("playerUuid");
                if (uuid instanceof UUID) {
                    startConversation((UUID) uuid);
                }
                break;

            case CONVERSATION_END:
                // Fin de conversation
                endConversation();
                break;

            case PLAYER_INTERACTION:
                // Interaction avec joueur - mettre à jour la relation
                uuid = signal.getData("playerUuid");
                if (uuid instanceof UUID) {
                    updateRelationship((UUID) uuid, signal);
                }
                break;

            case POSITIVE_FEELING:
                // Sentiment positif - améliorer la relation ET réparer trust damage
                if (currentConversationPartner != null) {
                    RelationshipData rel = getOrCreateRelationship(currentConversationPartner);
                    rel.repairTrust(); // SLOW trust repair through positive interaction
                    adjustTrust(currentConversationPartner, 0.05); // Smaller increase
                    adjustIntimacy(currentConversationPartner, 0.05);
                    IAMOD.LOGGER.debug("🧠 SocialBrain: Positive feeling, repairing trust (damage: {})",
                        rel.trustDamage);
                }
                break;

            case NEGATIVE_FEELING:
                // Sentiment négatif - détériorer la relation ET enregistrer violation
                if (currentConversationPartner != null) {
                    RelationshipData rel = getOrCreateRelationship(currentConversationPartner);
                    rel.recordViolation(); // Record trust damage
                    adjustTrust(currentConversationPartner, -0.15);
                    IAMOD.LOGGER.warn("🧠 SocialBrain: Negative feeling, trust violation recorded (damage: {})",
                        rel.trustDamage);
                }
                break;

            default:
                // Ignore other signals
                break;
        }
    }

    /**
     * Start a conversation with a player.
     */
    private void startConversation(UUID playerUuid) {
        currentConversationPartner = playerUuid;
        conversationStartTime = System.currentTimeMillis();
        conversationTurnCount = 0;

        RelationshipData rel = getOrCreateRelationship(playerUuid);
        rel.totalInteractions++;
        rel.lastInteractionTime = System.currentTimeMillis();

        IAMOD.LOGGER.debug("🧠 SocialBrain: Conversation started with player {}", playerUuid);

        sendSignal(new BrainSignal(BrainSignal.SignalType.RELATIONSHIP_UPDATE, moduleName)
            .withData("playerUuid", playerUuid)
            .withData("trust", rel.trustLevel)
            .withData("intimacy", rel.intimacy));
    }

    /**
     * End current conversation.
     */
    private void endConversation() {
        if (currentConversationPartner != null) {
            // Augmenter l'intimité à chaque conversation
            adjustIntimacy(currentConversationPartner, 0.02);

            IAMOD.LOGGER.debug("🧠 SocialBrain: Conversation ended after {} turns",
                conversationTurnCount);
        }

        currentConversationPartner = null;
        conversationTurnCount = 0;
    }

    /**
     * Update relationship based on interaction signal.
     */
    private void updateRelationship(UUID playerUuid, BrainSignal signal) {
        RelationshipData rel = getOrCreateRelationship(playerUuid);

        String interactionType = (String) signal.getData("type");
        Double intensity = (Double) signal.getData("intensity");

        if (interactionType != null) {
            switch (interactionType) {
                case "gift":
                    rel.repairTrust(); // Gifts help repair trust
                    adjustTrust(playerUuid, 0.08); // Smaller increase with repair
                    adjustIntimacy(playerUuid, 0.1);
                    break;
                case "attack":
                    rel.recordViolation(); // MAJOR trust violation
                    adjustTrust(playerUuid, -0.25);
                    IAMOD.LOGGER.warn("⚔️ SocialBrain: Attack violation recorded! Trust damage: {}",
                        rel.trustDamage);
                    break;
                case "help":
                    rel.repairTrust();
                    adjustTrust(playerUuid, 0.05);
                    adjustIntimacy(playerUuid, 0.05);
                    break;
                case "affection":
                    // Message d'affection → améliorer trust et intimacy BUT NOT instant reset
                    rel.repairTrust(); // Slow repair
                    double affectionAmount = intensity != null ? intensity * 0.08 : 0.04; // MUCH smaller
                    adjustTrust(playerUuid, affectionAmount);
                    adjustIntimacy(playerUuid, affectionAmount * 0.5);
                    IAMOD.LOGGER.info("💕 SocialBrain: Affection, slow trust repair (damage: {})",
                        rel.trustDamage);
                    break;
                case "aggression":
                    // Message agressif → VIOLATION
                    rel.recordViolation(); // Record as violation
                    double aggressionAmount = intensity != null ? intensity * -0.2 : -0.15;
                    adjustTrust(playerUuid, aggressionAmount);
                    IAMOD.LOGGER.warn("💢 SocialBrain: Aggression violation (damage: {}, count: {})",
                        rel.trustDamage, rel.violationCount);
                    break;
            }
        }
    }

    /**
     * Adjust trust level for a player.
     */
    private void adjustTrust(UUID playerUuid, double amount) {
        RelationshipData rel = getOrCreateRelationship(playerUuid);
        double oldTrust = rel.trustLevel;
        rel.trustLevel = Math.max(0.0, Math.min(1.0, rel.trustLevel + amount));

        if (rel.trustLevel != oldTrust) {
            IAMOD.LOGGER.debug("🧠 SocialBrain: Trust adjusted for {}: {} → {}",
                playerUuid, oldTrust, rel.trustLevel);

            sendSignal(new BrainSignal(BrainSignal.SignalType.RELATIONSHIP_UPDATE, moduleName)
                .withData("playerUuid", playerUuid)
                .withData("trust", rel.trustLevel));
        }
    }

    /**
     * Adjust intimacy (familiarity) level for a player.
     */
    private void adjustIntimacy(UUID playerUuid, double amount) {
        RelationshipData rel = getOrCreateRelationship(playerUuid);
        double oldIntimacy = rel.intimacy;
        rel.intimacy = Math.max(0.0, Math.min(1.0, rel.intimacy + amount));

        if (rel.intimacy != oldIntimacy) {
            IAMOD.LOGGER.debug("🧠 SocialBrain: Intimacy adjusted for {}: {} → {}",
                playerUuid, oldIntimacy, rel.intimacy);
        }
    }

    /**
     * Get or create relationship data for a player.
     */
    private RelationshipData getOrCreateRelationship(UUID playerUuid) {
        return relationships.computeIfAbsent(playerUuid, k -> new RelationshipData());
    }

    /**
     * Get relationship data for a player.
     */
    public RelationshipData getRelationship(UUID playerUuid) {
        return getOrCreateRelationship(playerUuid);
    }

    /**
     * Get social context for AI prompt.
     */
    public String getSocialContextForPrompt(UUID playerUuid) {
        if (playerUuid == null) {
            return "Je ne connais pas cette personne.";
        }

        RelationshipData rel = getOrCreateRelationship(playerUuid);
        StringBuilder context = new StringBuilder();

        // Trust description with DAMAGE awareness
        double effectiveTrust = rel.getEffectiveTrust();

        if (rel.trustDamage > 0.5) {
            context.append("Cette personne m'a gravement blessé. Je ne lui fais plus confiance. ");
        } else if (rel.trustDamage > 0.3) {
            context.append("Cette personne m'a fait du mal. Je reste sur mes gardes. ");
        } else if (effectiveTrust > 0.8) {
            context.append("Je fais totalement confiance à cette personne. ");
        } else if (effectiveTrust > 0.6) {
            context.append("Je fais plutôt confiance à cette personne. ");
        } else if (effectiveTrust > 0.4) {
            context.append("Je suis neutre envers cette personne. ");
        } else if (effectiveTrust > 0.2) {
            context.append("Je me méfie un peu de cette personne. ");
        } else {
            context.append("Je ne fais pas du tout confiance à cette personne. ");
        }

        // Mention violation count if significant
        if (rel.violationCount > 3) {
            context.append(String.format("Cette personne m'a fait du mal %d fois. ", rel.violationCount));
        } else if (rel.violationCount > 0) {
            context.append("Cette personne m'a déjà fait du mal. ");
        }

        // Intimacy description
        if (rel.intimacy > 0.7) {
            context.append("Nous sommes très proches, comme des amis. ");
        } else if (rel.intimacy > 0.4) {
            context.append("Nous nous connaissons assez bien. ");
        } else if (rel.intimacy > 0.2) {
            context.append("Nous nous connaissons un peu. ");
        } else {
            context.append("Nous ne sommes pas vraiment familiers. ");
        }

        // Interaction history
        if (rel.totalInteractions > 20) {
            context.append("Nous avons interagi de nombreuses fois. ");
        } else if (rel.totalInteractions > 5) {
            context.append("Nous avons déjà interagi plusieurs fois. ");
        } else if (rel.totalInteractions > 1) {
            context.append("C'est notre deuxième ou troisième interaction. ");
        } else {
            context.append("C'est notre première interaction. ");
        }

        return context.toString();
    }

    @Override
    public String getStateDescription() {
        return String.format(
            "Active conversation: %s, Relationships tracked: %d, Conversation turns: %d",
            currentConversationPartner != null ? "Yes" : "No",
            relationships.size(),
            conversationTurnCount
        );
    }

    // Getters
    public UUID getCurrentConversationPartner() { return currentConversationPartner; }
    public int getTotalRelationships() { return relationships.size(); }
}
