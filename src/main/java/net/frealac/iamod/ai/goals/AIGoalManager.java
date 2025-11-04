package net.frealac.iamod.ai.goals;

import net.minecraft.world.entity.Mob;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all AI goals for an entity.
 * Handles goal execution, priority, and state management.
 */
public class AIGoalManager {
    private final Mob entity;
    private final List<AIGoal> goals = new ArrayList<>();
    private final Set<AIGoal> activeGoals = ConcurrentHashMap.newKeySet();
    private AIGoal currentPriorityGoal = null;

    public AIGoalManager(Mob entity) {
        this.entity = entity;
    }

    /**
     * Add a goal to the manager.
     */
    public void addGoal(AIGoal goal) {
        goals.add(goal);
        // Sort by priority (lower number = higher priority)
        goals.sort(Comparator.comparingInt(AIGoal::getPriority));
    }

    /**
     * Remove a goal from the manager.
     */
    public void removeGoal(AIGoal goal) {
        goals.remove(goal);
        activeGoals.remove(goal);
        if (currentPriorityGoal == goal) {
            currentPriorityGoal = null;
        }
    }

    /**
     * Clear all goals.
     */
    public void clearGoals() {
        activeGoals.forEach(AIGoal::stop);
        activeGoals.clear();
        goals.clear();
        currentPriorityGoal = null;
    }

    /**
     * Tick all active goals. This should be called every game tick.
     */
    public void tick() {
        // Find the highest priority goal that can be used
        AIGoal highestPriorityGoal = null;
        for (AIGoal goal : goals) {
            if (goal.canUse()) {
                highestPriorityGoal = goal;
                break; // Already sorted by priority
            }
        }

        // Stop current goal if a higher priority goal is available
        if (currentPriorityGoal != null && highestPriorityGoal != currentPriorityGoal) {
            if (currentPriorityGoal.isActive()) {
                currentPriorityGoal.stop();
                currentPriorityGoal.setActive(false);
                activeGoals.remove(currentPriorityGoal);
            }
        }

        // Start new goal if needed
        if (highestPriorityGoal != null && !highestPriorityGoal.isActive()) {
            highestPriorityGoal.start();
            highestPriorityGoal.setActive(true);
            activeGoals.add(highestPriorityGoal);
            currentPriorityGoal = highestPriorityGoal;
        }

        // Tick active goal
        if (currentPriorityGoal != null && currentPriorityGoal.isActive()) {
            if (currentPriorityGoal.canContinueToUse()) {
                currentPriorityGoal.tick();
            } else {
                currentPriorityGoal.stop();
                currentPriorityGoal.setActive(false);
                activeGoals.remove(currentPriorityGoal);
                currentPriorityGoal = null;
            }
        }
    }

    /**
     * Get all registered goals.
     */
    public List<AIGoal> getGoals() {
        return Collections.unmodifiableList(goals);
    }

    /**
     * Get all currently active goals.
     */
    public Set<AIGoal> getActiveGoals() {
        return Collections.unmodifiableSet(activeGoals);
    }

    /**
     * Get the current priority goal.
     */
    public AIGoal getCurrentPriorityGoal() {
        return currentPriorityGoal;
    }

    /**
     * Enable or disable a specific goal by class.
     */
    public void setGoalEnabled(Class<? extends AIGoal> goalClass, boolean enabled) {
        for (AIGoal goal : goals) {
            if (goal.getClass().equals(goalClass)) {
                if (!enabled && goal.isActive()) {
                    goal.stop();
                    goal.setActive(false);
                    activeGoals.remove(goal);
                    if (currentPriorityGoal == goal) {
                        currentPriorityGoal = null;
                    }
                }
                // Mark for future use
                goal.setActive(enabled);
            }
        }
    }
}
