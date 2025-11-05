package net.frealac.iamod.ai.openai;

import com.google.gson.*;
import net.frealac.iamod.common.story.VillagerStory;

import java.io.IOException;

/**
 * Story enrichment service for VillagerStory.
 * Generates rich biographies and character details.
 */
public class OpenAiStoryService {

    private final OpenAiClient client;

    public OpenAiStoryService() {
        this.client = new OpenAiClient();
    }

    /**
     * Enrich villager story with a concise biography.
     * @param story Villager story to enrich
     * @return Enriched biography text
     */
    public String enrichStory(VillagerStory story) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();

        JsonArray messages = new JsonArray();

        // System prompt
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content",
            "Tu es un auteur-narrateur. Retourne UNIQUEMENT un objet JSON compact: {\"bioLong\": string}. " +
            "Résume la vie de ce villageois (120-180 mots), immersif, à la 3e personne, cohérent avec les âges/repères. " +
            "Évite dates relatives ambiguës.");
        messages.add(system);

        // User message with story data
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", buildStoryPayload(story));
        messages.add(user);

        payload.add("messages", messages);
        payload.addProperty("temperature", 0.6);
        payload.addProperty("max_tokens", 500);

        String responseBody = client.sendChatRequest(payload);
        return extractContent(responseBody);
    }

    /**
     * Build system prompt from villager story for character voice.
     */
    public static String buildSystemPromptFromStory(VillagerStory s) {
        if (s == null) {
            return "Tu es un villageois amical. Réponds en français, immersif, concis, à la première personne (\"je\").";
        }

        String name = nz(s.nameGiven) + (s.nameFamily != null ? (" " + s.nameFamily) : "");
        String age = s.ageYears > 0 ? (", " + s.ageYears + " ans") : "";
        String culture = s.cultureId != null ? (" (culture: " + s.cultureId + ")") : "";
        String profession = s.profession != null ? (" un(e) " + s.profession) : " un(e) habitant(e)";

        String traits = s.traits != null && !s.traits.isEmpty() ? joinComma(s.traits, 8) : "";

        StringBuilder sys = new StringBuilder();
        sys.append("Tu es ").append(name).append(profession).append(age).append(culture).append(". ");
        if (!traits.isEmpty()) sys.append("Traits: ").append(traits).append(". ");
        if (s.bioBrief != null && !s.bioBrief.isEmpty()) {
            sys.append("Bio: ").append(trim(s.bioBrief, 180)).append(" ");
        }

        sys.append("Parle à la première personne (\"je\"). Réponds en français, immersif et bref (1–3 phrases). ");
        sys.append("Ne contredis ni ta bio ni tes repères. Évite listes/énumérations. ");

        return sys.toString();
    }

    /**
     * Build story payload as JSON string.
     */
    private String buildStoryPayload(VillagerStory s) {
        JsonObject story = new JsonObject();
        story.addProperty("name", (s.nameGiven == null ? "" : s.nameGiven) + (s.nameFamily != null ? (" " + s.nameFamily) : ""));
        story.addProperty("age", s.ageYears);
        story.addProperty("sex", s.sex);
        story.addProperty("culture", s.cultureId);
        story.addProperty("profession", s.profession);
        story.addProperty("bioBrief", s.bioBrief);

        JsonArray traits = new JsonArray();
        if (s.traits != null) s.traits.forEach(traits::add);
        story.add("traits", traits);

        // Timeline
        JsonArray timeline = new JsonArray();
        if (s.lifeTimeline != null) {
            s.lifeTimeline.stream().sorted((a, b) -> Integer.compare(a.age, b.age)).limit(6).forEach(ev -> {
                JsonObject e = new JsonObject();
                e.addProperty("age", ev.age);
                if (ev.type != null) e.addProperty("type", ev.type);
                if (ev.place != null) e.addProperty("place", ev.place);
                if (ev.details != null) e.addProperty("details", ev.details);
                timeline.add(e);
            });
        }
        story.add("timeline", timeline);

        return story.toString();
    }

    /**
     * Extract content from OpenAI response.
     */
    private String extractContent(String json) throws IOException {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray choices = obj.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IOException("OpenAI response without 'choices'");
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null) {
                throw new IOException("OpenAI response without 'message'");
            }
            JsonElement content = message.get("content");
            if (content == null) {
                throw new IOException("OpenAI response without 'content'");
            }
            return content.getAsString().trim();
        } catch (RuntimeException ex) {
            throw new IOException("Cannot parse OpenAI response", ex);
        }
    }

    // Helper methods
    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String joinComma(java.util.List<String> list, int max) {
        return list.stream().limit(max).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max)) + "…";
    }
}
