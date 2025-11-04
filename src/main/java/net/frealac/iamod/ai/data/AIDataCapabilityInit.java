package net.frealac.iamod.ai.data;

import net.frealac.iamod.IAMOD;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles attachment of AI data capability to entities.
 */
@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AIDataCapabilityInit {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Mob mob) {
            // Attach AI data capability to all mobs (or specific types)
            // You can filter here if you only want certain entities to have AI data
            if (mob instanceof Villager) {
                AIDataProvider provider = new AIDataProvider();
                event.addCapability(
                    new net.minecraft.resources.ResourceLocation(IAMOD.MOD_ID, "ai_data"),
                    provider
                );
            }
        }
    }
}
