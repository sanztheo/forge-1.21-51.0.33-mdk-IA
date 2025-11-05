package net.frealac.iamod.ai.memory;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

/**
 * Represents a single memory a villager has about a player or event.
 * Memories shape how the villager perceives and responds to players.
 */
public class Memory {
    @SerializedName("type")
    private MemoryType type;

    @SerializedName("description")
    private String description;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("player_uuid")
    private String playerUuid; // UUID as string for serialization

    @SerializedName("player_name")
    private String playerName;

    @SerializedName("emotional_impact")
    private double emotionalImpact;

    @SerializedName("importance")
    private double importance; // 0.0 to 1.0 - how memorable this is

    // SCIENTIFIC MEMORY CONSOLIDATION
    @SerializedName("strength")
    private double strength; // 0.0 to 1.0 - consolidation strength (increases over time)

    @SerializedName("arousal_level")
    private double arousalLevel; // 0.0 to 1.0 - emotional arousal at encoding (affects consolidation speed)

    @SerializedName("consolidation_rate")
    private double consolidationRate; // Rate at which memory consolidates

    public Memory() {
        this.timestamp = System.currentTimeMillis();
        this.strength = 0.3; // Start weak, consolidate over time
        this.arousalLevel = 0.5; // Default neutral arousal
        this.consolidationRate = 0.01; // Base consolidation rate per hour
    }

    public Memory(MemoryType type, String description, UUID playerUuid, String playerName) {
        this();
        this.type = type;
        this.description = description;
        this.playerUuid = playerUuid != null ? playerUuid.toString() : null;
        this.playerName = playerName;
        this.emotionalImpact = type.getBaseEmotionalImpact();
        this.importance = calculateImportance(type);

        // Higher emotional impact = higher arousal = faster consolidation
        this.arousalLevel = Math.min(1.0, Math.abs(type.getBaseEmotionalImpact()) * 1.5);
        this.consolidationRate = 0.01 + (this.arousalLevel * 0.02); // 0.01 to 0.03 per hour
    }

    /**
     * Calculate how important/memorable this memory is.
     * More extreme emotions = more memorable.
     */
    private double calculateImportance(MemoryType type) {
        double impact = Math.abs(type.getBaseEmotionalImpact());

        // Very emotional events are very memorable
        if (impact > 0.6) return 1.0;
        if (impact > 0.4) return 0.8;
        if (impact > 0.2) return 0.6;
        return 0.4; // Base importance
    }

    /**
     * Get how many hours ago this memory happened.
     */
    public double getHoursAgo() {
        long diff = System.currentTimeMillis() - timestamp;
        return diff / (1000.0 * 60.0 * 60.0);
    }

    /**
     * Get how many days ago this memory happened.
     */
    public double getDaysAgo() {
        return getHoursAgo() / 24.0;
    }

    /**
     * Check if this memory is recent (within last hour).
     */
    public boolean isRecent() {
        return getHoursAgo() < 1.0;
    }

    /**
     * Consolidate this memory over time (called periodically).
     * Memories strengthen gradually based on arousal level and time.
     * Scientific basis: Memory consolidation in hippocampus.
     */
    public void consolidate() {
        double hoursElapsed = getHoursAgo();
        if (hoursElapsed > 0 && strength < 1.0) {
            // Consolidation formula: strength increases logarithmically
            double consolidationProgress = consolidationRate * Math.log1p(hoursElapsed);
            strength = Math.min(1.0, 0.3 + consolidationProgress);
        }
    }

    /**
     * Get effective emotional impact weighted by memory strength.
     * Stronger memories have more influence on current mood.
     */
    public double getEffectiveEmotionalImpact() {
        return emotionalImpact * strength;
    }

    /**
     * Get weighted importance for mood calculation.
     * Recent strong memories influence mood more than old weak ones.
     */
    public double getWeightedImportance() {
        double recencyWeight = Math.max(0.1, 1.0 - (getHoursAgo() / 168.0)); // Decay over 1 week
        return importance * strength * recencyWeight;
    }

    /**
     * Check if this memory is about a specific player.
     */
    public boolean isAboutPlayer(UUID uuid) {
        return playerUuid != null && playerUuid.equals(uuid.toString());
    }

