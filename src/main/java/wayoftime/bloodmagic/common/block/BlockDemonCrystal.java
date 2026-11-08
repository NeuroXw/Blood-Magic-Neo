package wayoftime.bloodmagic.common.block;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;
import wayoftime.bloodmagic.common.block.entity.DemonCrystalBlockEntity;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemCrystalCatalyst;
import wayoftime.bloodmagic.common.item.ItemDemonCrystal;
import wayoftime.bloodmagic.will.PlayerDemonWillHandler;

/** Upstream BlockDemonCrystal with the original age/count and attached-face state. */
public final class BlockDemonCrystal extends BaseEntityBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 6);
    public static final EnumProperty<Direction> ATTACHED = EnumProperty.create("attached", Direction.class);
    private static final EnumMap<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    static {
        SHAPES.put(Direction.UP, box(6, 0, 5, 10, 13, 9)); SHAPES.put(Direction.DOWN, box(6, 3, 7, 10, 16, 11));
        SHAPES.put(Direction.NORTH, box(6, 5, 3, 10, 9, 16)); SHAPES.put(Direction.SOUTH, box(6, 7, 0, 10, 11, 13));
        SHAPES.put(Direction.EAST, box(0, 6, 5, 13, 10, 9)); SHAPES.put(Direction.WEST, box(3, 6, 5, 16, 10, 9));
    }
    public static MapCodec<BlockDemonCrystal> codec(EnumDemonWillType type) { return simpleCodec(p -> new BlockDemonCrystal(p, type)); }
    private final EnumDemonWillType type;
    public BlockDemonCrystal(Properties properties, EnumDemonWillType type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(ATTACHED, Direction.UP).setValue(AGE, 0));
        registerDefaultState(stateDefinition.any().setValue(ATTACHED, Direction.UP).setValue(AGE, 0));
    }
    @Override protected MapCodec<? extends BlockDemonCrystal> codec() { return codec(type); }
    public EnumDemonWillType willType() { return type; }
    public static ItemStack crystalItem(EnumDemonWillType type) {
        return new ItemStack(switch (type) {
            case CORROSIVE -> BloodMagicItems.CORROSIVE_CRYSTAL.get();
            case DESTRUCTIVE -> BloodMagicItems.DESTRUCTIVE_CRYSTAL.get();
            case STEADFAST -> BloodMagicItems.STEADFAST_CRYSTAL.get();
            case VENGEFUL -> BloodMagicItems.VENGEFUL_CRYSTAL.get();
            case DEFAULT -> BloodMagicItems.RAW_CRYSTAL.get();
        });
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ATTACHED, AGE); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES.get(state.getValue(ATTACHED)); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction attached = state.getValue(ATTACHED); BlockPos support = pos.relative(attached.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, attached);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState state = defaultBlockState().setValue(ATTACHED, direction.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) return state;
        }
        return null;
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbor, BlockState neighborState, RandomSource random) {
        return direction.getOpposite() == state.getValue(ATTACHED) && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DemonCrystalBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, BloodMagicBlockEntities.DEMON_CRYSTAL.get(), DemonCrystalBlockEntity::serverTick);
    }
    private InteractionResult harvest(BlockState state, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof DemonCrystalBlockEntity crystal)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        boolean holdingCrystal = player.getMainHandItem().getItem() instanceof ItemDemonCrystal;
        if (PlayerDemonWillHandler.getTotalDemonWill(type, player) > 512 && !(holdingCrystal && player.isCreative()) && crystal.dropSingleCrystal())
            return InteractionResult.SUCCESS_SERVER;
        if (player.isCreative() && holdingCrystal && crystal.getCrystalCount() < 7) {
            crystal.internalCounter = 0;
            if (crystal.progressToNextCrystal > 0) crystal.progressToNextCrystal--;
            crystal.setCrystalCount(crystal.getCrystalCount() + 1);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return harvest(state, level, pos, player); }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Do not let automatic harvesting consume the click before a catalyst's item handler runs.
        return stack.getItem() instanceof ItemCrystalCatalyst ? InteractionResult.PASS : harvest(state, level, pos, player);
    }
}

