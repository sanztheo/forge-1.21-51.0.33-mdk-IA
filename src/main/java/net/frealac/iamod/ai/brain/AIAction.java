package net.frealac.iamod.ai.brain;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an action that the AI brain can execute.
 * Used for JSON communication with OpenAI.
 */
public class AIAction {

    public enum ActionType {
        @SerializedName("speak")
        SPEAK,              // Respond in chat

        @SerializedName("enable_goal")
        ENABLE_GOAL,        // Enable a specific goal

        @SerializedName("disable_goal")
        DISABLE_GOAL,       // Disable a specific goal

        @SerializedName("enable_all_goals")
        ENABLE_ALL_GOALS,   // Enable all goals

        @SerializedName("disable_all_goals")
        DISABLE_ALL_GOALS,  // Disable all goals

        @SerializedName("nothing")
        NOTHING             // Do nothing (just acknowledge)
    }

    public enum GoalType {
        @SerializedName("follow_player")
        FOLLOW_PLAYER,

        @SerializedName("collect_resources")
        COLLECT_RESOURCES,

        @SerializedName("patrol")
        PATROL,

        @SerializedName("all")
        ALL
    }

    @SerializedName("action")
    public ActionType actionType;

    @SerializedName("goal")
    public GoalType goalType;

    @SerializedName("message")
    public String message;

    @SerializedName("reasoning")
    public String reasoning;  // Why the AI chose this action (for debugging)

    public AIAction() {}

    public AIAction(ActionType actionType, GoalType goalType, String message) {
        this.actionType = actionType;
        this.goalType = goalType;
        this.message = message;
    }

    /**
     * Create a "speak" action
     */
    public static AIAction speak(String message) {
        return new AIAction(ActionType.SPEAK, null, message);
    }

    /**
     * Create an "enable goal" action
     */
    public static AIAction enableGoal(GoalType goal, String message) {
        return new AIAction(ActionType.ENABLE_GOAL, goal, message);
    }

    /**
     * Create a "disable goal" action
     */
    public static AIAction disableGoal(GoalType goal, String message) {
        return new AIAction(ActionType.DISABLE_GOAL, goal, message);
    }

    /**
     * Create a "do nothing" action
     */
    public static AIAction nothing(String message) {
        return new AIAction(ActionType.NOTHING, null, message);
    }

    @Override
    public String toString() {
        return "AIAction{" +
                "action=" + actionType +
                ", goal=" + goalType +
                ", message='" + message + '\'' +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}
