package net.frealac.iamod.ai.memory;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.common.story.IVillagerStory;
import net.frealac.iamod.common.story.VillagerStory;
import net.frealac.iamod.common.story.VillagerStoryProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Automatically captures game events and creates memories for villagers.
 * This makes villagers remember who hit them, helped them, etc.
 */
@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MemoryEventListener {

    /**
     * When a villager is hurt, remember who hurt them.
     */
    @SubscribeEvent
    public static void onVillagerHurt(LivingHurtEvent event) {
        // Check if a villager was hurt
        if (!(event.getEntity() instanceof Villager villager)) return;

        // Check if hurt by a player
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof Player player) {
            // Get villager story
            VillagerStory story = getVillagerStory(villager);
            if (story == null) return;

            // Add negative memory
            if (story.interactionMemory == null) {
                story.interactionMemory = new VillagerMemory();
            }

            float damage = event.getAmount();
            String description = String.format("%s m'a frappé (dégâts: %.1f) - Je m'en souviendrai !",
                    player.getName().getString(), damage);

            story.interactionMemory.addMemory(
                    MemoryType.WAS_HIT,
                    description,
                    player.getUUID(),
                    player.getName().getString()
            );

            IAMOD.LOGGER.info("Villager {} remembers being hit by {}",
                    getVillagerName(story), player.getName().getString());

            // Update mood/psychology if available
            if (story.psychology != null) {
                // Being hit decreases mood and increases stress
                story.psychology.moodBaseline = Math.max(-1.0, story.psychology.moodBaseline - 0.1);
                story.psychology.stress = Math.min(1.0, story.psychology.stress + 0.2);

                IAMOD.LOGGER.info("Villager mood decreased to {} and stress increased to {}",
                        story.psychology.moodBaseline, story.psychology.stress);
            }
        }
    }

    /**
     * When a player right-clicks a villager with an item (gift).
     */
    @SubscribeEvent
    public static void onPlayerInteractWithVillager(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;

        Player player = event.getEntity();
        VillagerStory story = getVillagerStory(villager);
        if (story == null) return;

        if (story.interactionMemory == null) {
            story.interactionMemory = new VillagerMemory();
        }

        // Check if player is holding valuable item (diamond, emerald, gold, food)
        String itemName = event.getItemStack().getDisplayName().getString();
        boolean isGift = itemName.toLowerCase().contains("diamond") ||
                        itemName.toLowerCase().contains("emerald") ||
                        itemName.toLowerCase().contains("gold") ||
                        itemName.toLowerCase().contains("bread") ||
                        itemName.toLowerCase().contains("apple");

        if (isGift && !event.getItemStack().isEmpty()) {
            String description = String.format("%s m'a donné %s - Quelle gentillesse !",
                    player.getName().getString(), itemName);

            story.interactionMemory.addMemory(
                    MemoryType.GIFT_RECEIVED,
                    description,
                    player.getUUID(),
                    player.getName().getString()
            );

            IAMOD.LOGGER.info("Villager {} received gift from {}",
                    getVillagerName(story), player.getName().getString());

            // Improve mood
            if (story.psychology != null) {
                story.psychology.moodBaseline = Math.min(1.0, story.psychology.moodBaseline + 0.2);
                story.psychology.stress = Math.max(0.0, story.psychology.stress - 0.1);
            }
        }
    }

    /**
     * When a villager witnesses another villager's death.
     */
    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager dyingVillager)) return;

        // Find nearby villagers who witness this
        var nearbyVillagers = dyingVillager.level().getEntitiesOfClass(
                Villager.class,
                dyingVillager.getBoundingBox().inflate(16.0),
                v -> v != dyingVillager && v.isAlive()
        );

        for (Villager witness : nearbyVillagers) {
            VillagerStory story = getVillagerStory(witness);
            if (story == null) continue;

            if (story.interactionMemory == null) {
                story.interactionMemory = new VillagerMemory();
            }

            // Check if killed by player
            if (event.getSource().getEntity() instanceof Player killer) {
                String description = String.format("J'ai vu %s tuer un villageois... C'est horrible !",
                        killer.getName().getString());

                story.interactionMemory.addMemory(
                        MemoryType.WITNESSED_VIOLENCE,
                        description,
                        killer.getUUID(),
                        killer.getName().getString()
                );

                // Increase stress and fear
                if (story.psychology != null) {
                    story.psychology.stress = Math.min(1.0, story.psychology.stress + 0.5);
                    story.psychology.moodBaseline = Math.max(-1.0, story.psychology.moodBaseline - 0.3);
                }

                IAMOD.LOGGER.info("Villager {} witnessed killing by {}",
                        getVillagerName(story), killer.getName().getString());
            }
        }
    }

    /**
     * Get VillagerStory from villager entity.
     */
    private static VillagerStory getVillagerStory(Villager villager) {
        try {
            var cap = villager.getCapability(VillagerStoryProvider.CAPABILITY);
            return cap.map(IVillagerStory::getStory).orElse(null);
        } catch (Exception e) {
            IAMOD.LOGGER.error("Failed to get VillagerStory", e);
            return null;
        }
    }

    /**
     * Get villager display name.
     */
    private static String getVillagerName(VillagerStory story) {
        if (story.nameGiven != null && !story.nameGiven.isEmpty()) {
            return story.nameGiven;
        }
        return "Villageois";
    }
}
