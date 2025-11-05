package net.frealac.iamod.ai.debug;

import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug information about a villager's current state.
 * Used to display real-time villager actions in HUD.
 * Shows EVERYTHING: actions, goals, memories, sentiment, psychology.
 */
public class VillagerDebugInfo {

    private String villagerName;
    private String currentAction;
    private String currentGoal;
    private String targetPlayer;
    private double mood;
    private double stress;
    private double resilience;
    private double sleepQuality;
    private int memoryCount;
    private double sentiment; // Sentiment towards current player
    private List<String> recentMemories; // Last 5 memories
    private double distanceToPlayer;

    // AI Activity tracking (NEW)
    private boolean isAiProcessing; // Is AI currently generating a response?
    private String lastAiActivity; // Last AI activity timestamp
    private int conversationMessageCount; // Number of messages in conversation
    private String lastMemoryAction; // Last memory write action

    public VillagerDebugInfo() {
        this.villagerName = "Villageois";
        this.currentAction = "Inactif";
        this.currentGoal = "Aucun";
        this.targetPlayer = "Aucun";
        this.mood = 0.0;
        this.stress = 0.0;
        this.resilience = 0.0;
        this.sleepQuality = 0.0;
        this.memoryCount = 0;
        this.sentiment = 0.0;
        this.recentMemories = new ArrayList<>();
        this.distanceToPlayer = 0.0;
        this.isAiProcessing = false;
        this.lastAiActivity = "Jamais";
        this.conversationMessageCount = 0;
        this.lastMemoryAction = "Aucune";
    }

    public VillagerDebugInfo(String villagerName, String currentAction, String currentGoal,
                            String targetPlayer, double mood, double stress, double resilience,
                            double sleepQuality, int memoryCount, double sentiment,
                            List<String> recentMemories, double distance,
                            boolean isAiProcessing, String lastAiActivity, int conversationMessageCount,
                            String lastMemoryAction) {
        this.villagerName = villagerName;
        this.currentAction = currentAction;
        this.currentGoal = currentGoal;
        this.targetPlayer = targetPlayer;
        this.mood = mood;
        this.stress = stress;
        this.resilience = resilience;
        this.sleepQuality = sleepQuality;
        this.memoryCount = memoryCount;
        this.sentiment = sentiment;
        this.recentMemories = recentMemories != null ? recentMemories : new ArrayList<>();
        this.distanceToPlayer = distance;
        this.isAiProcessing = isAiProcessing;
        this.lastAiActivity = lastAiActivity;
        this.conversationMessageCount = conversationMessageCount;
        this.lastMemoryAction = lastMemoryAction;
    }

    // Serialization for network packet
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(villagerName);
        buf.writeUtf(currentAction);
        buf.writeUtf(currentGoal);
        buf.writeUtf(targetPlayer);
        buf.writeDouble(mood);
        buf.writeDouble(stress);
        buf.writeDouble(resilience);
        buf.writeDouble(sleepQuality);
        buf.writeInt(memoryCount);
        buf.writeDouble(sentiment);

        // Write memories list
        buf.writeInt(recentMemories.size());
        for (String memory : recentMemories) {
            buf.writeUtf(memory);
        }

        buf.writeDouble(distanceToPlayer);

