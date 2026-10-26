package wayoftime.bloodmagic.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;

public final class BlockAltar extends BaseEntityBlock {
    public static final MapCodec<BlockAltar> CODEC = simpleCodec(BlockAltar::new);
    private static final VoxelShape BODY = box(0, 0, 0, 16, 12, 16);

    public BlockAltar(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BlockAltar> codec() {
        return CODEC;
    }

    @Override
    public BloodAltarBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloodAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type,
                BloodMagicBlockEntities.BLOOD_ALTAR.get(), BloodAltarBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        // Let vanilla dispatch empty main-hand use through useWithoutItem exactly once.
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return level.getBlockEntity(pos) instanceof BloodAltarBlockEntity altar
                ? altar.interact(player, hand) : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return level.getBlockEntity(pos) instanceof BloodAltarBlockEntity altar
                ? altar.interact(player, InteractionHand.MAIN_HAND) : InteractionResult.PASS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BODY;
    }
}
