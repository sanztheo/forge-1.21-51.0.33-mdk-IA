package net.frealac.iamod.client.story;

import net.frealac.iamod.common.story.VillagerStory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientStoryCache {
    private static final Map<Integer, VillagerStory> CACHE = new ConcurrentHashMap<>();

    public static void put(int entityId, VillagerStory story) {
        CACHE.put(entityId, story);
    }

    public static VillagerStory get(int entityId) {
        return CACHE.get(entityId);
    }
}

