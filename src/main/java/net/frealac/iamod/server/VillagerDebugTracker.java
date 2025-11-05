package net.frealac.iamod.server;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.behavior.BehaviorManager;
import net.frealac.iamod.ai.debug.VillagerDebugInfo;
import net.frealac.iamod.common.story.VillagerStory;
import net.frealac.iamod.common.story.VillagerStoryProvider;
import net.frealac.iamod.network.NetworkHandler;
import net.frealac.iamod.network.packet.SyncVillagerDebugS2CPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * Server-side tracker that monitors nearby villagers and sends debug information to clients.
 * Runs every few ticks to update player HUDs with real-time villager state.
 */
@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID)
public class VillagerDebugTracker {

    private static final double MAX_DISTANCE = 32.0; // Maximum distance to track villagers (32 blocks)
    private static final int UPDATE_INTERVAL = 5; // Update every 5 ticks (4 times per second)

    private static int tickCounter = 0;

    /**
     * Server tick event - update debug info for all players
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Only run on tick end to avoid conflicts
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Update every N ticks for performance
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;

        // Process each player on the server
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                updateDebugForPlayer(player);
            }
        }
    }

    /**
     * Update debug information for a specific player
     */
    private static void updateDebugForPlayer(ServerPlayer player) {
        // Find nearest villager within range
        Villager nearestVillager = findNearestVillager(player);

        if (nearestVillager == null) {
            // No villager nearby - clear HUD
            NetworkHandler.CHANNEL.send(
                new SyncVillagerDebugS2CPacket(false),
                PacketDistributor.PLAYER.with(player)
            );
            return;
        }

        // Get villager's story capability
        VillagerStory story = nearestVillager.getCapability(VillagerStoryProvider.CAPABILITY)
            .resolve()
            .map(cap -> cap.getStory())
            .orElse(null);

        // Get behavior manager and create debug info
        BehaviorManager behaviorManager = BehaviorManager.getOrCreate(nearestVillager);

        // Safety check - if story is null, send empty packet
        if (story == null) {
            NetworkHandler.CHANNEL.send(
                new SyncVillagerDebugS2CPacket(false),
                PacketDistributor.PLAYER.with(player)
            );
            return;
        }

        VillagerDebugInfo debugInfo = behaviorManager.createDebugInfo(story, player);

        // Send debug info to client
        NetworkHandler.CHANNEL.send(
            new SyncVillagerDebugS2CPacket(debugInfo),
            PacketDistributor.PLAYER.with(player)
        );
    }

    /**
     * Find the nearest villager to a player within MAX_DISTANCE
     */
    private static Villager findNearestVillager(ServerPlayer player) {
        // Create bounding box around player
        AABB searchBox = new AABB(
            player.getX() - MAX_DISTANCE,
            player.getY() - MAX_DISTANCE,
            player.getZ() - MAX_DISTANCE,
            player.getX() + MAX_DISTANCE,
            player.getY() + MAX_DISTANCE,
            player.getZ() + MAX_DISTANCE
        );

        // Get all villagers in range
        List<Villager> nearbyVillagers = player.level()
            .getEntitiesOfClass(Villager.class, searchBox);

        if (nearbyVillagers.isEmpty()) {
            return null;
        }

        // Find the closest one
        Villager nearest = null;
        double minDistance = MAX_DISTANCE * MAX_DISTANCE; // Use squared distance for performance

        for (Villager villager : nearbyVillagers) {
            double distSq = villager.distanceToSqr(player);
            if (distSq < minDistance) {
                minDistance = distSq;
                nearest = villager;
            }
        }

        // Only return if within max distance
        return (nearest != null && Math.sqrt(minDistance) <= MAX_DISTANCE) ? nearest : null;
    }
}
