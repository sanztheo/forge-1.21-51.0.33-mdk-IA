package net.frealac.iamod.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.frealac.iamod.Config;
import net.frealac.iamod.common.story.VillagerStory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class OpenAiService {

    private static final String DEFAULT_MODEL = "gpt-4.1-nano"; // favor small fast model by default
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
        root.addProperty("max_tokens", 3000);

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
        root.addProperty("max_tokens", 3000);

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
     * True streaming via SSE (Server-Sent Events). Calls handlers onStart -> onDelta(chunk) -> onDone.
     * Returns a future completed with the full aggregated reply.
     */
    public CompletableFuture<String> chatStreamSSE(List<ChatMessage> history, Runnable onStart, Consumer<String> onDelta, Runnable onDone) {
        CompletableFuture<String> result = new CompletableFuture<>();
        new Thread(() -> {
            StringBuilder full = new StringBuilder();
            try {
                final String apiKey = getApiKey();
                if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OPENAI_API_KEY manquant");
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
                root.addProperty("max_tokens", 3000);
                root.addProperty("stream", true);

                HttpURLConnection conn = (HttpURLConnection) CHAT_URI.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }
                int code = conn.getResponseCode();
                if (code / 100 != 2) {
                    InputStreamReader er = new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8);
                    String err = new BufferedReader(er).lines().reduce((a,b)->a+b).orElse("");
                    throw new IOException("OpenAI HTTP " + code + ": " + trim(err, 200));
                }
                if (onStart != null) onStart.run();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.equals(":")) continue; // comment/keepalive
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                            JsonArray choices = obj.getAsJsonArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                                if (delta != null) {
                                    String content = delta.has("content") && !delta.get("content").isJsonNull() ? delta.get("content").getAsString() : null;
                                    if (content != null && !content.isEmpty()) {
                                        full.append(content);
                                        if (onDelta != null) onDelta.accept(content);
                                    }
                                }
                            }
                        } catch (Exception ignore) {}
                    }
                }
                if (onDone != null) onDone.run();
                result.complete(full.toString());
            } catch (Exception ex) {
                result.completeExceptionally(ex);
            }
        }, "iamod-openai-sse").start();
        return result;
    }

    /**
     * Enrich villager story with a concise long bio and optional openers.
     * Returns the enriched bioLong (and may be extended in future).
     */
    public String enrichStory(VillagerStory s) throws IOException, InterruptedException {
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
        system.addProperty("content", "Tu es un auteur-narrateur. Retourne UNIQUEMENT un objet JSON compact: {\"bioLong\": string}. Résume la vie de ce villageois (120-180 mots), immersif, à la 3e personne, cohérent avec les âges/repères. Évite dates relatives ambiguës.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        // Build minimal JSON payload from story
        JsonObject story = new JsonObject();
        story.addProperty("name", (s.nameGiven==null?"":s.nameGiven) + (s.nameFamily!=null?(" "+s.nameFamily):""));
        story.addProperty("age", s.ageYears);
        story.addProperty("sex", s.sex);
        story.addProperty("culture", s.cultureId);
        story.addProperty("profession", s.profession);
        story.addProperty("bioBrief", s.bioBrief);
        JsonArray traits = new JsonArray();
        if (s.traits != null) s.traits.forEach(traits::add);
        story.add("traits", traits);
        // timeline (age, type, place, details)
        JsonArray tl = new JsonArray();
        if (s.lifeTimeline != null) {
            s.lifeTimeline.stream().sorted((a,b)->Integer.compare(a.age,b.age)).limit(6).forEach(ev -> {
                JsonObject e = new JsonObject();
                e.addProperty("age", ev.age);
                if (ev.type!=null) e.addProperty("type", ev.type);
                if (ev.place!=null) e.addProperty("place", ev.place);
                if (ev.details!=null) e.addProperty("details", ev.details);
                tl.add(e);
            });
        }
        story.add("timeline", tl);
        user.addProperty("content", story.toString());
        messages.add(user);

        root.add("messages", messages);
        root.addProperty("temperature", 0.6);
        root.addProperty("max_tokens", 5000);

        HttpRequest request = HttpRequest.newBuilder(CHAT_URI)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI HTTP " + response.statusCode() + ": " + trim(response.body(), 300));
        }
        String content = extractContent(response.body());
        return extractBioLongFromContent(content);
    }

    /**
     * Ask the model to produce a strict JSON with refined fields (bioLong, timeline, psychology, health...)
     * We request JSON-only via response_format when supported.
     */
    public com.google.gson.JsonObject refineStoryJson(VillagerStory s) throws IOException, InterruptedException {
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
        system.addProperty("content",
                "Tu es un raffineur de données de PNJ. Retourne STRICTEMENT un JSON valide sans texte autour, conforme à: " +
                        "{ bioLong:string, timeline:[{age:int,type:string,place?:string,details?:string}], psychology:{moodBaseline?:number,stress?:number,resilience?:number,trauma:{events:[{ageAt:int,type:string,description?:string,severity?:number}],coping?:string[]}}, health?:{allergies?:string[], phobias?:[{type:string,severity:number}] } }. " +
                        "Respecte l'âge actuel, pas d'événements > âge. Utilise place lisible fourni. bioLong: 120-180 mots, 3e personne, cohérente."
        );
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");

        JsonObject story = new JsonObject();
        story.addProperty("name", (s.nameGiven==null?"":s.nameGiven) + (s.nameFamily!=null?(" "+s.nameFamily):""));
        story.addProperty("age", s.ageYears);
        story.addProperty("sex", s.sex);
        story.addProperty("culture", s.cultureId);
        story.addProperty("villageName", s.villageName);
        story.addProperty("profession", s.profession);
        story.addProperty("bioBrief", s.bioBrief);
        JsonArray traits = new JsonArray(); if (s.traits!=null) s.traits.forEach(traits::add); story.add("traits", traits);
        JsonArray tl = new JsonArray();
        if (s.lifeTimeline != null) {
            s.lifeTimeline.stream().sorted((a,b)->Integer.compare(a.age,b.age)).limit(8).forEach(ev -> {
                JsonObject e = new JsonObject(); e.addProperty("age", ev.age);
                if (ev.type!=null) e.addProperty("type", ev.type);
                if (ev.place!=null) e.addProperty("place", ev.place);
                if (ev.details!=null) e.addProperty("details", ev.details);
                tl.add(e);
            });
        }
        story.add("timeline", tl);
        user.addProperty("content", story.toString());
        messages.add(user);

        root.add("messages", messages);
        root.addProperty("temperature", 0.6);
        // Ask for JSON output format when supported by the model
        JsonObject respFormat = new JsonObject(); respFormat.addProperty("type", "json_object");
        root.add("response_format", respFormat);

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
        String content = extractContent(response.body());
        var obj = JsonUtils.tryParseStrictObject(content);
        if (obj == null) {
            // fallback: try to wrap content
            try { obj = com.google.gson.JsonParser.parseString(content).getAsJsonObject(); } catch (Exception ignore) {}
        }
        return obj;
    }

    private static String extractBioLongFromContent(String content) {
        if (content == null) return null;
        String c = content.trim();
        // Strip code fences
        if (c.startsWith("```")) {
            int i = c.indexOf('{');
            int j = c.lastIndexOf('}');
            if (i >= 0 && j > i) c = c.substring(i, j + 1);
        }
        // If raw JSON object
        try {
            JsonObject obj = JsonParser.parseString(c).getAsJsonObject();
            JsonElement bioL = obj.get("bioLong");
            if (bioL != null && !bioL.isJsonNull()) return trim(bioL.getAsString(), 2000);
        } catch (Exception ignore) {}
        // If content contains a field-like pattern "bioLong":"..."
        int k = c.indexOf("\"bioLong\"");
        if (k >= 0) {
            int colon = c.indexOf(':', k);
            int q1 = c.indexOf('"', colon + 1);
            int q2 = -1;
            if (q1 >= 0) {
                // scan until closing unescaped quote
                boolean esc = false; char ch;
                for (int i = q1 + 1; i < c.length(); i++) {
                    ch = c.charAt(i);
                    if (esc) { esc = false; continue; }
                    if (ch == '\\') { esc = true; continue; }
                    if (ch == '"') { q2 = i; break; }
                }
            }
            if (q1 >= 0 && q2 > q1) {
                String raw = c.substring(q1 + 1, q2);
                return trim(raw.replace("\\n", "\n").replace("\\\"", "\""), 2000);
            }
        }
        // Fallback: return content trimmed
        return trim(c, 2000);
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
            s.lifeTimeline.stream().sorted((a,b)->Integer.compare(a.age,b.age)).limit(4).forEach(ev -> {
                if (sb.length()>0) sb.append("; ");
                sb.append(ev.age).append(" ans: ").append(nz(ev.type));
                if (ev.place != null && !looksLikeCoordBucket(ev.place)) sb.append(" @").append(ev.place);
                if (ev.details != null && !ev.details.isEmpty()) sb.append(" – ").append(ev.details);
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

        // Tone guidance based on psychology/health
        String tone = "";
        if (s.psychology != null || s.health != null) {
            double mood = s.psychology != null ? s.psychology.moodBaseline : 0.0;
            double stress = s.psychology != null ? s.psychology.stress : 0.0;
            double resilience = s.psychology != null ? s.psychology.resilience : 0.5;
            double sleep = (s.health != null) ? s.health.sleepQuality : 0.6;
            boolean fatigued = sleep < 0.4;
            boolean tense = stress > 0.6;
            boolean upbeat = mood > 0.2 && !fatigued && stress < 0.5;
            StringBuilder t = new StringBuilder("Style: ");
            if (fatigued) t.append("fatigué, phrases plus courtes; ");
            if (tense) t.append("léger agacement, prudence; ");
            if (upbeat) t.append("chaleureux, positif; ");
            if (resilience > 0.7) t.append("résilient malgré les difficultés; ");
            tone = t.toString();
        }

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
        if (!tone.isEmpty()) sys.append(tone);
        sys.append("Parle à la première personne (\"je\"). Réponds en français, immersif et bref (1–3 phrases). ");
        sys.append("Si tu mentionnes un événement passé, privilégie 'quand j’avais [âge]' plutôt que 'il y a X ans'. ");
        sys.append("Ne contredis ni ta bio ni tes repères. Évite listes/énumérations. ");
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