    /**
     * Get a human-readable time description.
     */
    public String getTimeDescription() {
        double hours = getHoursAgo();
        if (hours < 0.1) return "à l'instant";
        if (hours < 1) return "il y a quelques minutes";
        if (hours < 2) return "il y a une heure";
        if (hours < 24) return String.format("il y a %.0f heures", hours);

        double days = getDaysAgo();
        if (days < 2) return "hier";
        if (days < 7) return String.format("il y a %.0f jours", days);

        return "il y a longtemps";
    }

    /**
     * STANFORD GENERATIVE AGENTS: Memory Retrieval Score
     * Combines recency, importance, and relevance to surface the most relevant memories.
     *
     * Formula: score = α_recency × recency + α_importance × importance + α_relevance × relevance
     * With all α = 1 (equal weights)
     *
     * @param query The current context/query to match against
     * @param lastAccessTime Last time this memory was accessed (for recency decay)
     * @return Retrieval score [0, 1] normalized
     */
    public double getRetrievalScore(String query, long lastAccessTime) {
        double recency = calculateRecency(lastAccessTime);
        double importance = this.importance; // Already normalized [0, 1]
        double relevance = calculateRelevance(query);

        // Combine with equal weights (α = 1 for all)
        double score = recency + importance + relevance;

        // Normalize to [0, 1] (max possible = 3.0)
        return score / 3.0;
    }

    /**
     * Calculate recency score using exponential decay.
     * Formula: 0.995^hours_elapsed
     *
     * SCIENTIFIC BASIS: Recent memories are more accessible (recency effect).
     */
    private double calculateRecency(long lastAccessTime) {
        double hoursElapsed = (System.currentTimeMillis() - lastAccessTime) / (1000.0 * 60.0 * 60.0);
        return Math.pow(0.995, hoursElapsed);
    }

    /**
     * Calculate relevance score using word matching (simplified version).
     * TODO: Upgrade to cosine similarity with embeddings for better results.
     *
     * SCIENTIFIC BASIS: Contextually relevant memories are more likely to be retrieved.
     */
    private double calculateRelevance(String query) {
        if (query == null || query.isEmpty() || description == null || description.isEmpty()) {
            return 0.0;
        }

        // Normalize strings
        String queryLower = query.toLowerCase();
        String descLower = description.toLowerCase();

        // Split into words
        String[] queryWords = queryLower.split("\\s+");
        String[] descWords = descLower.split("\\s+");

        // Count matching words
        int matches = 0;
        for (String qWord : queryWords) {
            if (qWord.length() < 3) continue; // Skip short words
            for (String dWord : descWords) {
                if (dWord.contains(qWord) || qWord.contains(dWord)) {
                    matches++;
                    break;
                }
            }
        }

        // Normalize by query length
        return Math.min(1.0, (double) matches / Math.max(1, queryWords.length));
    }

    /**
     * Mark this memory as accessed (updates last access time for recency calculation).
     */
    public void markAccessed() {
        // Update timestamp to reflect recent access
        // This simulates "rehearsal" in memory consolidation
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get a formatted string for AI prompt.
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append("- ");
        if (playerName != null && !playerName.isEmpty()) {
            sb.append(playerName).append(": ");
        }
        sb.append(description);
        sb.append(" (").append(getTimeDescription()).append(")");

        return sb.toString();
    }

    // Getters and setters
    public MemoryType getType() {
        return type;
    }

    public void setType(MemoryType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public double getEmotionalImpact() {
        return emotionalImpact;
    }

    public void setEmotionalImpact(double emotionalImpact) {
        this.emotionalImpact = emotionalImpact;
    }

    public double getImportance() {
        return importance;
    }

    public void setImportance(double importance) {
        this.importance = importance;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public double getArousalLevel() {
        return arousalLevel;
    }

    public void setArousalLevel(double arousalLevel) {
        this.arousalLevel = arousalLevel;
    }

    public double getConsolidationRate() {
        return consolidationRate;
    }

    public void setConsolidationRate(double consolidationRate) {
        this.consolidationRate = consolidationRate;
    }

    @Override
    public String toString() {
        return String.format("Memory{type=%s, desc='%s', player=%s, impact=%.2f, strength=%.2f, %s}",
                type, description, playerName, emotionalImpact, strength, getTimeDescription());
    }
}
