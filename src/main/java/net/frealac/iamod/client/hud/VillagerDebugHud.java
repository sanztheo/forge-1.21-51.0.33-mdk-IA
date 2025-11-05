package net.frealac.iamod.client.hud;

import net.frealac.iamod.ai.debug.VillagerDebugInfo;

/**
 * Client-side HUD storage for villager debug information.
 * Stores debug info received from server for display in HUD.
 *
 * HUD is displayed constantly without timeout - always shows nearest villager info.
 */
public class VillagerDebugHud {

    private static VillagerDebugInfo currentDebugInfo = null;

    /**
     * Update debug info from server packet
     */
    public static void setDebugInfo(VillagerDebugInfo info) {
        currentDebugInfo = info;
    }

    /**
     * Clear debug info (no villager nearby)
     */
    public static void clearDebugInfo() {
        currentDebugInfo = null;
    }

    /**
     * Get current debug info (for rendering)
     */
    public static VillagerDebugInfo getCurrentDebugInfo() {
        return currentDebugInfo;
    }

    /**
     * Check if we have valid debug info to display
     */
    public static boolean hasDebugInfo() {
        return getCurrentDebugInfo() != null;
    }
}
