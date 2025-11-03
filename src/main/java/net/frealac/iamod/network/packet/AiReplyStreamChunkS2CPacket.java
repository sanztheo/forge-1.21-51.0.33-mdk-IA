package net.frealac.iamod.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public class AiReplyStreamChunkS2CPacket {
    private final int villagerId;
    private final String chunk;
    private final boolean start;
    private final boolean done;

    public AiReplyStreamChunkS2CPacket(int villagerId, String chunk, boolean start, boolean done) {
        this.villagerId = villagerId;
        this.chunk = chunk;
        this.start = start;
        this.done = done;
    }

    public int getVillagerId() { return villagerId; }
    public String getChunk() { return chunk; }
    public boolean isStart() { return start; }
    public boolean isDone() { return done; }

    public static void encode(AiReplyStreamChunkS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.villagerId);
        buf.writeUtf(msg.chunk);
        buf.writeBoolean(msg.start);
        buf.writeBoolean(msg.done);
    }

    public static AiReplyStreamChunkS2CPacket decode(FriendlyByteBuf buf) {
        return new AiReplyStreamChunkS2CPacket(buf.readVarInt(), buf.readUtf(), buf.readBoolean(), buf.readBoolean());
    }
}

