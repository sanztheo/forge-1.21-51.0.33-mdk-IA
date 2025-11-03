package net.frealac.iamod.common.story;

import net.frealac.iamod.IAMOD;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VillagerStoryProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(IAMOD.MOD_ID, "story");
    public static final Capability<IVillagerStory> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    private final LazyOptional<IVillagerStory> optional;
    private final IVillagerStory backend = new Backend();

    public VillagerStoryProvider() {
        this.optional = LazyOptional.of(() -> backend);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        return CAPABILITY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        VillagerStory s = backend.getStory();
        if (s != null) tag = s.toTag();
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt != null && !nbt.isEmpty()) {
            backend.setStory(VillagerStory.fromTag(nbt));
        }
    }

    private static class Backend implements IVillagerStory {
        private VillagerStory story;
        @Override public VillagerStory getStory() { return story; }
        @Override public void setStory(VillagerStory story) { this.story = story; }
    }
}
