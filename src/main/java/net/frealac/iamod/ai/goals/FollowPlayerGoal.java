package net.frealac.iamod.ai.goals;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * AI Goal that makes the entity follow the nearest player.
 */
public class FollowPlayerGoal extends AIGoal {
    private Player targetPlayer;
    private final double followDistance;
    private final double stopDistance;
    private final double speedModifier;

    public FollowPlayerGoal(Mob entity, int priority, double followDistance, double stopDistance, double speedModifier) {
        super(entity, priority);
        this.followDistance = followDistance;
        this.stopDistance = stopDistance;
        this.speedModifier = speedModifier;
    }

    public FollowPlayerGoal(Mob entity, int priority) {
        this(entity, priority, 10.0, 2.0, 1.0);
    }

    @Override
    public boolean canUse() {
        if (!isActive) return false;

        targetPlayer = entity.level().getNearestPlayer(entity, followDistance);
        return targetPlayer != null && !targetPlayer.isSpectator();
    }

    @Override
    public boolean canContinueToUse() {
        if (!isActive) return false;
        if (targetPlayer == null) return false;
        if (targetPlayer.isSpectator()) return false;

        double distance = entity.distanceToSqr(targetPlayer);
        return distance <= followDistance * followDistance;
    }

    @Override
    public void start() {
        // Nothing special on start
    }

    @Override
    public void tick() {
        if (targetPlayer == null) return;

        double distance = entity.distanceTo(targetPlayer);

        // If too far, move closer
        if (distance > stopDistance) {
            entity.getNavigation().moveTo(targetPlayer, speedModifier);
        } else {
            // Stop if close enough
            entity.getNavigation().stop();
            // Look at player
            entity.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        targetPlayer = null;
        entity.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Follow Player (range: " + followDistance + ")";
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }
}
