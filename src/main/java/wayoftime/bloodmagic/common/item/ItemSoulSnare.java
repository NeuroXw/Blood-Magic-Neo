package wayoftime.bloodmagic.common.item;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.entity.SoulSnare;
public final class ItemSoulSnare extends Item {
    public ItemSoulSnare(Properties p) { super(p.stacksTo(16)); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isAlive() || player.isSpectator()) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
        var stack = player.getItemInHand(hand); var snare = new SoulSnare(level, player, stack);
        snare.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5F, 1);
        if (!level.addFreshEntity(snare)) return InteractionResult.FAIL;
        if (!player.hasInfiniteMaterials()) stack.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
