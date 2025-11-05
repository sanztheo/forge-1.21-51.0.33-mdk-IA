package net.frealac.iamod.ai.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages all memories for a single villager.
 * Memories influence the villager's behavior and responses.
 */
public class VillagerMemory {
    private static final int MAX_MEMORIES = 50; // Keep most important/recent memories
    private static final int PROMPT_MEMORY_LIMIT = 10; // Max memories to include in AI prompt

    @SerializedName("memories")
    private final List<Memory> memories = new ArrayList<>();

    @SerializedName("known_players")
    private final Map<String, String> knownPlayers = new HashMap<>(); // UUID -> Name

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Add a new memory.
     */
    public void addMemory(Memory memory) {
        memories.add(memory);

        // Track player name if provided
        if (memory.getPlayerUuid() != null && memory.getPlayerName() != null) {
            knownPlayers.put(memory.getPlayerUuid(), memory.getPlayerName());
        }

        // Cleanup old/unimportant memories if we have too many
        if (memories.size() > MAX_MEMORIES) {
            pruneMemories();
        }
    }

    /**
     * Add a memory about a player.
     */
    public void addMemory(MemoryType type, String description, UUID playerUuid, String playerName) {
        addMemory(new Memory(type, description, playerUuid, playerName));
    }

    /**
     * Get all memories about a specific player.
     */
    public List<Memory> getMemoriesAboutPlayer(UUID playerUuid) {
        return memories.stream()
                .filter(m -> m.isAboutPlayer(playerUuid))
                .collect(Collectors.toList());
    }

    /**
     * Get player's name if known.
     */
    public String getPlayerName(UUID playerUuid) {
        return knownPlayers.get(playerUuid.toString());
    }

    /**
     * Check if villager knows this player's name.
     */
    public boolean knowsPlayer(UUID playerUuid) {
        return knownPlayers.containsKey(playerUuid.toString());
    }

    /**
     * Get the overall sentiment this villager has towards a player.
     * Returns: -1.0 (very negative) to +1.0 (very positive)
     */
    public double getSentimentTowardsPlayer(UUID playerUuid) {
        List<Memory> playerMemories = getMemoriesAboutPlayer(playerUuid);
        if (playerMemories.isEmpty()) return 0.0;

        // Recent memories have more weight
        double totalWeight = 0.0;
        double weightedSum = 0.0;

        for (Memory memory : playerMemories) {
            // Recency weight: recent memories matter more
            double recencyWeight = calculateRecencyWeight(memory.getHoursAgo());
            // Importance weight: important memories matter more
            double weight = memory.getImportance() * recencyWeight;

            weightedSum += memory.getEmotionalImpact() * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }

    /**
     * Calculate how much weight to give to a memory based on how recent it is.
     * Recent = 1.0, very old = 0.1
     */
    private double calculateRecencyWeight(double hoursAgo) {
        if (hoursAgo < 1) return 1.0;     // Within last hour: full weight
        if (hoursAgo < 24) return 0.8;    // Today: 80%
        if (hoursAgo < 168) return 0.5;   // This week: 50%
        return 0.2;                        // Older: 20%
    }

    /**
     * Get most important/recent memories for AI prompt.
     */
    public List<Memory> getMemoriesForPrompt(UUID currentPlayerUuid) {
        List<Memory> relevantMemories = new ArrayList<>();

        // 1. Get memories about current player (high priority)
        List<Memory> playerMemories = getMemoriesAboutPlayer(currentPlayerUuid);
        relevantMemories.addAll(playerMemories);

        // 2. Add other important memories
        List<Memory> otherMemories = memories.stream()
                .filter(m -> !m.isAboutPlayer(currentPlayerUuid))
                .filter(m -> m.getImportance() > 0.7 || m.isRecent())
                .collect(Collectors.toList());

        relevantMemories.addAll(otherMemories);

        // 3. Sort by importance and recency
        relevantMemories.sort((m1, m2) -> {
            double score1 = m1.getImportance() * calculateRecencyWeight(m1.getHoursAgo());
            double score2 = m2.getImportance() * calculateRecencyWeight(m2.getHoursAgo());
            return Double.compare(score2, score1); // Descending
        });

        // 4. Limit to most relevant
        return relevantMemories.stream()
                .limit(PROMPT_MEMORY_LIMIT)
                .collect(Collectors.toList());
    }

    /**
     * Format memories for AI prompt.
     */
    public String formatMemoriesForPrompt(UUID currentPlayerUuid) {
        List<Memory> memories = getMemoriesForPrompt(currentPlayerUuid);
        if (memories.isEmpty()) {
            return "Aucun souvenir particulier.";
        }

        StringBuilder sb = new StringBuilder();

        // Add sentiment summary
        double sentiment = getSentimentTowardsPlayer(currentPlayerUuid);
        String playerName = getPlayerName(currentPlayerUuid);

        if (playerName != null) {
            sb.append("Tu connais ").append(playerName).append(".\n");
        }

        if (sentiment > 0.3) {
            sb.append("Tu as une bonne impression de cette personne.\n");
        } else if (sentiment < -0.3) {
            sb.append("Tu n'as pas confiance en cette personne.\n");
        }

        // Add individual memories
        sb.append("Souvenirs récents:\n");
        for (Memory memory : memories) {
            sb.append(memory.toPromptString()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Prune old and unimportant memories to keep memory usage reasonable.
     */
    private void pruneMemories() {
        // Sort by importance and recency
        memories.sort((m1, m2) -> {
            double score1 = m1.getImportance() * calculateRecencyWeight(m1.getHoursAgo());
            double score2 = m2.getImportance() * calculateRecencyWeight(m2.getHoursAgo());
            return Double.compare(score2, score1); // Descending
        });

        // Keep only the most important/recent
        while (memories.size() > MAX_MEMORIES) {
            memories.remove(memories.size() - 1);
        }
    }

    /**
     * Get all memories.
     */
    public List<Memory> getAllMemories() {
        return new ArrayList<>(memories);
    }

    /**
     * Get number of memories.
     */
    public int getMemoryCount() {
        return memories.size();
    }

    /**
     * Get memories with a specific player (alias for getMemoriesAboutPlayer).
     * Used for debug HUD display.
     */
    public List<Memory> getMemoriesWithPlayer(UUID playerUuid) {
        return getMemoriesAboutPlayer(playerUuid);
    }

    /**
     * Clear all memories (for testing or reset).
     */
    public void clearMemories() {
        memories.clear();
        knownPlayers.clear();
    }

    /**
     * Serialize to JSON.
     */
    public String toJson() {
        return gson.toJson(this);
    }

    /**
     * Deserialize from JSON.
     */
    public static VillagerMemory fromJson(String json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, VillagerMemory.class);
    }
}
