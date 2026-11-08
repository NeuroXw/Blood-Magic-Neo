package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;

/** Stores a dimensional block anchor on sneak-use and teleports back on normal use. */
public final class ItemSigilTeleposition extends ItemSigilBase {
    public ItemSigilTeleposition(Properties properties) { super(properties, "teleposition", 1000); }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer(); ItemStack stack = context.getItemInHand();
        if (player == null || !canUse(player)) return InteractionResult.FAIL;
        if (!player.isShiftKeyDown() || !(context.getLevel() instanceof ServerLevel)) return InteractionResult.PASS;
        bindIfAbsent(player, stack);
        BlockPos pos = context.getClickedPos().immutable();
        stack.set(BloodMagicDataComponents.TELEPOS_POS.get(), pos);
        stack.set(BloodMagicDataComponents.TELEPOS_DIMENSION.get(), context.getLevel().dimension());
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.bloodmagic.telepos.bound", pos.getX(), pos.getY(), pos.getZ()));
        return InteractionResult.SUCCESS_SERVER;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.FAIL;
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        bindIfAbsent(player, stack);
        BlockPos pos = stack.get(BloodMagicDataComponents.TELEPOS_POS.get());
        ResourceKey<Level> dimension = stack.get(BloodMagicDataComponents.TELEPOS_DIMENSION.get());
        if (pos == null || dimension == null || !dimension.equals(level.dimension())) return InteractionResult.FAIL;
        var targetLevel = server.getServer().getLevel(dimension);
        if (targetLevel == null || !targetLevel.isLoaded(pos)) return InteractionResult.FAIL;
        return activate(server, player, stack, () -> {
            if (!player.teleportTo(targetLevel, pos.getX()+.5, pos.getY()+1, pos.getZ()+.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true)) return false;
            targetLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
            return true;
        }) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
}
