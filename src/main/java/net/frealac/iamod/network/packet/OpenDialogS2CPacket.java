package net.frealac.iamod.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public class OpenDialogS2CPacket {
    private final int villagerId;
    private final String greeting;

    public OpenDialogS2CPacket(int villagerId, String greeting) {
        this.villagerId = villagerId;
        this.greeting = greeting;
    }

    public int getVillagerId() { return villagerId; }
    public String getGreeting() { return greeting; }

    public static void encode(OpenDialogS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.villagerId);
        buf.writeUtf(msg.greeting);
    }

    public static OpenDialogS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenDialogS2CPacket(buf.readVarInt(), buf.readUtf());
    }
}
