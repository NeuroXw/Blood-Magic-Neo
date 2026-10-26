package wayoftime.bloodmagic.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.item.ItemBloodOrb;

/** Bind only explicit player insertions; network sync and automation cannot claim an orb. */
class OrbBindingSlot extends Slot {
    private final Player player;

    OrbBindingSlot(Container container, int index, int x, int y, Player player) {
        super(container, index, x, y);
        this.player = player;
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        ItemBloodOrb.bindForPlayer(player, stack);
        super.setByPlayer(stack, previous);
    }
}
