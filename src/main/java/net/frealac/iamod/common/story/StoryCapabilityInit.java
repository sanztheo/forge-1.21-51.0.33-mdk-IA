package net.frealac.iamod.common.story;

import net.frealac.iamod.IAMOD;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class StoryCapabilityInit {
    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(IVillagerStory.class);
    }
}

@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
class StoryCapabilityAttach {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Villager) {
            event.addCapability(VillagerStoryProvider.KEY, new VillagerStoryProvider());
        }
    }
}

