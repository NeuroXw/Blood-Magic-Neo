package wayoftime.bloodmagic.common.block;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;

/** Stage 6A static dungeon decoration. Dungeon machinery and seals belong to stage 6B. */
public final class DungeonDecorativeBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BloodMagic.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BloodMagic.MODID);
    public static final Map<String, DeferredBlock<Block>> BLOCK_BY_ID = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> ITEM_BY_ID = new LinkedHashMap<>();

    private static final String[] IDS = {
            "dungeon_brick_assorted",
            "dungeon_brick_stairs",
            "dungeon_brick1",
            "dungeon_brick1_corrosive",
            "dungeon_brick1_destructive",
            "dungeon_brick1_gate",
            "dungeon_brick1_gate_corrosive",
            "dungeon_brick1_gate_destructive",
            "dungeon_brick1_gate_steadfast",
            "dungeon_brick1_gate_vengeful",
            "dungeon_brick1_slab",
            "dungeon_brick1_slab_corrosive",
            "dungeon_brick1_slab_destructive",
            "dungeon_brick1_slab_steadfast",
            "dungeon_brick1_slab_vengeful",
            "dungeon_brick1_stairs_corrosive",
            "dungeon_brick1_stairs_destructive",
            "dungeon_brick1_stairs_steadfast",
            "dungeon_brick1_stairs_vengeful",
            "dungeon_brick1_steadfast",
            "dungeon_brick1_vengeful",
            "dungeon_brick1_wall",
            "dungeon_brick1_wall_corrosive",
            "dungeon_brick1_wall_destructive",
            "dungeon_brick1_wall_steadfast",
            "dungeon_brick1_wall_vengeful",
            "dungeon_brick2",
            "dungeon_brick2_corrosive",
            "dungeon_brick2_destructive",
            "dungeon_brick2_steadfast",
            "dungeon_brick2_vengeful",
            "dungeon_brick3",
            "dungeon_brick3_corrosive",
            "dungeon_brick3_destructive",
            "dungeon_brick3_steadfast",
            "dungeon_brick3_vengeful",
            "dungeon_cracked_brick1",
            "dungeon_emitter",
            "dungeon_eye",
            "dungeon_eye_corrosive",
            "dungeon_eye_destructive",
            "dungeon_eye_steadfast",
            "dungeon_eye_vengeful",
            "dungeon_metal_corrosive",
            "dungeon_metal_destructive",
            "dungeon_metal_steadfast",
            "dungeon_metal_vengeful",
            "dungeon_ore",
            "dungeon_pillar_cap",
            "dungeon_pillar_cap_corrosive",
            "dungeon_pillar_cap_destructive",
            "dungeon_pillar_cap_steadfast",
            "dungeon_pillar_cap_vengeful",
            "dungeon_pillar_center",
            "dungeon_pillar_center_corrosive",
            "dungeon_pillar_center_destructive",
            "dungeon_pillar_center_steadfast",
            "dungeon_pillar_center_vengeful",
            "dungeon_pillar_special",
            "dungeon_pillar_special_corrosive",
            "dungeon_pillar_special_destructive",
            "dungeon_pillar_special_steadfast",
            "dungeon_pillar_special_vengeful",
            "dungeon_polished",
            "dungeon_polished_corrosive",
            "dungeon_polished_destructive",
            "dungeon_polished_gate",
            "dungeon_polished_gate_corrosive",
            "dungeon_polished_gate_destructive",
            "dungeon_polished_gate_steadfast",
            "dungeon_polished_gate_vengeful",
            "dungeon_polished_slab",
            "dungeon_polished_slab_corrosive",
            "dungeon_polished_slab_destructive",
            "dungeon_polished_slab_steadfast",
            "dungeon_polished_slab_vengeful",
            "dungeon_polished_stairs",
            "dungeon_polished_stairs_corrosive",
            "dungeon_polished_stairs_destructive",
            "dungeon_polished_stairs_steadfast",
            "dungeon_polished_stairs_vengeful",
            "dungeon_polished_steadfast",
            "dungeon_polished_vengeful",
            "dungeon_polished_wall",
            "dungeon_polished_wall_corrosive",
            "dungeon_polished_wall_destructive",
            "dungeon_polished_wall_steadfast",
            "dungeon_polished_wall_vengeful",
            "dungeon_regular_cracked_brick1",
            "dungeon_smallbrick",
            "dungeon_smallbrick_corrosive",
            "dungeon_smallbrick_destructive",
            "dungeon_smallbrick_steadfast",
            "dungeon_smallbrick_vengeful",
            "dungeon_stone",
            "dungeon_stone_corrosive",
            "dungeon_stone_destructive",
            "dungeon_stone_slab",
            "dungeon_stone_slab_corrosive",
            "dungeon_stone_slab_destructive",
            "dungeon_stone_slab_steadfast",
            "dungeon_stone_slab_vengeful",
            "dungeon_stone_stairs",
            "dungeon_stone_stairs_corrosive",
            "dungeon_stone_stairs_destructive",
            "dungeon_stone_stairs_steadfast",
            "dungeon_stone_stairs_vengeful",
            "dungeon_stone_steadfast",
            "dungeon_stone_vengeful",
            "dungeon_stone_wall",
            "dungeon_stone_wall_corrosive",
            "dungeon_stone_wall_destructive",
            "dungeon_stone_wall_steadfast",
            "dungeon_stone_wall_vengeful",
            "dungeon_tile",
            "dungeon_tile_corrosive",
            "dungeon_tile_destructive",
            "dungeon_tile_slab",
            "dungeon_tile_slab_corrosive",
            "dungeon_tile_slab_destructive",
            "dungeon_tile_slab_steadfast",
            "dungeon_tile_slab_vengeful",
            "dungeon_tile_steadfast",
            "dungeon_tile_vengeful",
            "dungeon_tile_wall",
            "dungeon_tile_wall_corrosive",
            "dungeon_tile_wall_destructive",
            "dungeon_tile_wall_steadfast",
            "dungeon_tile_wall_vengeful",
            "dungeon_tilespecial",
            "dungeon_tilespecial_corrosive",
            "dungeon_tilespecial_destructive",
            "dungeon_tilespecial_steadfast",
            "dungeon_tilespecial_vengeful",
    };

    static {
        for (String id : IDS) {
            DeferredBlock<Block> block = BLOCKS.registerBlock(id, properties -> create(id, properties));
            BLOCK_BY_ID.put(id, block);
            ITEM_BY_ID.put(id, ITEMS.registerSimpleBlockItem(block));
        }
    }

    private DungeonDecorativeBlocks() {}

    private static Block create(String id, BlockBehaviour.Properties properties) {
        properties = properties(id, properties);
        if (id.contains("_stairs")) return new StairBlock(Blocks.STONE.defaultBlockState(), properties);
        if (id.contains("_slab")) return new SlabBlock(properties);
        if (id.contains("_wall")) return new WallBlock(properties);
        if (id.contains("_gate")) return new DungeonFenceGateBlock(WoodType.OAK, properties);
        if (id.contains("_pillar_cap")) return new PillarCapBlock(properties);
        if (id.contains("_pillar_center") || id.contains("_pillar_special")) return new ConnectingPillarBlock(properties);
        return new Block(properties);
    }

    private static BlockBehaviour.Properties properties(String id, BlockBehaviour.Properties result) {
        result = result.sound(id.contains("_metal") ? SoundType.METAL : SoundType.STONE).requiresCorrectToolForDrops();
        if (id.equals("dungeon_brick_assorted")) result.strength(20.0F, 50.0F);
        else if (id.contains("dungeon_eye")) result.strength(2.0F, 5.0F).lightLevel(state -> 15);
        else if (id.equals("dungeon_emitter")) result.strength(2.0F, 5.0F).lightLevel(state -> 8);
        else result.strength(2.0F, 5.0F);
        return result;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static int totalRegisteredBlocks() { return BLOCK_BY_ID.size() + 1; }
    public static DeferredBlock<Block> block(String id) { return BLOCK_BY_ID.get(id); }
    public static DeferredItem<BlockItem> item(String id) { return ITEM_BY_ID.get(id); }


    /** Column segments inherit the axis of an adjacent segment unless sneak-placed. */
    public static final class ConnectingPillarBlock extends RotatedPillarBlock {
        public ConnectingPillarBlock(BlockBehaviour.Properties properties) { super(properties); }

        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) return state;
            // BlockPlaceContext#getClickedPos is already the target position in 26.1.
            BlockPos target = context.getClickedPos();
            BlockState clicked = context.getLevel().getBlockState(target);
            if (clicked.getBlock() instanceof ConnectingPillarBlock) return state.setValue(AXIS, clicked.getValue(AXIS));
            int[] counts = new int[Direction.Axis.values().length];
            for (Direction direction : Direction.values()) {
                BlockState neighbor = context.getLevel().getBlockState(target.relative(direction));
                if (neighbor.getBlock() instanceof ConnectingPillarBlock) counts[neighbor.getValue(AXIS).ordinal()]++;
            }
            Direction.Axis best = null;
            for (Direction.Axis axis : Direction.Axis.values()) {
                if (best == null || counts[axis.ordinal()] > counts[best.ordinal()]) best = axis;
            }
            return best == null || counts[best.ordinal()] == 0 ? state : state.setValue(AXIS, best);
        }
    }

    /** The 26.1 visual is one block high, while the vanilla gate still collides up to 1.5 blocks. */
    public static final class DungeonFenceGateBlock extends FenceGateBlock {
        private static final Map<Direction.Axis, VoxelShape> COLLISION = Shapes.rotateHorizontalAxis(
                Block.column(16.0D, 4.0D, 0.0D, 16.0D));

        public DungeonFenceGateBlock(WoodType type, BlockBehaviour.Properties properties) { super(type, properties); }

        @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                                          net.minecraft.world.phys.shapes.CollisionContext context) {
            return state.getValue(OPEN) ? Shapes.empty() : COLLISION.get(state.getValue(FACING).getAxis());
        }
    }

    /** Upstream pillar caps expose all six placement directions. */
    public static final class PillarCapBlock extends Block {
        public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
        public PillarCapBlock(BlockBehaviour.Properties properties) { super(properties); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getClickedFace()); }
        @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
        @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    }
}
