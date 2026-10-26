package wayoftime.bloodmagic.common.registries;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.core.data.Binding;

public final class BloodMagicDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BloodMagic.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BLOOD_ORB_TIER =
            DATA_COMPONENTS.register("blood_orb_tier", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(1, 5))
                    .networkSynchronized(ByteBufCodecs.VAR_INT.map(
                            BloodMagicDataComponents::validateOrbTier, BloodMagicDataComponents::validateOrbTier))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Binding>> BINDING =
            DATA_COMPONENTS.register("binding", () -> DataComponentType.<Binding>builder()
                    .persistent(Binding.CODEC).networkSynchronized(Binding.STREAM_CODEC).build());

    private BloodMagicDataComponents() {
    }
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<wayoftime.bloodmagic.will.DemonWill>> DEMON_WILL = DATA_COMPONENTS.register("demon_will",
            () -> DataComponentType.<wayoftime.bloodmagic.will.DemonWill>builder().persistent(wayoftime.bloodmagic.will.DemonWill.CODEC).networkSynchronized(wayoftime.bloodmagic.will.DemonWill.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> RITUAL_SELECTION=DATA_COMPONENTS.register("ritual_selection",()->DataComponentType.<Integer>builder().persistent(Codec.intRange(0,2)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static int validateOrbTier(int tier) {
        if (tier < 1 || tier > 5) {
            throw new IllegalArgumentException("Blood orb tier must be between 1 and 5");
        }
        return tier;
    }
}
