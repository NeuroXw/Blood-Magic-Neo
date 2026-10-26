package wayoftime.bloodmagic.common.item;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.core.data.Binding;

/** Read/write examples used by ItemBloodOrb. Mutations belong on the server thread. */
public final class BloodOrbData {
    private BloodOrbData() {
    }

    public static int getTier(ItemStack stack) {
        return stack.getOrDefault(BloodMagicDataComponents.BLOOD_ORB_TIER.get(), 0);
    }

    public static void setTier(ItemStack stack, int tier) {
        requireOrb(stack);
        stack.set(BloodMagicDataComponents.BLOOD_ORB_TIER.get(), BloodMagicDataComponents.validateOrbTier(tier));
    }

    public static Optional<Binding> getBinding(ItemStack stack) {
        return Optional.ofNullable(stack.get(BloodMagicDataComponents.BINDING.get()));
    }

    public static void bind(ItemStack stack, UUID ownerId, String ownerName) {
        requireOrb(stack);
        stack.set(BloodMagicDataComponents.BINDING.get(), new Binding(ownerId, ownerName));
    }

    public static void unbind(ItemStack stack) {
        requireOrb(stack);
        stack.remove(BloodMagicDataComponents.BINDING.get());
    }

    private static void requireOrb(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBloodOrb)) {
            throw new IllegalArgumentException("Expected a blood orb stack");
        }
    }
}
