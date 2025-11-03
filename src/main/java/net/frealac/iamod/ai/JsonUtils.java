package net.frealac.iamod.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtils {
    public static JsonObject tryParseStrictObject(String content) {
        if (content == null) return null;
        String c = content.trim();
        // strip code fences ```json ... ```
        if (c.startsWith("```")) {
            int i = c.indexOf('{');
            int j = c.lastIndexOf('}');
            if (i >= 0 && j > i) c = c.substring(i, j + 1);
        }
        try {
            JsonElement el = JsonParser.parseString(c);
            if (el != null && el.isJsonObject()) return el.getAsJsonObject();
        } catch (Exception ignore) {}
        return null;
    }
}

