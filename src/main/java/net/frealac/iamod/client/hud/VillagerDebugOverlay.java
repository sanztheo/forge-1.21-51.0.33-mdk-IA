package net.frealac.iamod.client.hud;

import net.frealac.iamod.ai.debug.VillagerDebugInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD Overlay that renders villager debug information constantly.
 * Called from GuiMixin which injects into Minecraft's Gui.render() method.
 * This approach is necessary because Forge 1.21 removed all GUI overlay events.
 */
public class VillagerDebugOverlay {

    /**
     * Render HUD - called from GuiMixin
     */
    public static void renderHud(GuiGraphics guiGraphics) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null || mc.font == null) {
                return;
            }

            renderVillagerDebugHud(guiGraphics, mc);
        } catch (Exception e) {
            // Silently ignore rendering errors to prevent crashes
        }
    }

    /**
     * Render the villager debug HUD in top-right corner
     */
    private static void renderVillagerDebugHud(GuiGraphics graphics, Minecraft mc) {
        VillagerDebugInfo debugInfo = VillagerDebugHud.getCurrentDebugInfo();

        if (debugInfo == null) {
            return; // Nothing to display
        }

        if (mc.options.hideGui) {
            return;
        }

        // Get screen dimensions
        int screenWidth = graphics.guiWidth();

        // Calculate position (top-right corner with padding)
        int padding = 10;
        int lineHeight = 10;
        int x = screenWidth - 250 - padding; // 250 pixels wide
        int y = padding;

        // Get all lines to display
        String[] lines = debugInfo.toHudLines();

        // Draw semi-transparent background
        int bgWidth = 240;
        int bgHeight = lines.length * lineHeight + 10;
        graphics.fill(x - 5, y - 5, x + bgWidth, y + bgHeight, 0x80000000); // Semi-transparent black

        // Draw each line
        for (String line : lines) {
            graphics.drawString(mc.font, line, x, y, 0xFFFFFF, true);
            y += lineHeight;
        }
    }
}

