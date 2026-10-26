package wayoftime.bloodmagic.common.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import wayoftime.bloodmagic.common.block.entity.*;
public final class BlockHellfireForge extends BaseEntityBlock {
    public static final MapCodec<BlockHellfireForge> CODEC = simpleCodec(BlockHellfireForge::new);
    // Match soulforge.json: inset by one pixel, with a twelve-pixel height.
    // A full-cube shape would hide adjacent block faces across the visible gaps.
    private static final VoxelShape BODY = box(1, 0, 1, 15, 12, 15);
    public BlockHellfireForge(Properties p) { super(p); }
    @Override protected MapCodec<? extends BlockHellfireForge> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return BODY; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HellfireForgeBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return level.isClientSide() ? null : createTickerHelper(type, BloodMagicBlockEntities.HELLFIRE_FORGE.get(), HellfireForgeBlockEntity::serverTick); }
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!player.isAlive() || player.isSpectator() || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return level.getBlockEntity(pos) instanceof HellfireForgeBlockEntity forge && forge.stillValid(player) && player.openMenu(forge).isPresent() ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : open(level, pos, player); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return open(level, pos, player); }
}
