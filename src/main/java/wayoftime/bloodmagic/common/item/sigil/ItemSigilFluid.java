package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Basic world-fluid behavior. Tank capabilities/alchemy-array integration are separate subsystems. */
public final class ItemSigilFluid extends ItemSigilBase {
    public enum Kind { WATER, LAVA, VOID }
    private final Kind kind;

    public ItemSigilFluid(Properties properties, Kind kind) {
        super(properties, kind.name().toLowerCase(java.util.Locale.ROOT), switch (kind) {
            case WATER -> 100;
            case LAVA -> 1000;
            case VOID -> 50;
        });
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.FAIL;
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) bindIfAbsent(player, stack);
        BlockHitResult hit = getPlayerPOVHitResult(level, player,
                kind == Kind.VOID ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        return hit.getType() == HitResult.Type.BLOCK ? useAt(level, player, stack, hit) : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Void must ray trace sources, including waterlogged blocks, rather than the block behind water.
        if (kind == Kind.VOID && context.getPlayer() != null)
            return use(context.getLevel(), context.getPlayer(), context.getHand());
        return useAt(context.getLevel(), context.getPlayer(), stack,
                new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()));
    }

    private InteractionResult useAt(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        if (!canUse(player)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        bindIfAbsent(player, stack);
        BlockPos clicked = hit.getBlockPos();
        if (!server.getChunkSource().hasChunk(clicked.getX() >> 4, clicked.getZ() >> 4) || !player.isWithinBlockInteractionRange(clicked, 0)
                || !server.mayInteract(player, clicked)) return InteractionResult.FAIL;
        var state = server.getBlockState(clicked);
        if (kind == Kind.VOID) {
            if (!player.mayUseItemAt(clicked, hit.getDirection(), stack) || !state.getFluidState().isSource()
                    || !(state.getBlock() instanceof BucketPickup pickup)) return InteractionResult.FAIL;
            return activate(server, player, stack, () -> !pickup.pickupBlock(player, server, clicked, state).isEmpty())
                    ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        Fluid fluid = kind == Kind.WATER ? Fluids.WATER : Fluids.LAVA;
        BlockPos target = state.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, server, clicked, state, fluid) ? clicked : clicked.relative(hit.getDirection());
        if (!server.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4) || !server.isInWorldBounds(target) || !server.mayInteract(player, target)
                || !player.mayUseItemAt(target, hit.getDirection(), stack)) return InteractionResult.FAIL;
        var targetState = server.getBlockState(target);
        boolean waterlog = targetState.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, server, target, targetState, fluid);
        if (!waterlog && !(targetState.isAir() || targetState.canBeReplaced(fluid))) return InteractionResult.FAIL;
        if (targetState.getFluidState().isSource() && targetState.getFluidState().getType() == fluid) return InteractionResult.FAIL;
        BucketItem bucket = (BucketItem) (kind == Kind.WATER ? Items.WATER_BUCKET : Items.LAVA_BUCKET);
        // Vanilla handles 26.1 environment attributes (Nether evaporation), sounds and waterlogging.
        return activate(server, player, stack, () -> bucket.emptyContents(player, server, target, null, stack))
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
}
