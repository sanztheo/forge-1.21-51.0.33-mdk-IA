package net.frealac.iamod.common.story;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Phase 1: Minimal, deterministic story payload with brief bio and simple family/memories.
 */
public class VillagerStory {
    public static final String SCHEMA_VERSION = "1.0.0";
    public static final String GENERATOR_VERSION = "1.0.0";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public String schemaVersion = SCHEMA_VERSION;
    public String generatorVersion = GENERATOR_VERSION;

    public UUID uuid;                 // Villager entity UUID (server-authoritative)
    public String villageId;          // Phase1: optional/derived bucket id
    public long worldSeed;

    // Identity / culture
    public String cultureId;
    public String nameGiven;
    public String nameFamily;
    public String sex;                // "male" | "female" | "other"
    public int ageYears;

    // Personality & profession (simplified Phase 1)
    public List<String> traits = new ArrayList<>();
    public String profession;

    // Family (logical, not necessarily mapped to real entities for Phase 1)
    public List<String> parents = new ArrayList<>();
    public List<String> children = new ArrayList<>();
    public List<String> siblings = new ArrayList<>();

    // Memories (brief)
    public List<String> memories = new ArrayList<>();

    // Generated text
    public String bioBrief;
    public String bioLong; // Phase 2+: enriched by LLM (optional)

    // Phase 2: Extended fields (stored as JSON strings for simplicity in NBT)
    public Health health;              // detailed health
    public Psychology psychology;      // trauma, mood, coping
    public List<LifeEvent> lifeTimeline = new ArrayList<>();
    public List<MemoryEntry> memoriesDetailed = new ArrayList<>();
    public Routines routines;
    public Preferences preferences;

    public VillagerStory() {}

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("schemaVersion", Objects.toString(schemaVersion, SCHEMA_VERSION));
        tag.putString("generatorVersion", Objects.toString(generatorVersion, GENERATOR_VERSION));
        if (uuid != null) tag.putUUID("uuid", uuid);
        if (villageId != null) tag.putString("villageId", villageId);
        tag.putLong("worldSeed", worldSeed);
        if (cultureId != null) tag.putString("cultureId", cultureId);
        if (nameGiven != null) tag.putString("nameGiven", nameGiven);
        if (nameFamily != null) tag.putString("nameFamily", nameFamily);
        if (sex != null) tag.putString("sex", sex);
        tag.putInt("ageYears", ageYears);
        tag.putString("profession", Objects.toString(profession, ""));
        tag.putString("bioBrief", Objects.toString(bioBrief, ""));
        if (bioLong != null && !bioLong.isEmpty()) tag.putString("bioLong", bioLong);

        // Lists as JSON strings for simplicity in Phase 1
        tag.putString("traits", GSON.toJson(traits));
        tag.putString("parents", GSON.toJson(parents));
        tag.putString("children", GSON.toJson(children));
        tag.putString("siblings", GSON.toJson(siblings));
        tag.putString("memories", GSON.toJson(memories));

