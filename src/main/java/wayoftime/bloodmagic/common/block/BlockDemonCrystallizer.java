package wayoftime.bloodmagic.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;
import wayoftime.bloodmagic.common.block.entity.DemonCrystallizerBlockEntity;

public final class BlockDemonCrystallizer extends BaseEntityBlock {
    public static final MapCodec<BlockDemonCrystallizer> CODEC = simpleCodec(BlockDemonCrystallizer::new);
    private static final VoxelShape BODY = box(2, 2, 2, 14, 16, 14);
    public BlockDemonCrystallizer(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BlockDemonCrystallizer> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return BODY; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DemonCrystallizerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, BloodMagicBlockEntities.DEMON_CRYSTALLIZER.get(), DemonCrystallizerBlockEntity::serverTick);
    }
}
