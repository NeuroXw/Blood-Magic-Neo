package wayoftime.bloodmagic.common.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import net.minecraft.server.level.ServerLevel;
import wayoftime.bloodmagic.common.registries.BloodMagicBloodOrbs;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.core.data.SoulNetworks;
import wayoftime.bloodmagic.core.data.SoulNetworkPayment;

public final class ItemBloodOrb extends ItemBindable {
    private final int defaultTier;

    public ItemBloodOrb(Properties properties, int tier) {
        super(properties.stacksTo(1).component(BloodMagicDataComponents.BLOOD_ORB_TIER.get(),
                BloodMagicDataComponents.validateOrbTier(tier)), "orb.desc");
        defaultTier = tier;
    }

    public int defaultTier() { return defaultTier; }

    /** A player inserting an orb can bind it without paying health or replacing an existing owner. */
    public static void bindForPlayer(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBloodOrb) || !canUse(player)
                || !(player.level() instanceof ServerLevel server)) return;
        Binding owner = bindIfAbsent(player, stack);
        if (owner.ownerId().equals(player.getUUID())) {
            SoulNetworks.get(server).recordOrb(owner.ownerId(), BloodOrbData.getTier(stack));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        bindForPlayer(player, stack);
        Binding owner = binding(stack);
        BloodOrb orb = BloodMagicBloodOrbs.forTier(BloodOrbData.getTier(stack));
        SoulNetworks networks = SoulNetworks.get(server);
        int room = Math.max(0, orb.capacity() - networks.get(owner.ownerId()).currentEssence());
        if (room == 0) return InteractionResult.SUCCESS_SERVER;
        int paid = SoulNetworkPayment.sacrifice(server, player, Math.min(200, room));
        networks.fill(owner.ownerId(), paid, orb.capacity());
        return paid > 0 ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    @Override
    public net.minecraft.world.item.ItemStackTemplate getCraftingRemainder(net.minecraft.world.item.ItemInstance stack) {
        return switch (stack) {
            case ItemStack live -> net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(live).withCount(1);
            case net.minecraft.world.item.ItemStackTemplate template -> template.withCount(1);
            default -> throw new IllegalArgumentException("Unsupported orb ItemInstance: " + stack.getClass());
        };
    }
}
