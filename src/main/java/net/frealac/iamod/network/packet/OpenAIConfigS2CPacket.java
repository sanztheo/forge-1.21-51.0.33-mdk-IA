package net.frealac.iamod.network.packet;

import net.frealac.iamod.client.screen.AIConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet to open AI configuration screen on client.
 */
public class OpenAIConfigS2CPacket {
    private final int entityId;

    public OpenAIConfigS2CPacket(int entityId) {
        this.entityId = entityId;
    }

    public OpenAIConfigS2CPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new AIConfigScreen(entityId));
        });
        ctx.get().setPacketHandled(true);
    }
}
