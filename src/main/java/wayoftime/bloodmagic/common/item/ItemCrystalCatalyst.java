package wayoftime.bloodmagic.common.item;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.block.BlockDemonCrystal;
import wayoftime.bloodmagic.common.block.entity.DemonCrystalBlockEntity;

/** Upstream ItemCrystalCatalyst(type, injectedWill, speedModifier, conversionRate, maxInjectedWill). */
public final class ItemCrystalCatalyst extends ItemBase {
    public final EnumDemonWillType type;
    public final double injectedWill, speedModifier, conversionRate, maxInjectedWill;
    public ItemCrystalCatalyst(Properties p, EnumDemonWillType type, double injectedWill, double speedModifier, double conversionRate, double maxInjectedWill) {
        super(p, "crystalCatalyst");
        this.type = type; this.injectedWill = injectedWill; this.speedModifier = speedModifier; this.conversionRate = conversionRate; this.maxInjectedWill = maxInjectedWill;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); var pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof DemonCrystalBlockEntity crystal)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!applyCatalyst(crystal)) return InteractionResult.SUCCESS_SERVER;
        var particle = new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(BlockDemonCrystal.crystalItem(type)));
        server.sendParticles(particle, pos.getX() + .5, pos.getY() + .6, pos.getZ() + .5, 8, .2, .2, .2, .03);
        var player = context.getPlayer();
        if (player == null || !player.hasInfiniteMaterials()) context.getItemInHand().shrink(1);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1, 1);
        return InteractionResult.SUCCESS_SERVER;
    }
    public boolean applyCatalyst(DemonCrystalBlockEntity crystal) {
        if (type != crystal.getWillType() || crystal.getInjectedWill() + injectedWill > maxInjectedWill) return false;
        crystal.applyCatalyst(injectedWill, speedModifier, conversionRate);
        return true;
    }
}
