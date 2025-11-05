package net.frealac.iamod.network.packet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Packet to update AI configuration from client to server.
 */
public class UpdateAIConfigC2SPacket {
    private final int entityId;
    private final String goalName;
    private final boolean enabled;

    public UpdateAIConfigC2SPacket(int entityId, String goalName, boolean enabled) {
        this.entityId = entityId;
        this.goalName = goalName;
        this.enabled = enabled;
    }

    public UpdateAIConfigC2SPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.goalName = buf.readUtf();
        this.enabled = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(goalName);
        buf.writeBoolean(enabled);
    }

    public int getEntityId() {
        return entityId;
    }

    public String getGoalName() {
        return goalName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
