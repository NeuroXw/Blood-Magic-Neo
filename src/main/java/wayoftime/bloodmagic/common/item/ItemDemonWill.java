package wayoftime.bloodmagic.common.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.will.DemonWill;

/**
 * Upstream ItemMonsterSoul (gem=false, fixed type) and ItemSoulGem (gem=true, type stored in the
 * will_type component, switching with its contents). Amount lives in the existing demon_will component,
 * so pre-typing saves read as DEFAULT will.
 */
public final class ItemDemonWill extends Item {
    private final int capacity;
    private final boolean gem;
    private final EnumDemonWillType soulType;
    public ItemDemonWill(Properties properties, int capacity, boolean gem) { this(properties, capacity, gem, EnumDemonWillType.DEFAULT); }
    public ItemDemonWill(Properties properties, int capacity, boolean gem, EnumDemonWillType soulType) {
        super(properties.stacksTo(1).component(BloodMagicDataComponents.DEMON_WILL.get(), DemonWill.EMPTY));
        this.capacity = capacity; this.gem = gem; this.soulType = soulType;
    }
    public int capacity() { return capacity; }
    public boolean isGem() { return gem; }
    public static EnumDemonWillType type(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemDemonWill item)) return EnumDemonWillType.DEFAULT;
        return item.gem ? stack.getOrDefault(BloodMagicDataComponents.WILL_TYPE.get(), EnumDemonWillType.DEFAULT) : item.soulType;
    }
    public static double amount(ItemStack stack) {
        return stack.getItem() instanceof ItemDemonWill item ? Math.min(item.capacity, stack.getOrDefault(BloodMagicDataComponents.DEMON_WILL.get(), DemonWill.EMPTY).amount()) : 0;
    }
    public static double amount(ItemStack stack, EnumDemonWillType type) { return type(stack) == type ? amount(stack) : 0; }
    public static void setAmount(ItemStack stack, double amount) {
        if (!(stack.getItem() instanceof ItemDemonWill item) || !DemonWill.valid(amount) || amount > item.capacity) throw new IllegalArgumentException("Will exceeds item capacity");
        stack.set(BloodMagicDataComponents.DEMON_WILL.get(), new DemonWill(amount));
        if (item.gem && amount <= 0) stack.remove(BloodMagicDataComponents.WILL_TYPE.get());
    }
    /** Upstream setWill: gems adopt the type, souls only accept their own type. */
    public static void setWill(ItemStack stack, EnumDemonWillType type, double amount) {
        if (!(stack.getItem() instanceof ItemDemonWill item)) throw new IllegalArgumentException("Not a will item");
        if (item.gem) {
            if (type == EnumDemonWillType.DEFAULT || amount <= 0) stack.remove(BloodMagicDataComponents.WILL_TYPE.get());
            else stack.set(BloodMagicDataComponents.WILL_TYPE.get(), type);
        } else if (type != item.soulType) throw new IllegalArgumentException("Soul type mismatch");
        setAmount(stack, amount);
    }
    public static ItemStack createWill(Item item, double amount) {
        var stack = new ItemStack(item);
        if (item instanceof ItemDemonWill will) setAmount(stack, Math.clamp(amount, 0, will.capacity));
        return stack;
    }
    /** Upstream IDemonWillGem.fillWill. A gem holding another type accepts nothing. */
    public static double fill(ItemStack gemStack, EnumDemonWillType type, double amount, boolean doFill) {
        if (!(gemStack.getItem() instanceof ItemDemonWill item) || !item.gem || !(amount > 0)) return 0;
        double current = amount(gemStack);
        if (current > 0 && type(gemStack) != type) return 0;
        double filled = Math.min(amount, item.capacity - current);
        if (filled > 0 && doFill) setWill(gemStack, type, current + filled);
        return Math.max(0, filled);
    }
    public static double drain(ItemStack stack, EnumDemonWillType type, double amount, boolean doDrain) {
        if (!(stack.getItem() instanceof ItemDemonWill) || type(stack) != type || !(amount > 0)) return 0;
        double current = amount(stack), drained = Math.min(current, amount);
        if (doDrain && drained > 0) setAmount(stack, current - drained);
        return drained;
    }
    /** Moves will from source into a gem of the same (or no) type; empty raw souls are consumed. */
    public static double transfer(ItemStack source, ItemStack target) {
        if (source == target || !(source.getItem() instanceof ItemDemonWill) || !(target.getItem() instanceof ItemDemonWill item) || !item.gem) return 0;
        EnumDemonWillType t = type(source);
        double moved = fill(target, t, amount(source), false);
        if (moved <= 0) return 0;
        fill(target, t, moved, true);
        setAmount(source, amount(source) - moved);
        if (source.getItem() instanceof ItemDemonWill from && !from.gem && amount(source) <= 0) source.shrink(1);
        return moved;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isAlive() || player.isSpectator()) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand); double moved = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack other = player.getInventory().getItem(i);
            if (other == held || !(other.getItem() instanceof ItemDemonWill target) || !target.gem) continue;
            if (gem) {
                EnumDemonWillType heldType = type(held);
                double transfer = fill(other, heldType, amount(held), false);
                transfer = drain(held, heldType, transfer, true);
                moved += fill(other, heldType, transfer, true);
            } else moved += transfer(held, other);
        }
        if (moved > 0) { player.getInventory().setChanged(); return InteractionResult.SUCCESS_SERVER; }
        return InteractionResult.PASS;
    }
    /** 26.1 dispatches held-item block interactions through useOn, not use. */
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand());
    }
    @Override public boolean isBarVisible(ItemStack stack) { return gem; }
    @Override public int getBarWidth(ItemStack stack) { return (int) (13 * amount(stack) / capacity); }
    @Override public int getBarColor(ItemStack stack) { return net.minecraft.util.Mth.hsvToRgb((float) Math.max(0, amount(stack) / capacity) / 3F, 1F, 1F); }
}
