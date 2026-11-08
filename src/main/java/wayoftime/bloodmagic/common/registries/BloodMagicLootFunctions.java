package wayoftime.bloodmagic.common.registries;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.loot.SetWillFraction;
import wayoftime.bloodmagic.common.loot.SetWillRange;

/** Custom loot function codecs used by the original Blood Magic loot tables. */
public final class BloodMagicLootFunctions {
    public static final DeferredRegister<MapCodec<? extends LootItemFunction>> TYPES = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, BloodMagic.MODID);
    public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<SetWillRange>> SET_WILL_RANGE = TYPES.register("set_will_range", () -> SetWillRange.CODEC);
    public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<SetWillFraction>> SET_WILL_FRACTION = TYPES.register("set_will_fraction", () -> SetWillFraction.CODEC);
    private BloodMagicLootFunctions() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
