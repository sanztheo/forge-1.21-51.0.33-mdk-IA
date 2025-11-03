package net.frealac.iamod.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public class AiReplyS2CPacket {
    private final int villagerId;
    private final String reply;

    public AiReplyS2CPacket(int villagerId, String reply) {
        this.villagerId = villagerId;
        this.reply = reply;
    }

    public int getVillagerId() { return villagerId; }
    public String getReply() { return reply; }

    public static void encode(AiReplyS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.villagerId);
        buf.writeUtf(msg.reply);
    }

    public static AiReplyS2CPacket decode(FriendlyByteBuf buf) {
        return new AiReplyS2CPacket(buf.readVarInt(), buf.readUtf());
    }
}
