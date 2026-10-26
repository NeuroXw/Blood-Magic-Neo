package wayoftime.bloodmagic.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import wayoftime.bloodmagic.common.block.entity.*;

public final class BlockAlchemyArray extends BaseEntityBlock {
    public static final MapCodec<BlockAlchemyArray> CODEC = simpleCodec(BlockAlchemyArray::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1, 15);
    public BlockAlchemyArray(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BlockAlchemyArray> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AlchemyArrayBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, BloodMagicBlockEntities.ALCHEMY_ARRAY.get(), AlchemyArrayBlockEntity::serverTick);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return wayoftime.bloodmagic.ritual.WorldAccess.loaded(level, pos.below()) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbor, BlockState neighborState, RandomSource random) {
        return direction == Direction.DOWN && wayoftime.bloodmagic.ritual.WorldAccess.loaded(level, pos.below()) && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return level.getBlockEntity(pos) instanceof AlchemyArrayBlockEntity array ? array.interact(player, hand) : InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return level.getBlockEntity(pos) instanceof AlchemyArrayBlockEntity array ? array.interact(player, InteractionHand.MAIN_HAND) : InteractionResult.PASS;
    }
}
