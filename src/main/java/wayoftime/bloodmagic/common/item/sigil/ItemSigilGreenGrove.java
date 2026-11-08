package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import wayoftime.bloodmagic.common.item.BloodMagicItems;

public final class ItemSigilGreenGrove extends ItemSigilToggleableBase {
    public ItemSigilGreenGrove(Properties properties) { super(properties, "green_grove", 150); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !canUse(player)) return InteractionResult.FAIL;
        if (!(context.getLevel() instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        bindIfAbsent(player, stack);
        return activate(server, player, stack, () -> applyBonemeal(server, player, context.getClickedPos()))
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
    private static boolean applyBonemeal(ServerLevel level, Player player, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock growable)
                || !growable.isValidBonemealTarget(level, pos, state)) return false;
        if (!growable.isBonemealSuccess(level, level.getRandom(), pos, state)) return false;
        growable.performBonemeal(level, level.getRandom(), pos, state);
        level.levelEvent(2005, pos, 0);
        return true;
    }
    @Override protected void onSigilUpdate(ItemStack stack, ServerLevel level, Player player) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
            if (level.getRandom().nextInt(50) != 0) continue;
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock growable && state.getBlock() != Blocks.GRASS_BLOCK
                    && growable.isValidBonemealTarget(level, pos, state)
                    && growable.isBonemealSuccess(level, level.getRandom(), pos, state)) {
                growable.performBonemeal(level, level.getRandom(), pos, state);
                level.levelEvent(2005, pos, 0);
            }
        }
    }
}