        // Write AI activity tracking (NEW)
        buf.writeBoolean(isAiProcessing);
        buf.writeUtf(lastAiActivity);
        buf.writeInt(conversationMessageCount);
        buf.writeUtf(lastMemoryAction);
    }

    public static VillagerDebugInfo read(FriendlyByteBuf buf) {
        String villagerName = buf.readUtf();
        String currentAction = buf.readUtf();
        String currentGoal = buf.readUtf();
        String targetPlayer = buf.readUtf();
        double mood = buf.readDouble();
        double stress = buf.readDouble();
        double resilience = buf.readDouble();
        double sleepQuality = buf.readDouble();
        int memoryCount = buf.readInt();
        double sentiment = buf.readDouble();

        // Read memories list
        int memoriesSize = buf.readInt();
        List<String> memories = new ArrayList<>();
        for (int i = 0; i < memoriesSize; i++) {
            memories.add(buf.readUtf());
        }

        double distance = buf.readDouble();

        // Read AI activity tracking (NEW)
        boolean isAiProcessing = buf.readBoolean();
        String lastAiActivity = buf.readUtf();
        int conversationMessageCount = buf.readInt();
        String lastMemoryAction = buf.readUtf();

        return new VillagerDebugInfo(villagerName, currentAction, currentGoal,
                                    targetPlayer, mood, stress, resilience, sleepQuality,
                                    memoryCount, sentiment, memories, distance,
                                    isAiProcessing, lastAiActivity, conversationMessageCount,
                                    lastMemoryAction);
    }

    // Getters
    public String getVillagerName() { return villagerName; }
    public String getCurrentAction() { return currentAction; }
    public String getCurrentGoal() { return currentGoal; }
    public String getTargetPlayer() { return targetPlayer; }
    public double getMood() { return mood; }
    public double getStress() { return stress; }
    public double getResilience() { return resilience; }
    public double getSleepQuality() { return sleepQuality; }
    public int getMemoryCount() { return memoryCount; }
    public double getSentiment() { return sentiment; }
    public List<String> getRecentMemories() { return recentMemories; }
    public double getDistanceToPlayer() { return distanceToPlayer; }
    public boolean isAiProcessing() { return isAiProcessing; }
    public String getLastAiActivity() { return lastAiActivity; }
    public int getConversationMessageCount() { return conversationMessageCount; }
    public String getLastMemoryAction() { return lastMemoryAction; }

    // Setters
    public void setVillagerName(String name) { this.villagerName = name; }
    public void setCurrentAction(String action) { this.currentAction = action; }
    public void setCurrentGoal(String goal) { this.currentGoal = goal; }
    public void setTargetPlayer(String player) { this.targetPlayer = player; }
    public void setMood(double mood) { this.mood = mood; }
    public void setStress(double stress) { this.stress = stress; }
    public void setResilience(double resilience) { this.resilience = resilience; }
    public void setSleepQuality(double sleepQuality) { this.sleepQuality = sleepQuality; }
    public void setMemoryCount(int count) { this.memoryCount = count; }
    public void setSentiment(double sentiment) { this.sentiment = sentiment; }
    public void setRecentMemories(List<String> memories) { this.recentMemories = memories; }
    public void setDistanceToPlayer(double distance) { this.distanceToPlayer = distance; }

    /**
     * Format for HUD display with ALL information
     */
    public String[] toHudLines() {
        List<String> lines = new ArrayList<>();

        lines.add("§6§l=== VILLAGEOIS DEBUG ===");
        lines.add("§fNom: §e" + villagerName);
        lines.add(String.format("§fDistance: §e%.1fm", distanceToPlayer));

        lines.add("§6--- ACTION EN COURS ---");
        lines.add("§fAction: §a" + currentAction);
        lines.add("§fGoal: §b" + currentGoal);
        lines.add("§fCible: §d" + targetPlayer);

        lines.add("§6--- PSYCHOLOGIE ---");
        lines.add(String.format("§fHumeur: %s%.2f %s", getMoodColor(), mood, getMoodLabel()));
        lines.add(String.format("§fStress: %s%.2f %s", getStressColor(), stress, getStressLabel()));
        lines.add(String.format("§fRésilience: §b%.2f", resilience));
        lines.add(String.format("§fSommeil: %s%.2f", getSleepColor(), sleepQuality));

        lines.add("§6--- RELATION AVEC TOI ---");
        lines.add(String.format("§fSentiment: %s%.2f %s", getSentimentColor(), sentiment, getSentimentLabel()));
        lines.add("§fMémoires total: §e" + memoryCount);

        // AI ACTIVITY SECTION (NEW)
        lines.add("§6--- ACTIVITÉ IA ---");
        if (isAiProcessing) {
            lines.add("§aIA: §e⚡ EN TRAIN DE RÉPONDRE...");
        } else {
            lines.add("§7IA: Inactive");
        }
        lines.add("§fDernière activité: §e" + lastAiActivity);
        lines.add("§fMessages conversation: §e" + conversationMessageCount);
        lines.add("§fDernière mémoire: §e" + lastMemoryAction);

        if (!recentMemories.isEmpty()) {
            lines.add("§6--- SOUVENIRS RÉCENTS ---");
            for (int i = 0; i < Math.min(5, recentMemories.size()); i++) {
                lines.add("§7• §f" + recentMemories.get(i));
            }
        }

        lines.add("§6§l========================");

        return lines.toArray(new String[0]);
    }

    private String getMoodColor() {
        if (mood > 0.5) return "§a"; // Green - very happy
        if (mood > 0.2) return "§2"; // Dark green - happy
        if (mood < -0.5) return "§c"; // Red - very sad
        if (mood < -0.2) return "§4"; // Dark red - sad
        return "§7"; // Gray - neutral
    }

    private String getMoodLabel() {
        if (mood > 0.5) return "§a(Joyeux)";
        if (mood > 0.2) return "§2(Content)";
        if (mood < -0.5) return "§c(Déprimé)";
        if (mood < -0.2) return "§4(Triste)";
        return "§7(Neutre)";
    }

    private String getStressColor() {
        if (stress > 0.7) return "§c"; // Red - very high stress
        if (stress > 0.4) return "§6"; // Orange - medium stress
        if (stress > 0.2) return "§e"; // Yellow - low stress
        return "§a"; // Green - calm
    }

    private String getStressLabel() {
        if (stress > 0.7) return "§c(Très stressé)";
        if (stress > 0.4) return "§6(Stressé)";
        if (stress > 0.2) return "§e(Un peu tendu)";
        return "§a(Calme)";
    }

    private String getSleepColor() {
        if (sleepQuality > 0.7) return "§a"; // Green - well rested
        if (sleepQuality > 0.4) return "§e"; // Yellow - tired
        return "§c"; // Red - exhausted
    }

    private String getSentimentColor() {
        if (sentiment > 0.5) return "§a"; // Green - likes you
        if (sentiment > 0.2) return "§2"; // Dark green - friendly
        if (sentiment < -0.5) return "§c"; // Red - hates you
        if (sentiment < -0.2) return "§4"; // Dark red - dislikes you
        return "§7"; // Gray - neutral
    }

    private String getSentimentLabel() {
        if (sentiment > 0.5) return "§a(T'aime bien)";
        if (sentiment > 0.2) return "§2(Amical)";
        if (sentiment < -0.5) return "§c(Te déteste)";
        if (sentiment < -0.2) return "§4(T'aime pas)";
        return "§7(Neutre)";
    }
}
