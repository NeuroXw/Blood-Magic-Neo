package wayoftime.bloodmagic.will;

import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.item.ItemDemonWill;

/** Upstream WillHandler: auto-fill player gems when raw will is picked up. */
@EventBusSubscriber(modid = BloodMagic.MODID)
public final class WillPickupHandler {
    private WillPickupHandler() {}
    @SubscribeEvent
    public static void pickup(ItemEntityPickupEvent.Pre event) {
        ItemStack stack = event.getItemEntity().getItem();
        if (!(stack.getItem() instanceof ItemDemonWill will) || will.isGem()) return;
        var remainder = PlayerDemonWillHandler.addDemonWill(event.getPlayer(), stack);
        if (remainder.isEmpty() || ItemDemonWill.amount(remainder) < .0001) {
            event.getItemEntity().discard();
            event.setCanPickup(TriState.FALSE);
        }
    }
}
