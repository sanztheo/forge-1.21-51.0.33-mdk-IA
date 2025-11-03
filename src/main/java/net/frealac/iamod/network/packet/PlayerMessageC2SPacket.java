package net.frealac.iamod.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public class PlayerMessageC2SPacket {
    private final int villagerId;
    private final String message;

    public PlayerMessageC2SPacket(int villagerId, String message) {
        this.villagerId = villagerId;
        this.message = message;
    }

    public int getVillagerId() { return villagerId; }
    public String getMessage() { return message; }

    public static void encode(PlayerMessageC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.villagerId);
        buf.writeUtf(msg.message);
    }

    public static PlayerMessageC2SPacket decode(FriendlyByteBuf buf) {
        return new PlayerMessageC2SPacket(buf.readVarInt(), buf.readUtf());
    }
}
