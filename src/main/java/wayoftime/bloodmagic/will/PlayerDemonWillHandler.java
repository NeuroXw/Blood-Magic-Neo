package wayoftime.bloodmagic.will;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.item.ItemDemonWill;

/** Upstream will/PlayerDemonWillHandler over the whole player inventory (main, armor, offhand). */
public final class PlayerDemonWillHandler {
    private PlayerDemonWillHandler() {}
    private static java.util.List<ItemStack> inventory(Player player) {
        var inv = player.getInventory(); var list = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < inv.getContainerSize(); i++) list.add(inv.getItem(i));
        return list;
    }
    public static double getTotalDemonWill(EnumDemonWillType type, Player player) {
        double souls = 0;
        for (ItemStack stack : inventory(player)) souls += ItemDemonWill.amount(stack, type);
        return souls;
    }
    public static EnumDemonWillType getLargestWillType(Player player) {
        EnumDemonWillType best = EnumDemonWillType.DEFAULT; double max = getTotalDemonWill(best, player);
        for (var t : EnumDemonWillType.values()) { double v = getTotalDemonWill(t, player); if (v > max) { max = v; best = t; } }
        return best;
    }
    public static boolean isDemonWillFull(EnumDemonWillType type, Player player) {
        boolean hasGem = false;
        for (ItemStack stack : inventory(player)) if (stack.getItem() instanceof ItemDemonWill w && w.isGem()) {
            hasGem = true;
            if (ItemDemonWill.fill(stack, type, Double.MAX_VALUE, false) > 0) return false;
        }
        return hasGem;
    }
    public static double consumeDemonWill(EnumDemonWillType type, Player player, double amount) {
        double consumed = 0;
        for (ItemStack stack : inventory(player)) {
            if (consumed >= amount) break;
            consumed += ItemDemonWill.drain(stack, type, amount - consumed, true);
            if (stack.getItem() instanceof ItemDemonWill w && !w.isGem() && ItemDemonWill.amount(stack) <= 0) stack.shrink(1);
        }
        if (consumed > 0) player.getInventory().setChanged();
        return consumed;
    }
    /** Pours a will stack into the player's gems; returns the (possibly emptied) stack. */
    public static ItemStack addDemonWill(Player player, ItemStack willStack) {
        if (willStack.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack stack : inventory(player)) {
            if (stack == willStack) continue;
            ItemDemonWill.transfer(willStack, stack);
            if (willStack.isEmpty() || ItemDemonWill.amount(willStack) <= 0) { player.getInventory().setChanged(); return ItemStack.EMPTY; }
        }
        return willStack;
    }
    public static double addDemonWill(EnumDemonWillType type, Player player, double amount, ItemStack ignored) {
        double remaining = amount;
        for (ItemStack stack : inventory(player)) {
            if (stack == ignored) continue;
            remaining -= ItemDemonWill.fill(stack, type, remaining, true);
            if (remaining <= 0) break;
        }
        return amount - remaining;
    }
}
