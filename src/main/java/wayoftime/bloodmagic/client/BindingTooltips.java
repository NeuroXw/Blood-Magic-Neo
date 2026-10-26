package wayoftime.bloodmagic.client;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.item.ItemBindable;
import wayoftime.bloodmagic.common.item.ItemBloodOrb;
import wayoftime.bloodmagic.common.item.BloodOrbData;
import wayoftime.bloodmagic.common.item.sigil.ItemSigilBase;
import wayoftime.bloodmagic.common.registries.BloodMagicBloodOrbs;

/** Uses the client tooltip event; avoids deprecated Item.appendHoverText in 26.1.2. */
@EventBusSubscriber(modid = BloodMagic.MODID, value = Dist.CLIENT)
public final class BindingTooltips {
    private BindingTooltips() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.getItem() instanceof wayoftime.bloodmagic.common.item.ItemRitualDiviner diviner) {
            event.getToolTip().add(Component.translatable("tooltip.bloodmagic.diviner"));
            event.getToolTip().add(Component.translatable("message.bloodmagic.ritual.selected", Component.translatable("ritual.bloodmagic." + diviner.selected(stack).getSerializedName())));
        }
        if (stack.getItem() instanceof wayoftime.bloodmagic.common.item.ItemArcaneAshes) event.getToolTip().add(Component.translatable("tooltip.bloodmagic.ashes"));
        if (stack.getItem() instanceof wayoftime.bloodmagic.common.item.ItemDemonWill will) {
            event.getToolTip().add(Component.translatable("tooltip.bloodmagic.demon_will", String.format(java.util.Locale.ROOT, "%.2f", wayoftime.bloodmagic.common.item.ItemDemonWill.amount(stack)), will.capacity()));
        }
        if (!(stack.getItem() instanceof ItemBindable)) return;
        var owner = ItemBindable.binding(stack);
        event.getToolTip().add(owner == null ? Component.translatable("tooltip.bloodmagic.unbound")
                : Component.translatable("tooltip.bloodmagic.currentOwner", owner.ownerName()));
        if (stack.getItem() instanceof ItemBloodOrb) {
            var orb = BloodMagicBloodOrbs.forTier(BloodOrbData.getTier(stack));
            event.getToolTip().add(Component.translatable("tooltip.bloodmagic.orb.stats", orb.tier(), orb.capacity(), orb.fillRate()));
        } else if (stack.getItem() instanceof ItemSigilBase sigil) {
            event.getToolTip().add(Component.translatable("tooltip.bloodmagic.sigil.cost", sigil.getLpUsed()));
        }
    }
}
