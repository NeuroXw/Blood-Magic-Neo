package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.core.data.SoulNetworks;

public final class ItemSigilDivination extends ItemSigilBase {
    private final boolean seer;

    public ItemSigilDivination(Properties properties, boolean seer) {
        super(properties, seer ? "seer" : "divination", 0);
        this.seer = seer;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return read(level, player, player.getItemInHand(hand), getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Read before the altar's generic insertion handler; do not insert the reader into its slot.
        return read(context.getLevel(), context.getPlayer(), stack,
                new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()));
    }

    private InteractionResult read(Level level, Player player, ItemStack stack, HitResult hit) {
        if (!canUse(player)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        var owner = bindIfAbsent(player, stack);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && player.isWithinBlockInteractionRange(blockHit.getBlockPos(), 0)
                && server.getBlockEntity(blockHit.getBlockPos()) instanceof BloodAltarBlockEntity altar) {
            altar.refreshStructure();
            var state = altar.getAltarState();
            player.sendOverlayMessage(Component.translatable("message.bloodmagic.altar", state.tier(), state.blood(), state.capacity()));
            if (seer) player.sendSystemMessage(Component.translatable("message.bloodmagic.altar.progress",
                    state.progress(), state.requiredBlood(), state.inputBlood(), state.outputBlood()));
        } else {
            var network = SoulNetworks.get(server).get(owner.ownerId());
            player.sendOverlayMessage(Component.translatable("message.bloodmagic.network", owner.ownerName(),
                    network.currentEssence(), network.orbTier()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
