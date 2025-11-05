package net.frealac.iamod.ai.chat;

import net.frealac.iamod.IAMOD;
import net.frealac.iamod.ai.behavior.BehaviorManager;
import net.frealac.iamod.ai.brain.AIAction;
import net.frealac.iamod.ai.openai.OpenAiBrainService;
import net.frealac.iamod.common.story.VillagerStory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Listens to player chat messages and routes them to nearby villagers' AI brains.
 * Each villager processes the message through their unique personality and decides how to respond.
 */
@Mod.EventBusSubscriber(modid = IAMOD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChatHandler {

    private static final double INTERACTION_RADIUS = 10.0; // Distance for villager to hear player
    private static final OpenAiBrainService brainService = new OpenAiBrainService();

    /**
     * Listen to player chat messages and route to nearby villagers.
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String message = event.getRawText();

        // Find nearby villagers within interaction radius
        AABB searchBox = player.getBoundingBox().inflate(INTERACTION_RADIUS);
        List<Villager> nearbyVillagers = player.level().getEntitiesOfClass(
                Villager.class,
                searchBox,
                villager -> villager.isAlive() && !villager.isBaby()
        );

        if (nearbyVillagers.isEmpty()) {
            return; // No villagers nearby to hear the message
        }

        // Each nearby villager processes the message through their unique brain
        for (Villager villager : nearbyVillagers) {
            processVillagerResponse(villager, player, message);
        }
    }

    /**
     * Process player message through villager's AI brain and execute actions.
     * Each villager responds based on their UNIQUE personality, mood, health, etc.
     */
    private static void processVillagerResponse(Villager villager, ServerPlayer player, String message) {
        try {
            // Get villager's behavior manager
            BehaviorManager behaviorManager = BehaviorManager.getOrCreate(villager);

            // Get villager's unique story (personality, psychology, health)
            VillagerStory story = getVillagerStory(villager);

            // Get current goals state for context
            String goalsState = behaviorManager.getCurrentGoalsState();

            // Ask the AI brain to analyze the message with FULL personality context
            // The brain will decide actions based on this villager's unique state
            List<AIAction> actions = brainService.analyzeIntention(message, story, goalsState);

            IAMOD.LOGGER.info("Villager {} received message: '{}', brain decided {} actions",
                    getVillagerName(story), message, actions.size());

            // Execute each action decided by the brain
            for (AIAction action : actions) {
                executeAction(villager, player, action, behaviorManager);
            }

        } catch (Exception e) {
            IAMOD.LOGGER.error("Failed to process villager AI response", e);
        }
    }

    /**
     * Execute a single action decided by the AI brain.
     */
    private static void executeAction(Villager villager, ServerPlayer player,
                                     AIAction action, BehaviorManager behaviorManager) {

        if (action.actionType == null) {
            IAMOD.LOGGER.warn("AIAction with null actionType: {}", action);
            return;
        }

        switch (action.actionType) {
            case SPEAK:
                // Villager speaks in chat
                if (action.message != null && !action.message.isBlank()) {
                    sendVillagerMessage(villager, player, action.message);
                }
                break;

            case ENABLE_GOAL:
                // Enable a specific goal
                if (action.goalType != null) {
                    enableGoal(behaviorManager, action.goalType, player);
                    if (action.message != null && !action.message.isBlank()) {
                        sendVillagerMessage(villager, player, action.message);
                    }
                }
                break;

            case DISABLE_GOAL:
                // Disable a specific goal
                if (action.goalType != null) {
                    disableGoal(behaviorManager, action.goalType);
                    if (action.message != null && !action.message.isBlank()) {
                        sendVillagerMessage(villager, player, action.message);
                    }
                }
                break;

            case ENABLE_ALL_GOALS:
                // Enable all goals
                behaviorManager.enableAllGoals();
                if (action.message != null && !action.message.isBlank()) {
                    sendVillagerMessage(villager, player, action.message);
                }
                break;

            case DISABLE_ALL_GOALS:
                // Disable all goals
                behaviorManager.disableAllGoals();
                if (action.message != null && !action.message.isBlank()) {
                    sendVillagerMessage(villager, player, action.message);
                }
                break;

            case NOTHING:
                // Villager chooses to do nothing (maybe angry or ignoring)
                IAMOD.LOGGER.info("Villager chose to do nothing. Reasoning: {}", action.reasoning);
                break;

            default:
                IAMOD.LOGGER.warn("Unknown action type: {}", action.actionType);
        }
    }

    /**
     * Enable a specific goal type.
     */
    private static void enableGoal(BehaviorManager manager, AIAction.GoalType goalType, ServerPlayer player) {
        switch (goalType) {
            case FOLLOW_PLAYER:
                manager.enableGoal("follow_player", player);
                IAMOD.LOGGER.info("Enabled FOLLOW_PLAYER goal for {}", player.getName().getString());
                break;
            case COLLECT_RESOURCES:
                manager.enableGoal("collect_resources");
                IAMOD.LOGGER.info("Enabled COLLECT_RESOURCES goal");
                break;
            case PATROL:
                manager.enableGoal("patrol");
                IAMOD.LOGGER.info("Enabled PATROL goal");
                break;
            case ALL:
                manager.enableAllGoals();
                IAMOD.LOGGER.info("Enabled ALL goals");
                break;
            default:
                IAMOD.LOGGER.warn("Unknown goal type: {}", goalType);
        }
    }

    /**
     * Disable a specific goal type.
     */
    private static void disableGoal(BehaviorManager manager, AIAction.GoalType goalType) {
        switch (goalType) {
            case FOLLOW_PLAYER:
                manager.disableGoal("follow_player");
                IAMOD.LOGGER.info("Disabled FOLLOW_PLAYER goal");
                break;
            case COLLECT_RESOURCES:
                manager.disableGoal("collect_resources");
                IAMOD.LOGGER.info("Disabled COLLECT_RESOURCES goal");
                break;
            case PATROL:
                manager.disableGoal("patrol");
                IAMOD.LOGGER.info("Disabled PATROL goal");
                break;
            case ALL:
                manager.disableAllGoals();
                IAMOD.LOGGER.info("Disabled ALL goals");
                break;
            default:
                IAMOD.LOGGER.warn("Unknown goal type: {}", goalType);
        }
    }

    /**
     * Send a message from the villager to the player.
     * The message appears in chat as if the villager is speaking.
     */
    private static void sendVillagerMessage(Villager villager, ServerPlayer player, String message) {
        VillagerStory story = getVillagerStory(villager);
        String villagerName = getVillagerName(story);

        // Send message to player's chat with villager name prefix
        String formattedMessage = "§e[" + villagerName + "]§r " + message;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(formattedMessage));

        IAMOD.LOGGER.info("Villager {} spoke: {}", villagerName, message);
    }

    /**
     * Get the villager's unique story (personality, psychology, health).
     * Each villager has their own story that makes them a unique person.
     */
    private static VillagerStory getVillagerStory(Villager villager) {
        // TODO: Retrieve actual VillagerStory from entity capability or data attachment
        // For now, return a basic story
        VillagerStory story = new VillagerStory();
        story.nameGiven = "Villageois";
        story.ageYears = 25;
        story.profession = "habitant";

        // Default psychology (will be replaced by actual data)
        story.psychology = new VillagerStory.Psychology();
        story.psychology.moodBaseline = 0.1; // Neutral mood
        story.psychology.stress = 0.3; // Low stress
        story.psychology.resilience = 0.7; // Good resilience

        // Default health (will be replaced by actual data)
        story.health = new VillagerStory.Health();
        story.health.sleepQuality = 0.8; // Well rested
        story.health.wounds = new java.util.ArrayList<>();

        return story;
    }

    /**
     * Get villager's display name.
     */
    private static String getVillagerName(VillagerStory story) {
        if (story.nameGiven != null && !story.nameGiven.isEmpty()) {
            if (story.nameFamily != null && !story.nameFamily.isEmpty()) {
                return story.nameGiven + " " + story.nameFamily;
            }
            return story.nameGiven;
        }
        return "Villageois";
    }
}
