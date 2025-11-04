package net.frealac.iamod.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AI Goal that makes the entity patrol between multiple points.
 */
public class PatrolGoal extends AIGoal {
    private final List<BlockPos> patrolPoints = new ArrayList<>();
    private int currentPointIndex = 0;
    private final Random random = new Random();
    private int idleTicks = 0;
    private static final int IDLE_DURATION = 100; // 5 seconds at 20 ticks/second
    private boolean isIdling = false;

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

        // Check if arrived at current point
        BlockPos currentPoint = patrolPoints.get(currentPointIndex);
        if (entity.blockPosition().distSqr(currentPoint) < 4) { // Within 2 blocks
            // Start idling
            isIdling = true;
            idleTicks = 0;
            entity.getNavigation().stop();
        } else {
            // Continue navigating if path is lost
            PathNavigation navigation = entity.getNavigation();
            if (navigation.isDone()) {
                navigateToCurrentPoint();
            }
        }
    }

    @Override
    public void stop() {
        entity.getNavigation().stop();
        isIdling = false;
        idleTicks = 0;
    }

    private void moveToNextPoint() {
        currentPointIndex = (currentPointIndex + 1) % patrolPoints.size();
        navigateToCurrentPoint();
    }

    private void navigateToCurrentPoint() {
        if (patrolPoints.isEmpty()) return;
        BlockPos targetPos = patrolPoints.get(currentPointIndex);
        entity.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.0);
    }

    @Override
    public String getDescription() {
        return "Patrol (" + patrolPoints.size() + " points)";
    }

    public List<BlockPos> getPatrolPoints() {
        return new ArrayList<>(patrolPoints);
    }
}
