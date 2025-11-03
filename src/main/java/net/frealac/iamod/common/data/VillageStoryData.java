package net.frealac.iamod.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-level store for villager stories (server-authoritative). Stores per villager UUID.
 * Note: For Forge 51.0.33 (MC 1.21), SavedData.Factory expects (loader, constructor, dataFixType).
 */
public class VillageStoryData extends SavedData {
    public static final String NAME = "iamod_villager_stories";

    private final Map<UUID, CompoundTag> stories = new HashMap<>();

    public static VillageStoryData get(ServerLevel level) {
        SavedData.Factory<VillageStoryData> factory = new SavedData.Factory<>(VillageStoryData::new, VillageStoryData::load, null);
        return level.getDataStorage().computeIfAbsent(factory, NAME);
    }

    public VillageStoryData() {}

    public static VillageStoryData load(CompoundTag tag, HolderLookup.Provider provider) {
        VillageStoryData data = new VillageStoryData();
        ListTag list = tag.getList("stories", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (e.hasUUID("uuid")) {
                UUID id = e.getUUID("uuid");
                CompoundTag story = e.getCompound("story");
                data.stories.put(id, story.copy());
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, CompoundTag> e : stories.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("uuid", e.getKey());
            row.put("story", e.getValue());
            list.add(row);
        }
        tag.put("stories", list);
        return tag;
    }

    public CompoundTag get(UUID uuid) {
        return stories.get(uuid);
    }

    public void put(UUID uuid, CompoundTag story) {
        stories.put(uuid, story.copy());
        setDirty();
    }

    public boolean has(UUID uuid) {
        return stories.containsKey(uuid);
    }
}
