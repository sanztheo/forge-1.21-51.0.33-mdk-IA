package net.frealac.iamod.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public class CloseDialogC2SPacket {
    private final int villagerId;

    public CloseDialogC2SPacket(int villagerId) {
        this.villagerId = villagerId;
    }

    public int getVillagerId() {
        return villagerId;
    }

    public static void encode(CloseDialogC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.villagerId);
    }

    public static CloseDialogC2SPacket decode(FriendlyByteBuf buf) {
        return new CloseDialogC2SPacket(buf.readInt());
    }
}
