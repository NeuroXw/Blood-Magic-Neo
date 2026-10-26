package wayoftime.bloodmagic.common.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;

public final class ItemBloodOrb extends ItemBase {
    public ItemBloodOrb(Properties properties, int tier) {
        super(properties.stacksTo(1).component(BloodMagicDataComponents.BLOOD_ORB_TIER.get(),
                BloodMagicDataComponents.validateOrbTier(tier)), "stage2.orb");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && BloodOrbData.getBinding(stack).isEmpty()) {
            BloodOrbData.bind(stack, player.getUUID(), player.getGameProfile().name());
        }
        // TODO: Follow-up - custom BloodOrb registry, soul-network LP, sacrifice and dynamic owner tooltip.
        return InteractionResult.SUCCESS;
    }
}
