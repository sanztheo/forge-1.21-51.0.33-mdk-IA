package net.frealac.iamod.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.frealac.iamod.common.story.VillagerStory;

import java.util.HashSet;

public class StoryRefiner {
    public static void apply(VillagerStory s, JsonObject obj) {
        if (s == null || obj == null) return;
        // bioLong
        JsonElement bioL = obj.get("bioLong");
        if (bioL != null && !bioL.isJsonNull()) s.bioLong = trim(bioL.getAsString(), 2000);

        // timeline
        if (obj.has("timeline") && obj.get("timeline").isJsonArray()) {
            JsonArray tl = obj.getAsJsonArray("timeline");
            HashSet<String> dedup = new HashSet<>();
            for (JsonElement e : tl) {
                if (!e.isJsonObject()) continue;
                JsonObject ev = e.getAsJsonObject();
                int age = getInt(ev, "age", -1);
                if (age < 4 || age > Math.max(4, s.ageYears)) continue;
                String type = getStr(ev, "type", "");
                String key = age + "#" + type;
                if (!dedup.add(key)) continue;
                VillagerStory.LifeEvent le = new VillagerStory.LifeEvent();
                le.age = age;
                le.type = type;
                le.place = getStr(ev, "place", s.villageName);
                le.details = getStr(ev, "details", null);
                s.lifeTimeline.add(le);
            }
        }

        // psychology
        if (obj.has("psychology") && obj.get("psychology").isJsonObject()) {
            if (s.psychology == null) s.psychology = new VillagerStory.Psychology();
            JsonObject pj = obj.getAsJsonObject("psychology");
            if (pj.has("moodBaseline")) s.psychology.moodBaseline = clamp(pj.get("moodBaseline").getAsDouble(), -1, 1);
            if (pj.has("stress")) s.psychology.stress = clamp(pj.get("stress").getAsDouble(), 0, 1);
            if (pj.has("resilience")) s.psychology.resilience = clamp(pj.get("resilience").getAsDouble(), 0, 1);
            if (pj.has("trauma") && pj.get("trauma").isJsonObject()) {
                if (s.psychology.trauma == null) s.psychology.trauma = new VillagerStory.Trauma();
                JsonObject tj = pj.getAsJsonObject("trauma");
                if (tj.has("events") && tj.get("events").isJsonArray()) {
                    for (JsonElement e : tj.getAsJsonArray("events")) {
                        if (!e.isJsonObject()) continue;
                        JsonObject te = e.getAsJsonObject();
                        VillagerStory.TraumaEvent t = new VillagerStory.TraumaEvent();
                        t.ageAt = getInt(te, "ageAt", 0);
                        if (t.ageAt < 4 || t.ageAt > Math.max(4, s.ageYears)) continue;
                        t.type = getStr(te, "type", "");
                        t.description = getStr(te, "description", null);
                        t.severity = clamp(getDouble(te, "severity", 0.5), 0, 1);
                        s.psychology.trauma.events.add(t);
                    }
                }
                if (tj.has("coping") && tj.get("coping").isJsonArray()) {
                    for (JsonElement ce : tj.getAsJsonArray("coping")) s.psychology.trauma.coping.add(ce.getAsString());
                }
            }
        }

        // health (optional)
        if (obj.has("health") && obj.get("health").isJsonObject()) {
            if (s.health == null) s.health = new VillagerStory.Health();
            JsonObject hj = obj.getAsJsonObject("health");
            if (hj.has("allergies") && hj.get("allergies").isJsonArray()) {
                for (JsonElement ae : hj.getAsJsonArray("allergies")) s.health.allergies.add(ae.getAsString());
            }
            if (hj.has("phobias") && hj.get("phobias").isJsonArray()) {
                for (JsonElement pe : hj.getAsJsonArray("phobias")) {
                    if (!pe.isJsonObject()) continue;
                    JsonObject po = pe.getAsJsonObject();
                    VillagerStory.ScaleItem p = new VillagerStory.ScaleItem();
                    p.type = getStr(po, "type", "");
                    p.severity = clamp(getDouble(po, "severity", 0.5), 0, 1);
                    s.health.phobias.add(p);
                }
            }
        }
    }

    private static String getStr(JsonObject o, String k, String def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def; }
    private static int getInt(JsonObject o, String k, int def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : def; }
    private static double getDouble(JsonObject o, String k, double def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : def; }
    private static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }
    private static String trim(String s, int max) { if (s == null) return null; return s.length()<=max?s:s.substring(0, max); }
}

