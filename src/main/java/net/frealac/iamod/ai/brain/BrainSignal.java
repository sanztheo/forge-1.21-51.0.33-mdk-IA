package net.frealac.iamod.ai.brain;

import java.util.HashMap;
import java.util.Map;

/**
 * Signal sent between brain modules.
 * Like neurotransmitters in the brain, these signals carry information
 * between different brain regions.
 */
public class BrainSignal {

    public enum SignalType {
        // Emotional signals
        EMOTION_CHANGE,      // Mood or emotion changed
        STRESS_INCREASE,     // Stress level increased
        STRESS_DECREASE,     // Stress level decreased
        POSITIVE_FEELING,    // Something positive happened
        NEGATIVE_FEELING,    // Something negative happened

        // Social signals
        PLAYER_INTERACTION,  // Player interacted with villager
        RELATIONSHIP_UPDATE, // Relationship with player changed
        CONVERSATION_START,  // Conversation started
        CONVERSATION_END,    // Conversation ended

        // Memory signals
        MEMORY_RECALLED,     // A memory was recalled
        MEMORY_STORED,       // A new memory was stored
        IMPORTANT_EVENT,     // Something important to remember

        // Decision signals
        DECISION_REQUEST,    // Need to make a decision
        DECISION_MADE,       // A decision was made
        ACTION_PLANNED,      // An action was planned
        ACTION_EXECUTED,     // An action was executed

        // Physical signals
        PHYSICAL_PAIN,       // Physical pain (hit, hurt)
        PHYSICAL_PLEASURE,   // Physical pleasure (healed, gift)
        FATIGUE,            // Tired, need rest
        HUNGER,             // Hungry, need food

        // Cognitive signals
        CONFUSION,          // Don't understand something
        UNDERSTANDING,      // Understood something
        CURIOSITY,          // Want to learn more
        FOCUS_CHANGE        // Changed focus/attention
    }

    private final SignalType type;
    private final String sourceModule;
    private final Map<String, Object> data;
    private final long timestamp;

    public BrainSignal(SignalType type, String sourceModule) {
        this.type = type;
        this.sourceModule = sourceModule;
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Add data to the signal.
     */
    public BrainSignal withData(String key, Object value) {
        data.put(key, value);
        return this;
    }

    /**
     * Get signal type.
     */
    public SignalType getType() {
        return type;
    }

    /**
     * Get source module name.
     */
    public String getSourceModule() {
        return sourceModule;
    }

    /**
     * Get data from signal.
     */
    public Object getData(String key) {
        return data.get(key);
    }

    /**
     * Get all data.
     */
    public Map<String, Object> getAllData() {
        return new HashMap<>(data);
    }

    /**
     * Get timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("BrainSignal[type=%s, source=%s, data=%s]",
            type, sourceModule, data);
    }
}
