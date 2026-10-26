package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ItemSigilAir extends ItemSigilBase {
    public ItemSigilAir(Properties properties) { super(properties, "air", 50); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        return activate(server, player, player.getItemInHand(hand), () -> {
            player.setDeltaMovement(player.getLookAngle().normalize().scale(1.7));
            player.fallDistance = 0;
            // The server pays first and explicitly updates the user, avoiding the old client-only boost.
            if (player instanceof ServerPlayer remote)
                remote.connection.send(new ClientboundSetEntityMotionPacket(player));
            return true;
        }) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
}
