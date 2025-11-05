package net.frealac.iamod.ai.behavior;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.goals.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager that can use either simple Goals or advanced Behavior Trees.
 * Provides flexibility in AI complexity based on needs.
 */
public class BehaviorManager {
    private static final Map<UUID, BehaviorManager> MANAGER_CACHE = new HashMap<>();

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
                initializeGoalsMode();
                break;
            case BEHAVIOR_TREE:
                if (entity instanceof Villager villager) {
                    behaviorTree = new VillagerBehaviorTree(villager);
                } else {
                    // Fallback to goals for non-villagers
                    initializeGoalsMode();
                    mode = BehaviorMode.GOALS;
                }
                break;
        }
    }

    /**
     * Initialize the Goals system with default goals
     */
    private void initializeGoalsMode() {
        goalManager = new AIGoalManager(entity);

        // Register default goals
        BlockPos entityPos = entity.blockPosition();

        // 1. Collect Resources Goal (Priority 1 - highest)
        CollectResourcesGoal collectGoal = new CollectResourcesGoal(entity, 1, 16.0, 1.0);
        goalManager.addGoal(collectGoal);

        // 2. Follow Player Goal (Priority 2)
        FollowPlayerGoal followGoal = new FollowPlayerGoal(entity, 2, 16.0, 3.0, 1.0);
        goalManager.addGoal(followGoal);

        // 3. Patrol Goal (Priority 3 - lowest, default behavior)
        PatrolGoal patrolGoal = new PatrolGoal(entity, 3);
        // Add 3 patrol points around spawn location
        patrolGoal.addPatrolPoint(entityPos.offset(10, 0, 0));
        patrolGoal.addPatrolPoint(entityPos.offset(-10, 0, 10));
        patrolGoal.addPatrolPoint(entityPos.offset(0, 0, -10));
        goalManager.addGoal(patrolGoal);

        IAMOD.LOGGER.info("Initialized GOALS mode for entity {} with 3 goals", entity.getId());
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

    /**
     * Get or create a BehaviorManager for a villager entity.
     * Managers are cached per entity UUID.
     */
    public static BehaviorManager getOrCreate(Villager villager) {
        UUID uuid = villager.getUUID();
        return MANAGER_CACHE.computeIfAbsent(uuid, k -> new BehaviorManager(villager));
    }

    /**
     * Remove a BehaviorManager from cache (when entity is removed).
     */
    public static void remove(Villager villager) {
        MANAGER_CACHE.remove(villager.getUUID());
    }

    /**
     * Get current goals state as a formatted string for AI context.
     */
    public String getCurrentGoalsState() {
        if (mode != BehaviorMode.GOALS || goalManager == null) {
            return "No goals available (behavior tree mode)";
        }

        StringBuilder state = new StringBuilder();
        state.append("Goals registered: ").append(goalManager.getAllGoals().size()).append("\n");

        for (AIGoal goal : goalManager.getAllGoals()) {
            state.append("- ").append(goal.getDescription())
                 .append(" (Priority: ").append(goal.getPriority())
                 .append(", Active: ").append(goal.isActive() ? "YES" : "NO")
                 .append(")\n");
        }

        AIGoal current = goalManager.getCurrentGoal();
        if (current != null) {
            state.append("Currently executing: ").append(current.getDescription());
        } else {
            state.append("No goal currently executing");
        }

        return state.toString();
    }

    /**
     * Enable a specific goal by name.
     */
    public void enableGoal(String goalName) {
        if (goalManager == null) return;

        for (AIGoal goal : goalManager.getAllGoals()) {
            if (matchesGoalName(goal, goalName)) {
                goal.setActive(true);
                IAMOD.LOGGER.info("Enabled goal: {} for entity {}", goal.getDescription(), entity.getId());
                return;
            }
        }
        IAMOD.LOGGER.warn("Goal '{}' not found for entity {}", goalName, entity.getId());
    }

    /**
     * Enable a specific goal with a player target (for FollowPlayerGoal).
     */
    public void enableGoal(String goalName, ServerPlayer player) {
        if (goalManager == null) return;

        for (AIGoal goal : goalManager.getAllGoals()) {
            if (matchesGoalName(goal, goalName)) {
                // If it's a FollowPlayerGoal, set the target player
                if (goal instanceof FollowPlayerGoal followGoal) {
                    followGoal.setTargetPlayer(player);
                }
                goal.setActive(true);
                IAMOD.LOGGER.info("Enabled goal: {} for entity {} with player target {}",
                        goal.getDescription(), entity.getId(), player.getName().getString());
                return;
            }
        }
        IAMOD.LOGGER.warn("Goal '{}' not found for entity {}", goalName, entity.getId());
    }

    /**
     * Disable a specific goal by name.
     */
    public void disableGoal(String goalName) {
        if (goalManager == null) return;

        for (AIGoal goal : goalManager.getAllGoals()) {
            if (matchesGoalName(goal, goalName)) {
                goal.setActive(false);
                IAMOD.LOGGER.info("Disabled goal: {} for entity {}", goal.getDescription(), entity.getId());
                return;
            }
        }
        IAMOD.LOGGER.warn("Goal '{}' not found for entity {}", goalName, entity.getId());
    }

    /**
     * Enable all goals.
     */
    public void enableAllGoals() {
        if (goalManager == null) return;

        for (AIGoal goal : goalManager.getAllGoals()) {
            goal.setActive(true);
        }
        IAMOD.LOGGER.info("Enabled all goals for entity {}", entity.getId());
    }

    /**
     * Disable all goals.
     */
    public void disableAllGoals() {
        if (goalManager == null) return;

        for (AIGoal goal : goalManager.getAllGoals()) {
            goal.setActive(false);
        }
        IAMOD.LOGGER.info("Disabled all goals for entity {}", entity.getId());
    }

    /**
     * Check if a goal matches the given name.
     */
    private boolean matchesGoalName(AIGoal goal, String name) {
        String description = goal.getDescription().toLowerCase();
        String searchName = name.toLowerCase().replace("_", " ");

        return description.contains(searchName) ||
               description.equals(searchName) ||
               goal.getClass().getSimpleName().toLowerCase().contains(searchName);
    }

    /**
     * Create debug info for HUD display.
     * Collects all villager state information.
     */
    public net.frealac.iamod.ai.debug.VillagerDebugInfo createDebugInfo(
            net.frealac.iamod.common.story.VillagerStory story,
            net.minecraft.world.entity.player.Player player) {

        if (!(entity instanceof Villager villager)) {
            return new net.frealac.iamod.ai.debug.VillagerDebugInfo();
        }

        // Get villager name
        String villagerName = "Villageois";
        if (story != null && story.nameGiven != null && !story.nameGiven.isEmpty()) {
            villagerName = story.nameGiven;
            if (story.nameFamily != null && !story.nameFamily.isEmpty()) {
                villagerName += " " + story.nameFamily;
            }
        }

        // Get current action and goal
        String currentAction = "Inactif";
        String currentGoal = "Aucun";
        String targetPlayer = "Aucun";

        if (goalManager != null) {
            AIGoal activeGoal = goalManager.getCurrentGoal();
            if (activeGoal != null) {
                currentAction = activeGoal.getDescription();
                currentGoal = activeGoal.getClass().getSimpleName().replace("Goal", "");

                // Check if following a player
                if (activeGoal instanceof FollowPlayerGoal followGoal) {
                    net.minecraft.world.entity.player.Player target = followGoal.getTargetPlayer();
                    if (target != null) {
                        targetPlayer = target.getName().getString();
                    }
                }
            }
        }

        // Get psychology data
        double mood = 0.0;
        double stress = 0.0;
        double resilience = 0.0;
        double sleepQuality = 0.0;

        if (story != null && story.psychology != null) {
            mood = story.psychology.moodBaseline;
            stress = story.psychology.stress;
            resilience = story.psychology.resilience;
        }

        if (story != null && story.health != null) {
            sleepQuality = story.health.sleepQuality;
        }

        // Get memory data
        int memoryCount = 0;
        double sentiment = 0.0;
        java.util.List<String> recentMemories = new java.util.ArrayList<>();

        if (story != null && story.interactionMemory != null && player != null) {
            memoryCount = story.interactionMemory.getMemoryCount();
            sentiment = story.interactionMemory.getSentimentTowardsPlayer(player.getUUID());

            // Get last 5 memories
            java.util.List<net.frealac.iamod.ai.memory.Memory> memories =
                story.interactionMemory.getMemoriesWithPlayer(player.getUUID());

            for (int i = 0; i < Math.min(5, memories.size()); i++) {
                net.frealac.iamod.ai.memory.Memory mem = memories.get(i);
                recentMemories.add(mem.getTimeDescription() + ": " +
                    mem.getDescription().substring(0, Math.min(40, mem.getDescription().length())));
            }
        }

        // Calculate distance to player
        double distance = 0.0;
        if (player != null) {
            distance = villager.distanceTo(player);
        }

        return new net.frealac.iamod.ai.debug.VillagerDebugInfo(
            villagerName,
            currentAction,
            currentGoal,
            targetPlayer,
            mood,
            stress,
            resilience,
            sleepQuality,
            memoryCount,
            sentiment,
            recentMemories,
            distance
        );
    }
}
