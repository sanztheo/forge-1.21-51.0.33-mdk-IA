package net.frealac.iamod.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.frealac.iamod.Config;
import net.frealac.iamod.common.story.VillagerStory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class OpenAiService {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final URI CHAT_URI = URI.create("https://api.openai.com/v1/chat/completions");

    private final HttpClient httpClient;

    public OpenAiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String ask(String userMessage, String who) throws IOException, InterruptedException {
        final String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY manquant: définissez-le dans run/config/iamod-common.toml ou comme variable d'environnement.");
        }

        final String model = (Config.openAiModel == null || Config.openAiModel.isBlank()) ? DEFAULT_MODEL : Config.openAiModel;

        JsonObject root = new JsonObject();
        root.addProperty("model", model);

        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "Tu es un assistant Minecraft amical et concis. Réponds en français si possible.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prefixWho(who) + userMessage);
        messages.add(user);

        root.add("messages", messages);
        root.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder(CHAT_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI HTTP " + response.statusCode() + ": " + trim(response.body(), 300));
        }

        return extractContent(response.body());
    }

    public String chat(List<ChatMessage> history) throws IOException, InterruptedException {
        final String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY manquant: définissez-le dans run/config/iamod-common.toml ou comme variable d'environnement.");
        }
        final String model = (Config.openAiModel == null || Config.openAiModel.isBlank()) ? DEFAULT_MODEL : Config.openAiModel;

        JsonObject root = new JsonObject();
        root.addProperty("model", model);

        JsonArray messages = new JsonArray();
        for (ChatMessage m : history) {
            JsonObject j = new JsonObject();
            j.addProperty("role", m.role);
            j.addProperty("content", m.content);
            messages.add(j);
        }
        root.add("messages", messages);
        root.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder(CHAT_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI HTTP " + response.statusCode() + ": " + trim(response.body(), 300));
        }
        return extractContent(response.body());
    }

    /**
     * Build a rich, compact system prompt from a VillagerStory. Keeps it concise while covering identity, traits,
     * family, memories, timeline, health and psychology highlights. The wording guides the LLM to speak in-character.
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
        String parents = s.parents != null && !s.parents.isEmpty() ? joinComma(s.parents, 3) : "";
        String children = s.children != null && !s.children.isEmpty() ? joinComma(s.children, 4) : "";
        String siblings = s.siblings != null && !s.siblings.isEmpty() ? joinComma(s.siblings, 3) : "";

        String memories = s.memoriesDetailed != null && !s.memoriesDetailed.isEmpty()
                ? s.memoriesDetailed.stream().limit(3)
                .map(m -> (m.topic != null ? m.topic : "souvenir") + (m.place != null ? (" @" + m.place) : ""))
                .reduce((a,b) -> a + "; " + b).orElse("")
                : (s.memories != null ? trim(joinSemi(s.memories, 3), 160) : "");

        String timeline = "";
        if (s.lifeTimeline != null && !s.lifeTimeline.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            s.lifeTimeline.stream().sorted((a,b)->Integer.compare(a.age,b.age)).limit(3).forEach(ev -> {
                if (sb.length()>0) sb.append("; ");
                sb.append(ev.age).append(" ans: ").append(nz(ev.type));
                if (ev.place != null && !looksLikeCoordBucket(ev.place)) sb.append(" @").append(ev.place);
            });
            timeline = sb.toString();
        }

        String health = "";
        if (s.health != null) {
            int wounds = s.health.wounds != null ? s.health.wounds.size() : 0;
            String allergies = (s.health.allergies != null && !s.health.allergies.isEmpty()) ? joinComma(s.health.allergies, 3) : "aucune";
            health = "santé: blessures=" + wounds + ", allergies=" + allergies;
        }

        String psych = "";
        if (s.psychology != null) {
            int tcount = (s.psychology.trauma != null && s.psychology.trauma.events != null) ? s.psychology.trauma.events.size() : 0;
            psych = String.format("psychologie: humeur=%.2f, stress=%.2f, résilience=%.2f, traumas=%d",
                    s.psychology.moodBaseline, s.psychology.stress, s.psychology.resilience, tcount);
        }

        String likes = (s.preferences != null && s.preferences.likes != null && !s.preferences.likes.isEmpty()) ? joinComma(s.preferences.likes, 4) : "";
        String dislikes = (s.preferences != null && s.preferences.dislikes != null && !s.preferences.dislikes.isEmpty()) ? joinComma(s.preferences.dislikes, 4) : "";

        StringBuilder sys = new StringBuilder();
        sys.append("Tu es ").append(name).append(profession).append(age).append(culture).append(". ");
        if (!traits.isEmpty()) sys.append("Traits: ").append(traits).append(". ");
        if (s.bioBrief != null && !s.bioBrief.isEmpty()) sys.append("Bio: ").append(trim(s.bioBrief, 180)).append(" ");
        if (!parents.isEmpty()) sys.append("Parents: ").append(parents).append(". ");
        if (!siblings.isEmpty()) sys.append("Fratrie: ").append(siblings).append(". ");
        if (!children.isEmpty()) sys.append("Enfants: ").append(children).append(". ");
        if (!memories.isEmpty()) sys.append("Souvenirs: ").append(memories).append(". ");
        if (!timeline.isEmpty()) sys.append("Repères: ").append(timeline).append(". ");
        if (!health.isEmpty()) sys.append(health).append(". ");
        if (!psych.isEmpty()) sys.append(psych).append(". ");
        if (!likes.isEmpty()) sys.append("Aime: ").append(likes).append(". ");
        if (!dislikes.isEmpty()) sys.append("N’aime pas: ").append(dislikes).append(". ");
        sys.append("Parle à la première personne (\"je\"). Réponds en français, de façon immersive et brève (1–3 phrases). Ne contredis ni ta bio ni ta timeline. Évite les listes et les énumérations.");
        return sys.toString();
    }

    private static boolean looksLikeCoordBucket(String s) {
        return s != null && s.matches("C-?\\d+xC-?\\d+");
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static String joinComma(java.util.List<String> list, int max) {
        return list.stream().limit(max).reduce((a,b)->a+", "+b).orElse("");
    }
    private static String joinSemi(java.util.List<String> list, int max) {
        return list.stream().limit(max).reduce((a,b)->a+"; "+b).orElse("");
    }

    private static String getApiKey() {
        // Priorité au fichier de config Forge (run/config/iamod-common.toml), sinon variable d'environnement
        String fromConfig = Config.openAiApiKey;
        if (fromConfig != null && !fromConfig.isBlank()) return fromConfig.trim();
        String fromEnv = System.getenv("OPENAI_API_KEY");
        return fromEnv != null ? fromEnv.trim() : null;
    }

    private static String extractContent(String json) throws IOException {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray choices = obj.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) throw new IOException("Réponse OpenAI sans 'choices'.");
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null) throw new IOException("Réponse OpenAI sans 'message'.");
            JsonElement content = message.get("content");
            if (content == null) throw new IOException("Réponse OpenAI sans 'content'.");
            String text = content.getAsString();
            return trim(text, 512);
        } catch (RuntimeException ex) {
            throw new IOException("Impossible d'analyser la réponse OpenAI.", ex);
        }
    }

    private static String prefixWho(String who) {
        if (who == null || who.isBlank()) return "";
        return "[" + who + "] ";
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max)) + "…";
    }
}
