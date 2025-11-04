package net.frealac.iamod.ai.behavior;

import net.frealac.iamod.ai.goals.AIGoalManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;

/**
 * Manager that can use either simple Goals or advanced Behavior Trees.
 * Provides flexibility in AI complexity based on needs.
 */
public class BehaviorManager {
    private final Mob entity;
    private AIGoalManager goalManager;
    private VillagerBehaviorTree behaviorTree;
    private BehaviorMode mode;

    public enum BehaviorMode {
        /** Simple goal-based behavior (lighter, faster) */
        GOALS,
        /** Advanced behavior tree (more complex, realistic) */
        BEHAVIOR_TREE
    }

    public BehaviorManager(Mob entity, BehaviorMode mode) {
        this.entity = entity;
        this.mode = mode;
        initialize();
    }

    public BehaviorManager(Mob entity) {
        this(entity, BehaviorMode.GOALS);
    }

    private void initialize() {
        switch (mode) {
            case GOALS:
                goalManager = new AIGoalManager(entity);
                break;
            case BEHAVIOR_TREE:
                if (entity instanceof Villager villager) {
                    behaviorTree = new VillagerBehaviorTree(villager);
                } else {
                    // Fallback to goals for non-villagers
                    goalManager = new AIGoalManager(entity);
                    mode = BehaviorMode.GOALS;
                }
                break;
        }
    }

    /**
     * Tick the behavior system. Call this every game tick.
     */
    public void tick() {
        switch (mode) {
            case GOALS:
                if (goalManager != null) {
                    goalManager.tick();
                }
                break;
            case BEHAVIOR_TREE:
                if (behaviorTree != null) {
                    behaviorTree.step();
                }
                break;
        }
    }

    /**
     * Switch between behavior modes.
     */
    public void setMode(BehaviorMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            // Cleanup old system
            goalManager = null;
            behaviorTree = null;
            // Initialize new system
            initialize();
        }
    }

    /**
     * Get the current behavior mode.
     */
    public BehaviorMode getMode() {
        return mode;
    }

    /**
     * Get the goal manager (only if in GOALS mode).
     */
    public AIGoalManager getGoalManager() {
        return goalManager;
    }

    /**
     * Get the behavior tree (only if in BEHAVIOR_TREE mode).
     */
    public VillagerBehaviorTree getBehaviorTree() {
        return behaviorTree;
    }

    /**
     * Reset the behavior system.
     */
    public void reset() {
        switch (mode) {
            case GOALS:
                if (goalManager != null) {
                    goalManager.clearGoals();
                }
                break;
            case BEHAVIOR_TREE:
                if (behaviorTree != null) {
                    behaviorTree.reset();
                }
                break;
        }
    }
}
