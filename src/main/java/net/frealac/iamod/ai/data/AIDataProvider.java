package net.frealac.iamod.ai.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider for AI data capability.
 */
public class AIDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<IAIData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private final IAIData implementation = new AIDataImpl();
    private final LazyOptional<IAIData> holder = LazyOptional.of(() -> implementation);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CAPABILITY) {
            return holder.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return implementation.saveNBT();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        implementation.loadNBT(nbt);
    }

    /**
     * Implementation of the AI data capability.
     */
    private static class AIDataImpl implements IAIData {
        private AIData data = new AIData();

        @Override
        public AIData getData() {
            return data;
        }

        @Override
        public void setData(AIData data) {
            this.data = data;
        }

        @Override
        public CompoundTag saveNBT() {
            return data.saveToNBT();
        }

        @Override
        public void loadNBT(CompoundTag tag) {
            data.loadFromNBT(tag);
        }
    }
}
