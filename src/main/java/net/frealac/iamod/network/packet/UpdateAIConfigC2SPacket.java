package net.frealac.iamod.network.packet;

import net.frealac.iamod.ai.data.AIDataProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.level().getEntity(entityId);
            if (!(entity instanceof Mob mob)) return;

            // Update AI data capability
            mob.getCapability(AIDataProvider.CAPABILITY).ifPresent(aiData -> {
                aiData.getData().setGoalEnabled(goalName, enabled);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
