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

    public Memory() {
        this.timestamp = System.currentTimeMillis();
    }

    public Memory(MemoryType type, String description, UUID playerUuid, String playerName) {
        this();
        this.type = type;
        this.description = description;
        this.playerUuid = playerUuid != null ? playerUuid.toString() : null;
        this.playerName = playerName;
        this.emotionalImpact = type.getBaseEmotionalImpact();
        this.importance = calculateImportance(type);
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

    @Override
    public String toString() {
        return String.format("Memory{type=%s, desc='%s', player=%s, impact=%.2f, %s}",
                type, description, playerName, emotionalImpact, getTimeDescription());
    }
}
