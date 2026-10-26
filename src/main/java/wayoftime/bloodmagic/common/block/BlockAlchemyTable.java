package wayoftime.bloodmagic.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import wayoftime.bloodmagic.common.block.entity.*;

/** Two blocks, exactly one inventory/ticker. The secondary half never owns items. */
public final class BlockAlchemyTable extends BaseEntityBlock {
    public static final MapCodec<BlockAlchemyTable> CODEC = simpleCodec(BlockAlchemyTable::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public BlockAlchemyTable(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT)); }
    @Override protected MapCodec<? extends BlockAlchemyTable> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, PART); }
    public static BlockPos other(BlockPos pos, BlockState state) { return pos.relative(state.getValue(PART) == BedPart.FOOT ? state.getValue(FACING) : state.getValue(FACING).getOpposite()); }
    public static boolean complete(LevelReader level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BlockAlchemyTable)) return false;
        BlockPos other = other(pos, state);
        if (!wayoftime.bloodmagic.ritual.WorldAccess.loaded(level, other)) return false;
        BlockState next = level.getBlockState(other);
        return next.is(state.getBlock()) && next.getValue(FACING) == state.getValue(FACING) && next.getValue(PART) != state.getValue(PART);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        BlockPos other = other(context.getClickedPos(), state);
        var level = context.getLevel(); var player = context.getPlayer();
        return wayoftime.bloodmagic.ritual.WorldAccess.loaded(level, other) && level.getWorldBorder().isWithinBounds(other) && level.getBlockState(other).canBeReplaced(context)
                && player != null && player.mayUseItemAt(other, context.getClickedFace(), context.getItemInHand()) && level.mayInteract(player, other) ? state : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide()) level.setBlockAndUpdate(other(pos, state), state.setValue(PART, BedPart.HEAD));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(PART) == BedPart.FOOT ? new AlchemyTableBlockEntity(pos, state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(PART) != BedPart.FOOT ? null : createTickerHelper(type, BloodMagicBlockEntities.ALCHEMY_TABLE.get(), AlchemyTableBlockEntity::serverTick);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbor, BlockState neighborState, RandomSource random) {
        if (neighbor.equals(other(pos, state))) ticks.scheduleTick(pos, this, 1);
        return state;
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!wayoftime.bloodmagic.ritual.WorldAccess.loaded(level, other(pos, state))) level.scheduleTick(pos, this, 20);
        else if (!complete(level, pos, state)) level.destroyBlock(pos, true);
    }
    private InteractionResult open(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.isAlive() || player.isSpectator() || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!complete(level, pos, state)) return InteractionResult.FAIL;
        BlockPos master = state.getValue(PART) == BedPart.FOOT ? pos : other(pos, state);
        return level.getBlockEntity(master) instanceof AlchemyTableBlockEntity table && table.stillValid(player)
                && player.openMenu(table).isPresent() ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : open(level, pos, state, player); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return open(level, pos, state, player); }
}
