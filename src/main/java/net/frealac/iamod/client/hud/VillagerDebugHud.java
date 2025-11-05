package net.frealac.iamod.client.hud;

import net.frealac.iamod.ai.debug.VillagerDebugInfo;

/**
 * Client-side HUD storage for villager debug information.
 * Stores debug info received from server for display in HUD.
 *
 * The actual rendering is done via RegisterGuiOverlaysEvent on mod initialization.
 */
public class VillagerDebugHud {

    private static VillagerDebugInfo currentDebugInfo = null;
    private static long lastUpdateTime = 0;
    private static final long TIMEOUT_MS = 2000; // Hide HUD after 2 seconds without updates

    /**
     * Update debug info from server packet
     */
    public static void setDebugInfo(VillagerDebugInfo info) {
        currentDebugInfo = info;
        lastUpdateTime = System.currentTimeMillis();
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
        // Check if timed out
        if (currentDebugInfo != null) {
            long timeSinceUpdate = System.currentTimeMillis() - lastUpdateTime;
            if (timeSinceUpdate > TIMEOUT_MS) {
                currentDebugInfo = null;
            }
        }
        return currentDebugInfo;
    }

    /**
     * Check if we have valid debug info to display
     */
    public static boolean hasDebugInfo() {
        return getCurrentDebugInfo() != null;
    }
}
