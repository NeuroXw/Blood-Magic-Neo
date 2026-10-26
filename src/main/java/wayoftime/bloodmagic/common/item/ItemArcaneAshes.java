package wayoftime.bloodmagic.common.item;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.AlchemyArrayBlockEntity;

/** Upstream drawing chalk: Arcane Ashes, twenty successful placements per item. */
public final class ItemArcaneAshes extends Item {
    public ItemArcaneAshes(Properties properties) { super(properties.durability(20)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos().above();
        var state = BloodMagicBlocks.ALCHEMY_ARRAY.get().defaultBlockState();
        if (player == null || !player.isAlive() || player.isSpectator() || !player.mayBuild() || context.getClickedFace() != Direction.UP
                || !wayoftime.bloodmagic.ritual.WorldAccess.loaded(level, pos) || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)
                || !player.isWithinBlockInteractionRange(context.getClickedPos(), 0) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, Direction.UP, context.getItemInHand()) || !level.isEmptyBlock(pos)
                || !state.canSurvive(level, pos)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!level.setBlockAndUpdate(pos, state)) return InteractionResult.FAIL;
        if (level.getBlockEntity(pos) instanceof AlchemyArrayBlockEntity array) array.initialize(player.getUUID(), player.getDirection());
        if (!player.hasInfiniteMaterials()) context.getItemInHand().hurtAndBreak(1, player, context.getHand());
        return InteractionResult.SUCCESS_SERVER;
    }
}
