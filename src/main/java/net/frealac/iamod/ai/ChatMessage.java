package net.frealac.iamod.ai;

public class ChatMessage {
    public final String role;
    public final String content;
    public ChatMessage(String role, String content) {
        this.role = role; this.content = content;
    }
    public static ChatMessage system(String c)    { return new ChatMessage("system", c); }
    public static ChatMessage user(String c)      { return new ChatMessage("user", c); }
    public static ChatMessage assistant(String c) { return new ChatMessage("assistant", c); }
}

