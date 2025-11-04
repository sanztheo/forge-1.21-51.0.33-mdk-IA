package net.frealac.iamod.ai.goals;

import net.frealac.iamod.ai.pathfinding.PathfindingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AI Goal that makes the entity patrol between multiple points.
 * Enhanced with advanced pathfinding using PathfindingManager.
 */
public class PatrolGoal extends AIGoal {
    private final List<BlockPos> patrolPoints = new ArrayList<>();
    private int currentPointIndex = 0;
    private final Random random = new Random();
    private int idleTicks = 0;
    private static final int IDLE_DURATION = 100; // 5 seconds at 20 ticks/second
    private boolean isIdling = false;
    private boolean isCalculatingPath = false;
    private List<BlockPos> currentPath = new ArrayList<>();
    private int currentPathStep = 0;

    public PatrolGoal(Mob entity, int priority) {
        super(entity, priority);
    }

    public void addPatrolPoint(BlockPos pos) {
        patrolPoints.add(pos);
    }

    public void clearPatrolPoints() {
        patrolPoints.clear();
    }

    @Override
    public boolean canUse() {
        return !patrolPoints.isEmpty() && isActive;
    }

    @Override
    public boolean canContinueToUse() {
        return !patrolPoints.isEmpty() && isActive;
    }

    @Override
    public void start() {
        if (patrolPoints.isEmpty()) return;
        currentPointIndex = random.nextInt(patrolPoints.size());
        navigateToCurrentPoint();
    }

    @Override
    public void tick() {
        if (patrolPoints.isEmpty()) return;

        // If idling, count down
        if (isIdling) {
            idleTicks++;
            if (idleTicks >= IDLE_DURATION) {
                isIdling = false;
                idleTicks = 0;
                moveToNextPoint();
            }
            return;
        }

        // If calculating path, wait
        if (isCalculatingPath) {
            return;
        }

        // Check if arrived at current point
        BlockPos currentPoint = patrolPoints.get(currentPointIndex);
        if (entity.blockPosition().distSqr(currentPoint) < 4) { // Within 2 blocks
            // Start idling
            isIdling = true;
            idleTicks = 0;
            entity.getNavigation().stop();
            currentPath.clear();
        } else {
            // Follow calculated path if available
            if (!currentPath.isEmpty() && currentPathStep < currentPath.size()) {
                BlockPos nextStep = currentPath.get(currentPathStep);

                // Check if reached current step
                if (entity.blockPosition().distSqr(nextStep) < 1.5) {
                    currentPathStep++;
                    if (currentPathStep < currentPath.size()) {
                        BlockPos nextTarget = currentPath.get(currentPathStep);
                        entity.getNavigation().moveTo(
                            nextTarget.getX() + 0.5,
                            nextTarget.getY(),
                            nextTarget.getZ() + 0.5,
                            1.0
                        );
                    }
                }
            } else {
                // Recalculate path if navigation is done
                PathNavigation navigation = entity.getNavigation();
                if (navigation.isDone()) {
                    navigateToCurrentPoint();
                }
            }
        }
    }

    @Override
    public void stop() {
        entity.getNavigation().stop();
        isIdling = false;
        idleTicks = 0;
        currentPath.clear();
        isCalculatingPath = false;
    }

    private void moveToNextPoint() {
        currentPointIndex = (currentPointIndex + 1) % patrolPoints.size();
        navigateToCurrentPoint();
    }

    private void navigateToCurrentPoint() {
        if (patrolPoints.isEmpty() || isCalculatingPath) return;

        BlockPos targetPos = patrolPoints.get(currentPointIndex);
        BlockPos startPos = entity.blockPosition();

        // Use advanced pathfinding asynchronously
        isCalculatingPath = true;
        PathfindingManager.getInstance().findPathAsync(
            entity.level(),
            startPos,
            targetPos,
            path -> {
                isCalculatingPath = false;
                if (!path.isEmpty()) {
                    // Path found, use it
                    currentPath = new ArrayList<>(path);
                    currentPathStep = 0;

                    if (!currentPath.isEmpty()) {
                        BlockPos firstStep = currentPath.get(0);
                        entity.getNavigation().moveTo(
                            firstStep.getX() + 0.5,
                            firstStep.getY(),
                            firstStep.getZ() + 0.5,
                            1.0
                        );
                    }
                } else {
                    // No path found, fallback to vanilla navigation
                    entity.getNavigation().moveTo(
                        targetPos.getX() + 0.5,
                        targetPos.getY(),
                        targetPos.getZ() + 0.5,
                        1.0
                    );
                }
            }
        );
    }

    @Override
    public String getDescription() {
        return "Patrol (" + patrolPoints.size() + " points) [Advanced Pathfinding]";
    }

    public List<BlockPos> getPatrolPoints() {
        return new ArrayList<>(patrolPoints);
    }
}
