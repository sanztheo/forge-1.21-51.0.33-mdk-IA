package net.frealac.iamod.server;

import net.frealac.iamod.Config;
import net.frealac.iamod.ai.ChatMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManager {
    public record Key(UUID player, int villagerId) {}
    private static final ConcurrentHashMap<Key, Conversation> SESSIONS = new ConcurrentHashMap<>();

    private static class Conversation {
        final List<ChatMessage> history = new ArrayList<>();
        Instant lastAccess = Instant.now();
        boolean hasSystem = false;
    }

    public static Key key(UUID player, int villagerId) {
        return new Key(player, villagerId);
    }

    private static Conversation fresh() {
        var c = new Conversation();
        return c;
    }

    private static Conversation getOrCreate(Key key) {
        int ttl = Math.max(60, Config.openAiSessionTtlSeconds);
        var now = Instant.now();
        return SESSIONS.compute(key, (k, existing) -> {
            if (existing == null) return fresh();
            if (now.getEpochSecond() - existing.lastAccess.getEpochSecond() > ttl) return fresh();
            return existing;
        });
    }

    public static List<ChatMessage> appendUserAndGetHistory(Key key, String userText) {
        var conv = getOrCreate(key);
        conv.history.add(ChatMessage.user(userText));
        conv.lastAccess = Instant.now();
        return List.copyOf(conv.history);
    }

    public static void appendAssistant(Key key, String assistantText) {
        var conv = getOrCreate(key);
        conv.history.add(ChatMessage.assistant(assistantText));
        conv.lastAccess = Instant.now();
    }

    public static void ensureSystem(Key key, String systemPrompt) {
        var conv = getOrCreate(key);
        if (!conv.hasSystem) {
            conv.history.add(ChatMessage.system(systemPrompt));
            conv.hasSystem = true;
            conv.lastAccess = Instant.now();
        }
    }
}
