package net.frealac.iamod.ai.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/**
 * Stores persistent data for an AI entity.
 * This data is saved and loaded from NBT.
 */
public class AIData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // AI State
    private String currentGoal = "none";
    private int experience = 0;
    private Map<String, Integer> skills = new HashMap<>();

    // Memory / Points of Interest
    private List<BlockPos> knownLocations = new ArrayList<>();
    private Map<String, String> memory = new HashMap<>();

    // Configuration
    private Map<String, Boolean> enabledGoals = new HashMap<>();
    private Map<String, Object> configuration = new HashMap<>();

    public AIData() {
        // Default initialization
    }

    // Getters and Setters
    public String getCurrentGoal() {
        return currentGoal;
    }

    public void setCurrentGoal(String currentGoal) {
        this.currentGoal = currentGoal;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public void addExperience(int amount) {
        this.experience += amount;
    }

    public Map<String, Integer> getSkills() {
        return skills;
    }

    public void setSkill(String skill, int level) {
        skills.put(skill, level);
    }

    public int getSkill(String skill) {
        return skills.getOrDefault(skill, 0);
    }

    public List<BlockPos> getKnownLocations() {
        return new ArrayList<>(knownLocations);
    }

    public void addKnownLocation(BlockPos pos) {
        if (!knownLocations.contains(pos)) {
            knownLocations.add(pos);
        }
    }

    public void removeKnownLocation(BlockPos pos) {
        knownLocations.remove(pos);
    }

    public void clearKnownLocations() {
        knownLocations.clear();
    }

    public Map<String, String> getMemory() {
        return new HashMap<>(memory);
    }

    public void setMemory(String key, String value) {
        memory.put(key, value);
    }

    public String getMemory(String key) {
        return memory.get(key);
    }

    public void clearMemory() {
        memory.clear();
    }

    public boolean isGoalEnabled(String goalName) {
        return enabledGoals.getOrDefault(goalName, true);
    }

    public void setGoalEnabled(String goalName, boolean enabled) {
        enabledGoals.put(goalName, enabled);
    }

    public Map<String, Boolean> getEnabledGoals() {
        return new HashMap<>(enabledGoals);
    }

    public void setConfiguration(String key, Object value) {
        configuration.put(key, value);
    }

    public Object getConfiguration(String key) {
        return configuration.get(key);
    }

    // NBT Serialization
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        // Basic data
        tag.putString("currentGoal", currentGoal);
        tag.putInt("experience", experience);

        // Skills
        CompoundTag skillsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skills.entrySet()) {
            skillsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("skills", skillsTag);

        // Known locations
        ListTag locationsTag = new ListTag();
        for (BlockPos pos : knownLocations) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            locationsTag.add(posTag);
        }
        tag.put("knownLocations", locationsTag);

        // Memory
        CompoundTag memoryTag = new CompoundTag();
        for (Map.Entry<String, String> entry : memory.entrySet()) {
            memoryTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("memory", memoryTag);

        // Enabled goals
        CompoundTag enabledGoalsTag = new CompoundTag();
        for (Map.Entry<String, Boolean> entry : enabledGoals.entrySet()) {
            enabledGoalsTag.putBoolean(entry.getKey(), entry.getValue());
        }
        tag.put("enabledGoals", enabledGoalsTag);

        // Configuration (as JSON string for flexibility)
        tag.putString("configuration", GSON.toJson(configuration));

        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        // Basic data
        currentGoal = tag.getString("currentGoal");
        experience = tag.getInt("experience");

        // Skills
        skills.clear();
        if (tag.contains("skills", Tag.TAG_COMPOUND)) {
            CompoundTag skillsTag = tag.getCompound("skills");
            for (String key : skillsTag.getAllKeys()) {
                skills.put(key, skillsTag.getInt(key));
            }
        }

        // Known locations
        knownLocations.clear();
        if (tag.contains("knownLocations", Tag.TAG_LIST)) {
            ListTag locationsTag = tag.getList("knownLocations", Tag.TAG_COMPOUND);
            for (int i = 0; i < locationsTag.size(); i++) {
                CompoundTag posTag = locationsTag.getCompound(i);
                BlockPos pos = new BlockPos(
                    posTag.getInt("x"),
                    posTag.getInt("y"),
                    posTag.getInt("z")
                );
                knownLocations.add(pos);
            }
        }

        // Memory
        memory.clear();
        if (tag.contains("memory", Tag.TAG_COMPOUND)) {
            CompoundTag memoryTag = tag.getCompound("memory");
            for (String key : memoryTag.getAllKeys()) {
                memory.put(key, memoryTag.getString(key));
            }
        }

        // Enabled goals
        enabledGoals.clear();
        if (tag.contains("enabledGoals", Tag.TAG_COMPOUND)) {
            CompoundTag enabledGoalsTag = tag.getCompound("enabledGoals");
            for (String key : enabledGoalsTag.getAllKeys()) {
                enabledGoals.put(key, enabledGoalsTag.getBoolean(key));
            }
        }

        // Configuration
        configuration.clear();
        if (tag.contains("configuration", Tag.TAG_STRING)) {
            String configJson = tag.getString("configuration");
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> loaded = GSON.fromJson(configJson, Map.class);
                if (loaded != null) {
                    configuration.putAll(loaded);
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
    }

    public void copyFrom(AIData other) {
        this.currentGoal = other.currentGoal;
        this.experience = other.experience;
        this.skills = new HashMap<>(other.skills);
        this.knownLocations = new ArrayList<>(other.knownLocations);
        this.memory = new HashMap<>(other.memory);
        this.enabledGoals = new HashMap<>(other.enabledGoals);
        this.configuration = new HashMap<>(other.configuration);
    }
}
