
package net.frealac.iamod.mixin;

import net.frealac.iamod.client.hud.VillagerDebugOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Minecraft's Gui class to render our custom HUD overlay.
 * This is necessary because Forge 1.21 removed all GUI overlay events.
 */
@Mixin(value = Gui.class, remap = false)
public class GuiMixin {

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void onRenderGui(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VillagerDebugOverlay.renderHud(guiGraphics);
    }
}
