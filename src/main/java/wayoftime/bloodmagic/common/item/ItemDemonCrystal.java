package wayoftime.bloodmagic.common.item;

import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;

/** Upstream ItemDemonCrystal (IDiscreteDemonWill): each crystal is 50 will of one type. */
public final class ItemDemonCrystal extends ItemBase {
    public static final double DISCRETIZATION = 50;
    private final EnumDemonWillType type;
    public ItemDemonCrystal(Properties properties, EnumDemonWillType type) { super(properties); this.type = type; }
    public EnumDemonWillType type() { return type; }
    public static double getWill(ItemStack stack) { return stack.getItem() instanceof ItemDemonCrystal ? DISCRETIZATION * stack.getCount() : 0; }
    /** Removes whole crystals worth at most drainAmount; returns the drained will. */
    public static double drainWill(ItemStack stack, double drainAmount) {
        if (!(stack.getItem() instanceof ItemDemonCrystal)) return 0;
        int n = (int) Math.floor(Math.min(stack.getCount() * DISCRETIZATION, drainAmount) / DISCRETIZATION);
        if (n <= 0) return 0;
        stack.shrink(n);
        return n * DISCRETIZATION;
    }
}
