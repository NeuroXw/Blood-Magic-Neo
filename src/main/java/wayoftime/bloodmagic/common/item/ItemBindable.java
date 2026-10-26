package wayoftime.bloodmagic.common.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.core.data.Binding;

/** Binding lives in the existing persistent/networked data component, not legacy item NBT. */
public abstract class ItemBindable extends ItemBase {
    protected ItemBindable(Properties properties, String description) {
        super(properties.stacksTo(1), description);
    }

    protected static boolean canUse(Player player) {
        return player != null && player.isAlive() && !player.isSpectator()
                && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer);
    }

    public static Binding binding(ItemStack stack) { return stack.get(BloodMagicDataComponents.BINDING.get()); }

    protected static Binding bindIfAbsent(Player player, ItemStack stack) {
        Binding binding = binding(stack);
        if (binding == null && player.level() instanceof ServerLevel && canUse(player)) {
            binding = new Binding(player.getUUID(), player.getGameProfile().name());
            stack.set(BloodMagicDataComponents.BINDING.get(), binding);
        }
        return binding;
    }
}
