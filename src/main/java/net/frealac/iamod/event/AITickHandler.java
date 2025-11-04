package net.frealac.iamod.event;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.behavior.BehaviorManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Event handler for AI system tick updates.
 * Manages BehaviorManagers for all AI entities.
 */
@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AITickHandler {
    // WeakHashMap to automatically clean up when entities are removed
    private static final Map<Mob, BehaviorManager> behaviorManagers = new WeakHashMap<>();

    /**
     * Initialize AI for entities when they join the level.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Only on server side
        if (event.getLevel().isClientSide()) return;

        // Check if entity is a Villager (can be extended to other mobs)
        if (event.getEntity() instanceof Villager villager) {
            // Create and register BehaviorManager with Behavior Tree mode for villagers
            BehaviorManager manager = new BehaviorManager(
                villager,
                BehaviorManager.BehaviorMode.BEHAVIOR_TREE
            );
            behaviorManagers.put(villager, manager);

            IAMOD.LOGGER.debug("Initialized BehaviorTree AI for villager: {}", villager.getId());
        }
        // For other mobs, you could use GOALS mode:
        // else if (event.getEntity() instanceof Mob mob) {
        //     BehaviorManager manager = new BehaviorManager(mob, BehaviorManager.BehaviorMode.GOALS);
        //     behaviorManagers.put(mob, manager);
        // }
    }

    /**
     * Tick all registered AI systems.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Tick all behavior managers
        behaviorManagers.forEach((mob, manager) -> {
            if (mob.isAlive() && !mob.isRemoved()) {
                try {
                    manager.tick();
                } catch (Exception e) {
                    IAMOD.LOGGER.error("Error ticking AI for mob {}: {}", mob.getId(), e.getMessage());
                }
            }
        });
    }

    /**
     * Get the BehaviorManager for a specific mob.
     * Useful for external systems that want to interact with the AI.
     */
    public static BehaviorManager getBehaviorManager(Mob mob) {
        return behaviorManagers.get(mob);
    }

    /**
     * Manually register a BehaviorManager for a mob.
     */
    public static void registerBehaviorManager(Mob mob, BehaviorManager manager) {
        behaviorManagers.put(mob, manager);
    }

    /**
     * Remove a BehaviorManager for a mob.
     */
    public static void unregisterBehaviorManager(Mob mob) {
        behaviorManagers.remove(mob);
    }

    /**
     * Get the count of registered AI systems.
     */
    public static int getRegisteredCount() {
        return behaviorManagers.size();
    }

    /**
     * Clear all registered AI systems.
     * Should only be called when shutting down the server.
     */
    public static void clearAll() {
        behaviorManagers.clear();
    }
}
