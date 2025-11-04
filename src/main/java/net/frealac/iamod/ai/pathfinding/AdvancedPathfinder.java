package net.frealac.iamod.ai.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Advanced A* pathfinding implementation for AI entities.
 * Provides better pathfinding than the default Minecraft pathfinding.
 */
public class AdvancedPathfinder {
    private final Level level;
    private final int maxSearchNodes;
    private final PathCache cache;

    private static final Direction[] DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
        Direction.UP, Direction.DOWN
    };

    public AdvancedPathfinder(Level level, int maxSearchNodes) {
        this.level = level;
        this.maxSearchNodes = maxSearchNodes;
        this.cache = new PathCache(100); // Cache last 100 paths
    }

    public AdvancedPathfinder(Level level) {
        this(level, 1000);
    }

    /**
     * Find a path from start to goal using A* algorithm.
     * @param start Starting position
     * @param goal Goal position
     * @return List of positions representing the path, or empty list if no path found
     */
    public List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        // Check cache first
        List<BlockPos> cachedPath = cache.getPath(start, goal);
        if (cachedPath != null) {
            return cachedPath;
        }

        // A* algorithm
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, PathNode> allNodes = new HashMap<>();

        PathNode startNode = new PathNode(start);
        startNode.calculateCosts(goal, 0);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int nodesExplored = 0;

        while (!openSet.isEmpty() && nodesExplored < maxSearchNodes) {
            PathNode current = openSet.poll();
            nodesExplored++;

            // Goal reached
            if (current.pos.equals(goal)) {
                List<BlockPos> path = reconstructPath(current);
                cache.cachePath(start, goal, path);
                return path;
            }

            closedSet.add(current.pos);

            // Explore neighbors
            for (Direction dir : DIRECTIONS) {
                BlockPos neighborPos = current.pos.relative(dir);

                if (closedSet.contains(neighborPos)) continue;
                if (!isWalkable(neighborPos)) continue;

                PathNode neighbor = allNodes.computeIfAbsent(neighborPos, PathNode::new);

                double tentativeGCost = current.gCost + getCost(current.pos, neighborPos);

                if (tentativeGCost < neighbor.gCost || !openSet.contains(neighbor)) {
                    neighbor.parent = current;
                    neighbor.calculateCosts(goal, current.gCost);

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // No path found
        return Collections.emptyList();
    }

    /**
     * Check if a position is walkable.
     */
    private boolean isWalkable(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        // Must have solid ground below and air/passable blocks above
        boolean solidBelow = !below.isAir() && below.isSolid();
        boolean passableHere = state.isAir() || state.canBeReplaced();
        boolean passableAbove = above.isAir() || above.canBeReplaced();

        return solidBelow && passableHere && passableAbove;
    }

    /**
     * Get the cost of moving from one position to another.
     */
    private double getCost(BlockPos from, BlockPos to) {
        double baseCost = 1.0;

        // Penalize vertical movement
        if (from.getY() != to.getY()) {
            baseCost += 0.5;
        }

        // Penalize dangerous blocks (lava, fire, etc.)
        BlockState state = level.getBlockState(to);
        if (state.is(net.minecraft.tags.BlockTags.FIRE)) {
            baseCost += 10.0;
        }

        return baseCost;
    }

    /**
     * Reconstruct the path from the goal node back to the start.
     */
    private List<BlockPos> reconstructPath(PathNode goal) {
        List<BlockPos> path = new ArrayList<>();
        PathNode current = goal;

        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * Clear the path cache.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Simple path cache to avoid recalculating the same paths.
     */
    private static class PathCache {
        private final Map<PathKey, List<BlockPos>> cache;
        private final int maxSize;

        public PathCache(int maxSize) {
            this.maxSize = maxSize;
            this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<PathKey, List<BlockPos>> eldest) {
                    return size() > maxSize;
                }
            };
        }

        public List<BlockPos> getPath(BlockPos start, BlockPos goal) {
            return cache.get(new PathKey(start, goal));
        }

        public void cachePath(BlockPos start, BlockPos goal, List<BlockPos> path) {
            cache.put(new PathKey(start, goal), new ArrayList<>(path));
        }

        public void clear() {
            cache.clear();
        }

        private record PathKey(BlockPos start, BlockPos goal) {
            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof PathKey other)) return false;
                return start.equals(other.start) && goal.equals(other.goal);
            }

            @Override
            public int hashCode() {
                return Objects.hash(start, goal);
            }
        }
    }
}
