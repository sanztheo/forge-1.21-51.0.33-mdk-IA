package net.frealac.iamod.ai.brain;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.memory.Memory;
import net.frealac.iamod.ai.memory.VillagerMemory;

import java.util.List;
import java.util.UUID;

/**
 * Memory Brain Module - Manages memories and recalls past interactions.
 * Like the hippocampus in humans.
 *
 * This brain:
 * - Stores interaction memories
 * - Recalls relevant memories when needed
 * - Recognizes patterns in past interactions
 * - Influences decisions based on past experiences
 */
public class MemoryBrain extends BrainModule {

    private final VillagerMemory memorySystem;
    private UUID currentPlayerUuid;

    public MemoryBrain(VillagerMemory memorySystem) {
        super("MemoryBrain");
        this.memorySystem = memorySystem != null ? memorySystem : new VillagerMemory();

        IAMOD.LOGGER.info("🧠 MemoryBrain initialized with {} memories",
            this.memorySystem.getMemoryCount());
    }

    @Override
    public void receiveSignal(BrainSignal signal) {
        switch (signal.getType()) {
            case PLAYER_INTERACTION:
                // Un joueur interagit - mémoriser son UUID
                Object uuid = signal.getData("playerUuid");
                if (uuid instanceof UUID) {
                    currentPlayerUuid = (UUID) uuid;
                    IAMOD.LOGGER.debug("🧠 MemoryBrain: Player interaction detected, UUID={}", currentPlayerUuid);
                }
                break;

            case IMPORTANT_EVENT:
                // Un événement important s'est produit - le mémoriser
                String description = (String) signal.getData("description");
                if (description != null && currentPlayerUuid != null) {
                    sendSignal(new BrainSignal(BrainSignal.SignalType.MEMORY_STORED, moduleName)
                        .withData("description", description));
                    IAMOD.LOGGER.debug("🧠 MemoryBrain: Important event stored: {}", description);
                }
                break;

            case DECISION_REQUEST:
                // Une décision est demandée - rappeler les souvenirs pertinents
                if (currentPlayerUuid != null) {
                    List<Memory> playerMemories = memorySystem.getMemoriesWithPlayer(currentPlayerUuid);
                    sendSignal(new BrainSignal(BrainSignal.SignalType.MEMORY_RECALLED, moduleName)
                        .withData("memories", playerMemories)
                        .withData("memoryCount", playerMemories.size()));
                }
                break;

            default:
                // Ignore other signals
                break;
        }
    }

    /**
     * Get memories with the current player formatted for AI prompt.
     */
    public String getMemoriesForPrompt(UUID playerUuid) {
        if (playerUuid == null) {
            return "Aucun souvenir particulier avec ce joueur.";
        }

        currentPlayerUuid = playerUuid;
        return memorySystem.formatMemoriesForPrompt(playerUuid);
    }

    /**
     * Get all memories with a specific player.
     */
    public List<Memory> getMemoriesWithPlayer(UUID playerUuid) {
        return memorySystem.getMemoriesWithPlayer(playerUuid);
    }

    /**
     * Check if villager remembers being hit by this player.
     */
    public boolean remembersBeingHit(UUID playerUuid) {
        if (playerUuid == null) return false;

        List<Memory> memories = memorySystem.getMemoriesWithPlayer(playerUuid);
        for (Memory memory : memories) {
            if (memory.getType() == net.frealac.iamod.ai.memory.MemoryType.WAS_HIT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if villager remembers receiving a gift from this player.
     */
    public boolean remembersGiftFrom(UUID playerUuid) {
        if (playerUuid == null) return false;

        List<Memory> memories = memorySystem.getMemoriesWithPlayer(playerUuid);
        for (Memory memory : memories) {
            if (memory.getType() == net.frealac.iamod.ai.memory.MemoryType.GIFT_RECEIVED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get sentiment towards player based on memories.
     */
    public double getSentimentTowardsPlayer(UUID playerUuid) {
        if (playerUuid == null) return 0.0;
        return memorySystem.getSentimentTowardsPlayer(playerUuid);
    }

    /**
     * Get total memory count.
     */
    public int getMemoryCount() {
        return memorySystem.getMemoryCount();
    }

    @Override
    public String getStateDescription() {
        return String.format(
            "Total memories: %d, Current player memories: %d, Current player sentiment: %.2f",
            memorySystem.getMemoryCount(),
            currentPlayerUuid != null ? memorySystem.getMemoriesWithPlayer(currentPlayerUuid).size() : 0,
            currentPlayerUuid != null ? memorySystem.getSentimentTowardsPlayer(currentPlayerUuid) : 0.0
        );
    }

    /**
     * Get memory summary for AI prompt.
     */
    public String getMemorySummaryForPrompt(UUID playerUuid) {
        if (playerUuid == null) {
            return "Je ne connais pas ce joueur.";
        }

        List<Memory> memories = memorySystem.getMemoriesWithPlayer(playerUuid);
        double sentiment = memorySystem.getSentimentTowardsPlayer(playerUuid);

        StringBuilder summary = new StringBuilder();

        if (memories.isEmpty()) {
            summary.append("C'est notre première rencontre. ");
        } else {
            summary.append(String.format("J'ai %d souvenirs avec ce joueur. ", memories.size()));

            // Sentiment description
            if (sentiment > 0.5) {
                summary.append("Je ressens de l'affection pour cette personne. ");
            } else if (sentiment > 0.2) {
                summary.append("J'ai une impression plutôt positive de cette personne. ");
            } else if (sentiment > -0.2) {
                summary.append("Je suis neutre envers cette personne. ");
            } else if (sentiment > -0.5) {
                summary.append("J'ai une impression plutôt négative de cette personne. ");
            } else {
                summary.append("Je n'aime pas cette personne et je me méfie d'elle. ");
            }

            // Key memories
            if (remembersBeingHit(playerUuid)) {
                summary.append("Je me souviens qu'elle m'a frappé. ");
            }
            if (remembersGiftFrom(playerUuid)) {
                summary.append("Je me souviens qu'elle m'a donné des cadeaux. ");
            }
        }

        return summary.toString();
    }

    // Getters
    public VillagerMemory getMemorySystem() { return memorySystem; }
}
