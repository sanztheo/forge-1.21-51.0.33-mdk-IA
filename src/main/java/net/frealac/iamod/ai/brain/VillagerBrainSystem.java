package net.frealac.iamod.ai.brain;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.common.story.VillagerStory;

import java.util.UUID;

/**
 * Complete brain system for a villager.
 * Manages all brain modules and coordinates their communication.
 *
 * ARCHITECTURE:
 * VillagerBrainSystem
 * ├─ BrainHub (central communication)
 * ├─ EmotionalBrain (emotions, mood, stress)
 * ├─ MemoryBrain (souvenirs, past interactions)
 * ├─ SocialBrain (relationships, social context)
 * └─ GeneralBrain (coordination, decision making)
 *
 * All brains communicate via BrainHub using BrainSignals.
 * GeneralBrain reads from all others to make informed decisions.
 */
public class VillagerBrainSystem {

    private final BrainHub hub;
    private final EmotionalBrain emotionalBrain;
    private final MemoryBrain memoryBrain;
    private final SocialBrain socialBrain;
    private final GeneralBrain generalBrain;

    private final int villagerId;

    /**
     * Create a complete brain system for a villager.
     */
    public VillagerBrainSystem(int villagerId, VillagerStory story) {
        this.villagerId = villagerId;

        IAMOD.LOGGER.info("🧠 Initializing Brain System for villager ID={}", villagerId);

        // Create central communication hub
        this.hub = new BrainHub();

        // Create specialized brain modules
        this.emotionalBrain = new EmotionalBrain(story.psychology);
        this.memoryBrain = new MemoryBrain(story.interactionMemory);
        this.socialBrain = new SocialBrain();
        this.generalBrain = new GeneralBrain(emotionalBrain, memoryBrain, socialBrain);

        // Register all modules to the hub (they can now communicate)
        hub.registerModule(emotionalBrain);
        hub.registerModule(memoryBrain);
        hub.registerModule(socialBrain);
        hub.registerModule(generalBrain);

        IAMOD.LOGGER.info("✓ Brain System initialized with {} modules connected via hub",
            hub.getModules().size());
    }

    /**
     * Process a player message through the entire brain system.
     * This is the main entry point for AI interaction.
     */
    public String processPlayerMessage(UUID playerUuid, String message,
                                       VillagerStory story, String currentGoalsState) {

        IAMOD.LOGGER.info("🧠 Brain System: Processing message from player {}", playerUuid);

        try {
            // 1. GeneralBrain analyzes the interaction
            generalBrain.analyzePlayerInteraction(playerUuid, message);

            // 2. GeneralBrain generates comprehensive context from ALL brains
            String comprehensiveContext = generalBrain.generateComprehensiveContext(
                playerUuid, story, message, currentGoalsState);

            IAMOD.LOGGER.debug("🧠 Brain System: Context generated ({} characters)",
                comprehensiveContext.length());

            return comprehensiveContext;

        } catch (Exception e) {
            IAMOD.LOGGER.error("🧠 Brain System: Error processing message", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Notify the brain system of an important event.
     */
    public void notifyEvent(BrainSignal.SignalType eventType, Object... keyValues) {
        BrainSignal signal = new BrainSignal(eventType, "VillagerBrainSystem");

        // Add key-value pairs to signal
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                signal.withData((String) keyValues[i], keyValues[i + 1]);
            }
        }

        hub.broadcastSignal(signal, null);
        IAMOD.LOGGER.debug("🧠 Brain System: Event notified - {}", eventType);
    }

    /**
     * Get the complete brain status for debugging.
     */
    public String getSystemStatus() {
        return generalBrain.getAllBrainsStatus();
    }

    /**
     * Get signal history from the hub.
     */
    public String getSignalHistory() {
        StringBuilder history = new StringBuilder();
        history.append("=== BRAIN SIGNAL HISTORY ===\n");

        for (BrainSignal signal : hub.getSignalHistory()) {
            history.append(String.format("[%d] %s from %s\n",
                signal.getTimestamp(), signal.getType(), signal.getSourceModule()));
        }

        return history.toString();
    }

    // Getters for individual brains
    public BrainHub getHub() { return hub; }
    public EmotionalBrain getEmotionalBrain() { return emotionalBrain; }
    public MemoryBrain getMemoryBrain() { return memoryBrain; }
    public SocialBrain getSocialBrain() { return socialBrain; }
    public GeneralBrain getGeneralBrain() { return generalBrain; }
    public int getVillagerId() { return villagerId; }
}
