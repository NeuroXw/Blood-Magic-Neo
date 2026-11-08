package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Upstream suppression sigil. Spectral support blocks are not in stage 2, so this clears nearby mob targets. */
public final class ItemSigilSuppression extends ItemSigilToggleableBase {
    public ItemSigilSuppression(Properties properties) { super(properties, "suppression", 400); }
    @Override protected void onSigilUpdate(ItemStack stack, ServerLevel level, Player player) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(5))) {
            if (mob.getTarget() == player) mob.setTarget(null);
        }
    }
}
