package wayoftime.bloodmagic.common.item.sigil;

import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.item.ItemBindable;
import wayoftime.bloodmagic.core.data.SoulNetworkPayment;

public abstract class ItemSigilBase extends ItemBindable {
    private final int lpCost;

    protected ItemSigilBase(Properties properties, String name, int lpCost) {
        super(properties, "sigil." + name + ".desc");
        this.lpCost = lpCost;
    }

    public int getLpUsed() { return lpCost; }

    protected boolean activate(ServerLevel level, Player player, ItemStack stack, BooleanSupplier effect) {
        return SoulNetworkPayment.perform(level, player, bindIfAbsent(player, stack), lpCost, effect);
    }
}
