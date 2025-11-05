package net.frealac.iamod.network.packet;

import net.frealac.iamod.client.screen.AIConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

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

    public int getEntityId() {
        return entityId;
    }
}
