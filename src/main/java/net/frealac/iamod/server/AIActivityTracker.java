
package net.frealac.iamod.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks AI activity for debug HUD display.
 * Monitors when AI is processing responses and when memories are written.
 */
public class AIActivityTracker {

    private static final Map<Integer, ActivityData> villagerActivity = new HashMap<>();

    public static class ActivityData {
        public boolean isAiProcessing;
        public String lastAiActivity;
        public int conversationMessageCount;
        public String lastMemoryAction;

        public ActivityData() {
            this.isAiProcessing = false;
            this.lastAiActivity = "Jamais";
            this.conversationMessageCount = 0;
            this.lastMemoryAction = "Aucune";
        }
    }

    /**
     * Mark that AI started processing for a villager
     */
    public static void startAiProcessing(int villagerId) {
        ActivityData data = getOrCreate(villagerId);
        data.isAiProcessing = true;
        data.lastAiActivity = getCurrentTime() + " - Génération réponse...";
    }

    /**
     * Mark that AI finished processing for a villager
     */
    public static void finishAiProcessing(int villagerId, boolean success) {
        ActivityData data = getOrCreate(villagerId);
        data.isAiProcessing = false;
        if (success) {
            data.lastAiActivity = getCurrentTime() + " - Réponse envoyée";
            data.conversationMessageCount++;
        } else {
            data.lastAiActivity = getCurrentTime() + " - Erreur IA";
        }
    }

    /**
     * Mark that a memory was written
     */
    public static void recordMemoryWrite(int villagerId, String memoryDescription) {
        ActivityData data = getOrCreate(villagerId);
        String shortDesc = memoryDescription.length() > 30
            ? memoryDescription.substring(0, 30) + "..."
            : memoryDescription;
        data.lastMemoryAction = getCurrentTime() + " - " + shortDesc;
    }

    /**
     * Get activity data for a villager
     */
    public static ActivityData getActivity(int villagerId) {
        return getOrCreate(villagerId);
    }

    /**
     * Get or create activity data for a villager
     */
    private static ActivityData getOrCreate(int villagerId) {
        return villagerActivity.computeIfAbsent(villagerId, k -> new ActivityData());
    }

    /**
     * Clear activity data for a villager (when villager is removed)
     */
    public static void clear(int villagerId) {
        villagerActivity.remove(villagerId);
    }

    /**
     * Get current time as formatted string
     */
    private static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }
}
