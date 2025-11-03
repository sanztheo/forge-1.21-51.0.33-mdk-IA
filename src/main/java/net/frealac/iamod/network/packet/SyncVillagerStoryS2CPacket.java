package net.frealac.iamod.network.packet;

import net.frealac.iamod.common.story.VillagerStory;
import net.minecraft.network.FriendlyByteBuf;

public class SyncVillagerStoryS2CPacket {
    private final int villagerId;
    private final String storyJson;

    public SyncVillagerStoryS2CPacket(int villagerId, String storyJson) {
        this.villagerId = villagerId;
        this.storyJson = storyJson;
    }

    public int getVillagerId() { return villagerId; }
    public VillagerStory getStory() { return VillagerStory.fromJson(storyJson); }

    public static void encode(SyncVillagerStoryS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.villagerId);
        buf.writeUtf(msg.storyJson);
    }

    public static SyncVillagerStoryS2CPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String json = buf.readUtf();
        return new SyncVillagerStoryS2CPacket(id, json);
    }
}

