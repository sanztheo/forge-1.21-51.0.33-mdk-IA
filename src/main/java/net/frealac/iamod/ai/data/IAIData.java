package net.frealac.iamod.ai.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Interface for AI data capability.
 */
public interface IAIData {
    AIData getData();
    void setData(AIData data);
    CompoundTag saveNBT();
    void loadNBT(CompoundTag tag);
}
