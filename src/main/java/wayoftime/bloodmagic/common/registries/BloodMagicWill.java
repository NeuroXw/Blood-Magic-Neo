package wayoftime.bloodmagic.common.registries;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.entity.SoulSnare;

/** Demon Will related entities and effects used by the stage 1 ritual integration. */
public final class BloodMagicWill {
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BloodMagic.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, BloodMagic.MODID);

    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<SoulSnare>> SOUL_SNARE =
            ENTITIES.register("soulsnare", () -> net.minecraft.world.entity.EntityType.Builder
                    .<SoulSnare>of(SoulSnare::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(4).updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, BloodMagic.rl("soulsnare"))));

    public static final DeferredHolder<MobEffect, MobEffect> SNARED = simple("soul_snare", MobEffectCategory.NEUTRAL, 0xDDDDFF);
    public static final DeferredHolder<MobEffect, MobEffect> FIRE_FUSE = EFFECTS.register("firefuse", FireFuse::new);
    public static final DeferredHolder<MobEffect, MobEffect> SOUL_FRAY = simple("soulfray", MobEffectCategory.HARMFUL, 0xFFFFFFFF);
    public static final DeferredHolder<MobEffect, MobEffect> PLANT_LEECH = EFFECTS.register("plantleech", PlantLeech::new);
    public static final DeferredHolder<MobEffect, MobEffect> SACRIFICIAL_LAMB = EFFECTS.register("sacrificallamb", SacrificialLamb::new);
    public static final DeferredHolder<MobEffect, MobEffect> SUSPENDED = EFFECTS.register("suspended", Suspended::new);
    public static final DeferredHolder<MobEffect, MobEffect> GROUNDED = simple("grounded", MobEffectCategory.HARMFUL, 0xBA855B);
    public static final DeferredHolder<MobEffect, MobEffect> GRAVITY = EFFECTS.register("gravity",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x800080) {
            }.addAttributeModifier(Attributes.GRAVITY, BloodMagic.rl("gravity"), 0.5,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static final DeferredHolder<MobEffect, MobEffect> HEAVY_HEART = EFFECTS.register("heavy_heart", HeavyHeart::new);
    public static final DeferredHolder<MobEffect, MobEffect> SOFT_FALL = EFFECTS.register("soft_fall", SoftFall::new);

    private BloodMagicWill() {
    }

    private static DeferredHolder<MobEffect, MobEffect> simple(String id, MobEffectCategory category, int color) {
        return EFFECTS.register(id, () -> new MobEffect(category, color) {
        });
    }

    private static final class PlantLeech extends MobEffect {
        private PlantLeech() {
            super(MobEffectCategory.HARMFUL, 0x00FF00FF);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            int grown = 0;
            int max = 25 * (1 + amplifier);
            for (int i = 0; i < max; i++) {
                var pos = entity.blockPosition().offset(level.getRandom().nextInt(3) - 1,
                        level.getRandom().nextInt(3) - 1, level.getRandom().nextInt(3) - 1);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof BonemealableBlock growable) {
                    growable.performBonemeal(level, level.getRandom(), pos, state);
                    if (!level.getBlockState(pos).equals(state)) grown++;
                }
            }
            double damage = grown * (0.5 * 3.0 / (amplifier + 3));
            if (damage > 0) entity.hurtServer(level, level.damageSources().magic(), (float) damage);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0;
        }
    }

    private static final class SacrificialLamb extends MobEffect {
        private SacrificialLamb() {
            super(MobEffectCategory.HARMFUL, 0xFFFFFF);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            if (!(entity instanceof PathfinderMob animal)) return true;
            if (animal.getTarget() == null || !animal.getTarget().isAlive()) {
                var target = level.getEntitiesOfClass(Monster.class, animal.getBoundingBox().inflate(8),
                        monster -> monster.isAlive()).stream().findFirst().orElse(null);
                if (target != null) animal.setTarget(target);
            }
            if (animal.getTarget() != null && animal.distanceToSqr(animal.getTarget()) < 4)
                level.explode(null, animal.getX(), animal.getY() + animal.getBbHeight() / 16.0,
                        animal.getZ(), 2 + amplifier * 1.5F, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            return true;
        }
    }

    private static final class Suspended extends MobEffect {
        private Suspended() {
            super(MobEffectCategory.NEUTRAL, 0x23DDE1);
        }

        @Override
        public void onEffectStarted(LivingEntity entity, int amplifier) {
            entity.setNoGravity(true);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            MobEffectInstance instance = entity.getEffect(SUSPENDED);
            entity.setNoGravity(instance != null && instance.getDuration() > 1);
            return true;
        }

        @Override
        public void onMobRemoved(ServerLevel level, LivingEntity entity, int amplifier,
                                 net.minecraft.world.entity.Entity.RemovalReason reason) {
            entity.setNoGravity(false);
        }
    }

    private static final class HeavyHeart extends MobEffect {
        private HeavyHeart() {
            super(MobEffectCategory.HARMFUL, 0x000000);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            var motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, motion.y - 0.03 * (amplifier + 1), motion.z);
            return true;
        }
    }

    private static final class SoftFall extends MobEffect {
        private SoftFall() {
            super(MobEffectCategory.BENEFICIAL, 0x4AEDD9);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            entity.fallDistance = 0;
            return true;
        }
    }

    private static final class FireFuse extends MobEffect {
        private FireFuse() {
            super(MobEffectCategory.HARMFUL, 0xFF0000FF);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.2, entity.getZ(),
                    1, 0.2, 0.1, 0.2, 0.01);
            MobEffectInstance instance = entity.getEffect(FIRE_FUSE);
            if (instance != null && instance.getDuration() <= 3) {
                net.minecraft.world.level.Level.ExplosionInteraction interaction = level.getGameRules().get(
                        net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)
                        ? net.minecraft.world.level.Level.ExplosionInteraction.MOB : net.minecraft.world.level.Level.ExplosionInteraction.NONE;
                level.explode(null, entity.getX(), entity.getY(), entity.getZ(), amplifier + 1, false, interaction);
            }
            return true;
        }
    }
}

