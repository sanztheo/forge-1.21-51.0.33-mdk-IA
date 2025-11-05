package net.frealac.iamod.network.packet;

import net.frealac.iamod.ai.debug.VillagerDebugInfo;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Synchronizes villager debug info from server to client for HUD display.
 * Sent every tick when a villager is nearby.
 */
public class SyncVillagerDebugS2CPacket {

    private final VillagerDebugInfo debugInfo;
    private final boolean hasVillager; // True if there's a villager to display

    public SyncVillagerDebugS2CPacket(VillagerDebugInfo debugInfo) {
        this.debugInfo = debugInfo;
        this.hasVillager = true;
    }

    public SyncVillagerDebugS2CPacket(boolean hasVillager) {
        this.debugInfo = new VillagerDebugInfo(); // Empty
        this.hasVillager = false;
    }

    public static SyncVillagerDebugS2CPacket decode(FriendlyByteBuf buf) {
        boolean hasVillager = buf.readBoolean();
        if (hasVillager) {
            VillagerDebugInfo info = VillagerDebugInfo.read(buf);
            return new SyncVillagerDebugS2CPacket(info);
        } else {
            return new SyncVillagerDebugS2CPacket(false);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasVillager);
        if (hasVillager) {
            debugInfo.write(buf);
        }
    }

    public VillagerDebugInfo getDebugInfo() {
        return debugInfo;
    }

    public boolean hasVillager() {
        return hasVillager;
    }
}
