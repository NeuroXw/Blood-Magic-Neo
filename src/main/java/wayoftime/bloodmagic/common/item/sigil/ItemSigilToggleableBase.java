package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;

/** Upstream toggleable sigil: sneak-use toggles, activated sigils pay upkeep every 100 ticks. */
public abstract class ItemSigilToggleableBase extends ItemSigilBase {
    protected ItemSigilToggleableBase(Properties properties, String name, int lpCost) {
        super(properties.stacksTo(1).component(BloodMagicDataComponents.SIGIL_ACTIVATED.get(), false), name, lpCost);
    }
    public static boolean activated(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(BloodMagicDataComponents.SIGIL_ACTIVATED.get(), false);
    }
    protected static void setActivated(ItemStack stack, boolean value) {
        stack.set(BloodMagicDataComponents.SIGIL_ACTIVATED.get(), value);
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.FAIL;
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            boolean next = !activated(stack);
            if (next) {
                bindIfAbsent(player, stack);
                // Charge the first upkeep period at activation. This prevents a free
                // interval when the bound network is empty.
                if (!activate(server, player, stack, () -> true)) return InteractionResult.FAIL;
            }
            setActivated(stack, next);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "tooltip.bloodmagic." + (next ? "activated" : "deactivated")));
            return InteractionResult.SUCCESS_SERVER;
        }
        return activated(stack) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }
    @Override public void inventoryTick(ItemStack stack, ServerLevel level, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot) {
        if (!(entity instanceof Player player) || !canUse(player) || !activated(stack)) return;
        if (entity.tickCount % 100 == 0 && !activate(level, player, stack, () -> true)) {
            setActivated(stack, false);
            return;
        }
        onSigilUpdate(stack, level, player);
    }
    protected abstract void onSigilUpdate(ItemStack stack, ServerLevel level, Player player);
}
