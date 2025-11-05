package net.frealac.iamod.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.debug.VillagerDebugInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
// TODO: RegisterGuiLayersEvent removed in Forge 1.21 - need to find correct Minecraft 1.21 LayeredDraw registration API
// import net.minecraftforge.client.event.RegisterGuiLayersEvent;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders the villager debug HUD overlay using Minecraft's native LayeredDraw system.
 * Displays real-time villager information in top-right corner.
 *
 * TODO: Registration temporarily disabled - Forge 1.21 removed RegisterGuiLayersEvent.
 * Need to find correct Minecraft 1.21 native GUI layer registration method.
 */
// @Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class VillagerDebugRenderer {

    /**
     * Register the debug HUD layer using Minecraft's native system
     * TODO: Currently disabled - need correct Forge 1.21 / Minecraft 1.21 API
     */
    /*
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(IAMOD.MOD_ID, "villager_debug"),
            VillagerDebugRenderer::renderDebugOverlay
        );
    }
    */

    /**
     * Render the debug HUD overlay
     */
    private static void renderDebugOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        VillagerDebugInfo debugInfo = VillagerDebugHud.getCurrentDebugInfo();

        if (debugInfo == null) {
            return; // Nothing to display
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        // Get screen dimensions
        int screenWidth = mc.getWindow().getGuiScaledWidth();

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
        RenderSystem.enableBlend();
        for (String line : lines) {
            graphics.drawString(mc.font, line, x, y, 0xFFFFFF, true);
            y += lineHeight;
        }
        RenderSystem.disableBlend();
    }
}
