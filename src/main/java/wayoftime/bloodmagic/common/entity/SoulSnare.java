package wayoftime.bloodmagic.common.entity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.registries.BloodMagicWill;
public final class SoulSnare extends ThrowableItemProjectile {
    public SoulSnare(EntityType<? extends SoulSnare> type, Level level) { super(type, level); }
    public SoulSnare(Level level, LivingEntity owner, ItemStack stack) { super(BloodMagicWill.SOUL_SNARE.get(), owner, level, stack.copyWithCount(1)); }
    @Override protected Item getDefaultItem() { return BloodMagicItems.SOUL_SNARE.get(); }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if (!level().isClientSide() && hit.getEntity() instanceof Mob mob && hit.getEntity() != getOwner()) mob.addEffect(new MobEffectInstance(BloodMagicWill.SNARED, 300));
        super.onHitEntity(hit);
    }
    @Override protected void onHit(HitResult hit) { super.onHit(hit); if (!level().isClientSide()) discard(); }
}
