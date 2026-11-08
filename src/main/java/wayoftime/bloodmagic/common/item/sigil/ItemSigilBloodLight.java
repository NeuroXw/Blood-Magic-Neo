package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;

public final class ItemSigilBloodLight extends ItemSigilBase {
    public ItemSigilBloodLight(Properties properties) { super(properties, "bloodlight", 10); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.FAIL;
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        bindIfAbsent(player, stack);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return InteractionResult.FAIL;
        BlockPos target = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (!level.isEmptyBlock(target) || !player.mayUseItemAt(target, blockHit.getDirection(), stack)) return InteractionResult.FAIL;
        boolean paid = activate(server, player, stack, () -> level.setBlockAndUpdate(target,
                BloodMagicBlocks.BLOOD_LIGHT.get().defaultBlockState()));
        if (!paid) return InteractionResult.FAIL;
        player.getCooldowns().addCooldown(stack, 10);
        level.playSound(null, target, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, .6F, 1.6F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