        // Phase 2: extended fields as JSON
        if (health != null) tag.putString("health", GSON.toJson(health));
        if (psychology != null) tag.putString("psychology", GSON.toJson(psychology));
        if (!lifeTimeline.isEmpty()) tag.putString("lifeTimeline", GSON.toJson(lifeTimeline));
        if (!memoriesDetailed.isEmpty()) tag.putString("memoriesDetailed", GSON.toJson(memoriesDetailed));
        if (routines != null) tag.putString("routines", GSON.toJson(routines));
        if (preferences != null) tag.putString("preferences", GSON.toJson(preferences));
        return tag;
    }

    public static VillagerStory fromTag(CompoundTag tag) {
        VillagerStory s = new VillagerStory();
        s.schemaVersion = tag.getString("schemaVersion");
        s.generatorVersion = tag.getString("generatorVersion");
        if (tag.hasUUID("uuid")) s.uuid = tag.getUUID("uuid");
        if (tag.contains("villageId")) s.villageId = tag.getString("villageId");
        s.worldSeed = tag.getLong("worldSeed");
        s.cultureId = tag.getString("cultureId");
        s.nameGiven = tag.getString("nameGiven");
        s.nameFamily = tag.getString("nameFamily");
        s.sex = tag.getString("sex");
        s.ageYears = tag.getInt("ageYears");
        s.profession = tag.getString("profession");
        s.bioBrief = tag.getString("bioBrief");
        if (tag.contains("bioLong")) s.bioLong = tag.getString("bioLong");
        // Lists
        s.traits = GSON.fromJson(tag.getString("traits"), List.class);
        s.parents = GSON.fromJson(tag.getString("parents"), List.class);
        s.children = GSON.fromJson(tag.getString("children"), List.class);
        s.siblings = GSON.fromJson(tag.getString("siblings"), List.class);
        s.memories = GSON.fromJson(tag.getString("memories"), List.class);
        if (s.traits == null) s.traits = new ArrayList<>();
        if (s.parents == null) s.parents = new ArrayList<>();
        if (s.children == null) s.children = new ArrayList<>();
        if (s.siblings == null) s.siblings = new ArrayList<>();
        if (s.memories == null) s.memories = new ArrayList<>();

        // Phase 2
        if (tag.contains("health")) s.health = GSON.fromJson(tag.getString("health"), Health.class);
        if (tag.contains("psychology")) s.psychology = GSON.fromJson(tag.getString("psychology"), Psychology.class);
        if (tag.contains("lifeTimeline")) s.lifeTimeline = GSON.fromJson(tag.getString("lifeTimeline"), List.class);
        if (tag.contains("memoriesDetailed")) s.memoriesDetailed = GSON.fromJson(tag.getString("memoriesDetailed"), List.class);
        if (s.lifeTimeline == null) s.lifeTimeline = new ArrayList<>();
        if (s.memoriesDetailed == null) s.memoriesDetailed = new ArrayList<>();
        if (tag.contains("routines")) s.routines = GSON.fromJson(tag.getString("routines"), Routines.class);
        if (tag.contains("preferences")) s.preferences = GSON.fromJson(tag.getString("preferences"), Preferences.class);
        return s;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static VillagerStory fromJson(String json) {
        return GSON.fromJson(json, VillagerStory.class);
    }

    // ---- Phase 2 POJOs ----
    public static class Health {
        public List<String> disabilities = new ArrayList<>();
        public List<String> chronicDiseases = new ArrayList<>();
        public List<String> allergies = new ArrayList<>();
        public List<ScaleItem> addictions = new ArrayList<>();
        public List<ScaleItem> phobias = new ArrayList<>();
        public List<Wound> wounds = new ArrayList<>();
        public List<String> scars = new ArrayList<>();
        public double stamina = 0.5;
        public double painTolerance = 0.5;
        public double sleepQuality = 0.5;
    }

    public static class Psychology {
        public Trauma trauma = new Trauma();
        public double moodBaseline = 0.0; // -1..1
        public double stress = 0.0;
        public double resilience = 0.5;
        public List<String> fears = new ArrayList<>();
        public List<String> hopes = new ArrayList<>();
    }

    public static class Trauma {
        public List<TraumaEvent> events = new ArrayList<>();
        public boolean cptsd = false;
        public List<String> coping = new ArrayList<>();
    }

    public static class TraumaEvent {
        public String id;
        public String type;
        public int ageAt;
        public String description;
        public List<String> tags = new ArrayList<>();
        public double severity; // 0..1
        public List<String> recurringSymptoms = new ArrayList<>();
    }

    public static class ScaleItem {
        public String type;
        public double severity; // 0..1
    }

    public static class Wound {
        public String type;
        public String date;
        public double severity; // 0..1
        public boolean permanent;
    }

    public static class LifeEvent {
        public int age;
        public String type;
        public String place;
        public List<String> with = new ArrayList<>();
        public String details;
    }

    public static class MemoryEntry {
        public String id;
        public String date;
        public String topic;
        public List<String> persons = new ArrayList<>();
        public String place;
        public double moodDelta; // -1..1
        public double importance; // 0..1
        public List<String> tags = new ArrayList<>();
    }

    public static class Routines {
        public List<String> daily = new ArrayList<>();
        public Sleep sleepSchedule = new Sleep();
    }

    public static class Sleep { public String start = "21:30"; public String end = "06:00"; }

    public static class Preferences {
        public List<String> likes = new ArrayList<>();
        public List<String> dislikes = new ArrayList<>();
        public Foods foods = new Foods();
        public List<String> colors = new ArrayList<>();
        public List<String> hobbies = new ArrayList<>();
    }

    public static class Foods {
        public List<String> favorite = new ArrayList<>();
        public List<String> allergies = new ArrayList<>();
        public List<String> disliked = new ArrayList<>();
    }
}
