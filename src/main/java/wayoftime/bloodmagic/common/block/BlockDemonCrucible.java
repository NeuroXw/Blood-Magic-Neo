package wayoftime.bloodmagic.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;
import wayoftime.bloodmagic.common.block.entity.DemonCrucibleBlockEntity;
import wayoftime.bloodmagic.common.item.ItemDemonCrystal;
import wayoftime.bloodmagic.common.item.ItemDemonWill;

/** Upstream BlockDemonCrucible: one slot, redstone-controlled aura/gem transfer. */
public final class BlockDemonCrucible extends BaseEntityBlock {
    public static final MapCodec<BlockDemonCrucible> CODEC = simpleCodec(BlockDemonCrucible::new);
    private static final VoxelShape BODY = box(1, 0, 1, 15, 12, 15);
    public BlockDemonCrucible(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BlockDemonCrucible> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return BODY; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DemonCrucibleBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, BloodMagicBlockEntities.DEMON_CRUCIBLE.get(), DemonCrucibleBlockEntity::serverTick);
    }
    private InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof DemonCrucibleBlockEntity crucible)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        ItemStack slot = crucible.getItem(0);
        if (slot.isEmpty()) {
            if (held.isEmpty() || (!(held.getItem() instanceof ItemDemonCrystal) && !(held.getItem() instanceof ItemDemonWill))) return InteractionResult.PASS;
            crucible.setItem(0, held.copyWithCount(1));
            if (!player.hasInfiniteMaterials()) held.shrink(1);
        } else if (held.isEmpty()) {
            if (!player.getInventory().add(slot)) Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, slot);
            crucible.clearContent();
        } else return InteractionResult.PASS;
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        return InteractionResult.SUCCESS_SERVER;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return interact(level, pos, player, hand); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return interact(level, pos, player, InteractionHand.MAIN_HAND); }
}
