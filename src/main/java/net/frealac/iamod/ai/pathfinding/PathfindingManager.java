package net.frealac.iamod.ai.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages pathfinding operations for AI entities.
 * Supports asynchronous pathfinding to avoid blocking the main game thread.
 */
public class PathfindingManager {
    private static final PathfindingManager INSTANCE = new PathfindingManager();
    private final Map<Level, AdvancedPathfinder> pathfinders = new HashMap<>();
    private final ExecutorService executorService;

    private PathfindingManager() {
        // Create a thread pool for async pathfinding
        this.executorService = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread thread = new Thread(r, "AI-Pathfinding-Thread");
                thread.setDaemon(true);
                return thread;
            }
        );
    }

    public static PathfindingManager getInstance() {
        return INSTANCE;
    }

    /**
     * Get or create a pathfinder for a level.
     */
    private AdvancedPathfinder getPathfinder(Level level) {
        return pathfinders.computeIfAbsent(level, l -> new AdvancedPathfinder(l));
    }

    /**
     * Find a path synchronously (blocks the current thread).
     */
    public List<BlockPos> findPathSync(Level level, BlockPos start, BlockPos goal) {
        return getPathfinder(level).findPath(start, goal);
    }

    /**
     * Find a path asynchronously (returns a Future).
     */
    public CompletableFuture<List<BlockPos>> findPathAsync(Level level, BlockPos start, BlockPos goal) {
        return CompletableFuture.supplyAsync(
            () -> getPathfinder(level).findPath(start, goal),
            executorService
        );
    }

    /**
     * Find a path asynchronously with a callback.
     */
    public void findPathAsync(Level level, BlockPos start, BlockPos goal, PathCallback callback) {
        findPathAsync(level, start, goal).thenAccept(path -> {
            // Execute callback on main thread
            level.getServer().execute(() -> callback.onPathFound(path));
        });
    }

    /**
     * Clear all pathfinding caches.
     */
    public void clearAllCaches() {
        pathfinders.values().forEach(AdvancedPathfinder::clearCache);
    }

    /**
     * Clear cache for a specific level.
     */
    public void clearCache(Level level) {
        AdvancedPathfinder pathfinder = pathfinders.get(level);
        if (pathfinder != null) {
            pathfinder.clearCache();
        }
    }

    /**
     * Shutdown the pathfinding manager (should be called on server stop).
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        pathfinders.clear();
    }

    /**
     * Callback interface for async pathfinding results.
     */
    @FunctionalInterface
    public interface PathCallback {
        void onPathFound(List<BlockPos> path);
    }
}
