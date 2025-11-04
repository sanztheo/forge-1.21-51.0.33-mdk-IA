package net.frealac.iamod.ai.goals;

import net.minecraft.world.entity.Mob;

/**
 * Base abstract class for all AI goals.
 * Each AI behavior (patrol, follow, collect resources, etc.) should extend this class.
 */
public abstract class AIGoal {
    protected final Mob entity;
    protected final int priority;
    protected boolean isActive = false;

    public AIGoal(Mob entity, int priority) {
        this.entity = entity;
        this.priority = priority;
    }

    /**
     * Check if this goal can be executed.
     * @return true if the goal can start
     */
    public abstract boolean canUse();

    /**
     * Check if this goal should continue executing.
     * @return true if the goal should continue
     */
    public abstract boolean canContinueToUse();

    /**
     * Called when the goal starts.
     */
    public abstract void start();

    /**
     * Called every tick while the goal is active.
     */
    public abstract void tick();

    /**
     * Called when the goal stops.
     */
    public abstract void stop();

    /**
     * Get the priority of this goal (lower = higher priority).
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Check if this goal is currently active.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Set the active state of this goal.
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Get a description of this goal (for debugging/UI).
     */
    public abstract String getDescription();
}
