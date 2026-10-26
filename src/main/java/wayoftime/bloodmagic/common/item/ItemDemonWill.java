package wayoftime.bloodmagic.common.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.will.DemonWill;

public final class ItemDemonWill extends Item {
    private final int capacity;
    private final boolean gem;
    public ItemDemonWill(Properties properties, int capacity, boolean gem) {
        super(properties.stacksTo(1).component(BloodMagicDataComponents.DEMON_WILL.get(), DemonWill.EMPTY));
        this.capacity = capacity; this.gem = gem;
    }
    public int capacity() { return capacity; }
    public boolean isGem() { return gem; }
    public static double amount(ItemStack stack) {
        return stack.getItem() instanceof ItemDemonWill item ? Math.min(item.capacity, stack.getOrDefault(BloodMagicDataComponents.DEMON_WILL.get(), DemonWill.EMPTY).amount()) : 0;
    }
    public static void setAmount(ItemStack stack, double amount) {
        if (!(stack.getItem() instanceof ItemDemonWill item) || !DemonWill.valid(amount) || amount > item.capacity) throw new IllegalArgumentException("Will exceeds item capacity");
        stack.set(BloodMagicDataComponents.DEMON_WILL.get(), new DemonWill(amount));
    }
    public static double transfer(ItemStack source, ItemStack target) {
        if (source == target || !(target.getItem() instanceof ItemDemonWill item) || !item.gem) return 0;
        double moved = Math.min(amount(source), item.capacity - amount(target));
        if (moved <= 0) return 0;
        setAmount(target, amount(target) + moved); setAmount(source, amount(source) - moved);
        if (source.getItem() instanceof ItemDemonWill from && !from.gem && amount(source) <= 0) source.shrink(1);
        return moved;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isAlive() || player.isSpectator()) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand); double moved = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack other = player.getInventory().getItem(i);
            if (other == held) continue;
            if (gem && other.getItem() instanceof ItemDemonWill raw && !raw.gem) moved += transfer(other, held);
            else if (!gem) moved += transfer(held, other);
        }
        if (moved > 0) { player.getInventory().setChanged(); return InteractionResult.SUCCESS_SERVER; }
        return InteractionResult.PASS;
    }
    @Override public boolean isBarVisible(ItemStack stack) { return gem; }
    @Override public int getBarWidth(ItemStack stack) { return (int) (13 * amount(stack) / capacity); }
    @Override public int getBarColor(ItemStack stack) { return 0x55BBD8; }
}
