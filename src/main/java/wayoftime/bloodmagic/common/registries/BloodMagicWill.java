package wayoftime.bloodmagic.common.registries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.neoforged.neoforge.registries.*;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.entity.SoulSnare;
public final class BloodMagicWill {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, BloodMagic.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, BloodMagic.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<SoulSnare>> SOUL_SNARE = ENTITIES.register("soulsnare",
            () -> EntityType.Builder.<SoulSnare>of(SoulSnare::new, MobCategory.MISC).sized(.25F, .25F).clientTrackingRange(4).updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, BloodMagic.rl("soulsnare"))));
    public static final DeferredHolder<MobEffect, MobEffect> SNARED = EFFECTS.register("soul_snare", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xDDDDFF) {});
    private BloodMagicWill() {}
}
