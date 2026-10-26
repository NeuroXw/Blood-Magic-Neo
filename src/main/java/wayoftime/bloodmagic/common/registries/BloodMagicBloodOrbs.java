package wayoftime.bloodmagic.common.registries;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.item.BloodOrb;

public final class BloodMagicBloodOrbs {
    public static final ResourceKey<Registry<BloodOrb>> KEY = ResourceKey.createRegistryKey(BloodMagic.rl("blood_orb"));
    public static final DeferredRegister<BloodOrb> ORBS = DeferredRegister.create(KEY, BloodMagic.MODID);
    public static final Registry<BloodOrb> REGISTRY = ORBS.makeRegistry(builder -> builder.sync(true));
    public static final DeferredHolder<BloodOrb, BloodOrb> WEAK = ORBS.register("weakbloodorb", () -> new BloodOrb(1, 5_000, 2));
    public static final DeferredHolder<BloodOrb, BloodOrb> APPRENTICE = ORBS.register("apprenticebloodorb", () -> new BloodOrb(2, 25_000, 5));
    public static final DeferredHolder<BloodOrb, BloodOrb> MAGICIAN = ORBS.register("magicianbloodorb", () -> new BloodOrb(3, 150_000, 15));
    public static final DeferredHolder<BloodOrb, BloodOrb> MASTER = ORBS.register("masterbloodorb", () -> new BloodOrb(4, 1_000_000, 25));
    public static final DeferredHolder<BloodOrb, BloodOrb> ARCHMAGE = ORBS.register("archmagebloodorb", () -> new BloodOrb(5, 10_000_000, 50));
    private static final List<DeferredHolder<BloodOrb, BloodOrb>> TIERS = List.of(WEAK, APPRENTICE, MAGICIAN, MASTER, ARCHMAGE);

    private BloodMagicBloodOrbs() {}

    public static BloodOrb forTier(int tier) {
        return TIERS.get(BloodMagicDataComponents.validateOrbTier(tier) - 1).get();
    }
}
