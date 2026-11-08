package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Paid Frost Walker equivalent: freezes nearby surface water and never replaces solid blocks. */
public final class ItemSigilFrost extends ItemSigilToggleableBase {
    private static final int RADIUS = 2;

    public ItemSigilFrost(Properties properties) { super(properties, "frost", 100); }

    @Override protected void onSigilUpdate(ItemStack stack, ServerLevel level, Player player) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -1, -RADIUS),
                center.offset(RADIUS, 1, RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.WATER) || !state.getFluidState().isSourceOfType(Fluids.WATER)) continue;
            if (!level.getFluidState(pos.above()).isEmpty() || !level.getBlockState(pos.above()).canBeReplaced()) continue;
            level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
        }
    }
}
