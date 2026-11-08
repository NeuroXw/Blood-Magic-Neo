package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;

/** Five-slot sigil carrier with cycling and delegated use. GUI editing is deferred to later UI work. */
public final class ItemSigilHolding extends ItemSigilBase {
    public static final int SLOTS = 5;
    public ItemSigilHolding(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .component(BloodMagicDataComponents.SIGIL_SLOT.get(), 0), "holding", 0);
    }
    public static int selected(ItemStack holding) {
        return Math.clamp(holding.getOrDefault(BloodMagicDataComponents.SIGIL_SLOT.get(), 0), 0, SLOTS-1);
    }
    public static NonNullList<ItemStack> contents(ItemStack holding) {
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        holding.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(list);
        return list;
    }
    public static void save(ItemStack holding, NonNullList<ItemStack> list) {
        holding.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }
    public static ItemStack held(ItemStack holding) { return contents(holding).get(selected(holding)); }
    private static void cycle(ItemStack holding, Player player) {
        int next = (selected(holding)+1) % SLOTS;
        holding.set(BloodMagicDataComponents.SIGIL_SLOT.get(), next);
        ItemStack item = held(holding);
        player.sendOverlayMessage(Component.translatable("message.bloodmagic.holding.slot", next+1,
                item.isEmpty() ? Component.translatable("message.bloodmagic.holding.empty") : item.getHoverName()));
    }
    private static boolean insert(Player player, ItemStack holding) {
        ItemStack offered = player.getOffhandItem();
        if (!(offered.getItem() instanceof ItemSigilBase) || offered.getItem() instanceof ItemSigilHolding) return false;
        NonNullList<ItemStack> list = contents(holding);
        for (int i = 0; i < list.size(); i++) if (list.get(i).isEmpty()) {
            list.set(i, offered.copyWithCount(1)); save(holding, list);
            if (!player.hasInfiniteMaterials()) offered.shrink(1);
            return true;
        }
        return false;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.FAIL;
        ItemStack holding = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            if (!insert(player, holding)) cycle(holding, player);
            return InteractionResult.SUCCESS_SERVER;
        }
        ItemStack inner = held(holding);
        if (inner.isEmpty()) return InteractionResult.PASS;
        player.setItemInHand(hand, inner.copy());
        InteractionResult result;
        try { result = player.getItemInHand(hand).getItem().use(server, player, hand); }
        finally {
            ItemStack changed = player.getItemInHand(hand);
            if (!changed.isEmpty() && !changed.is(holding.getItem())) {
                NonNullList<ItemStack> list = contents(holding); list.set(selected(holding), changed.copy()); save(holding, list);
            }
            player.setItemInHand(hand, holding);
        }
        return result;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer(); ItemStack holding = context.getItemInHand();
        if (player == null || !canUse(player)) return InteractionResult.FAIL;
        if (!(context.getLevel() instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            if (!insert(player, holding)) cycle(holding, player);
            return InteractionResult.SUCCESS_SERVER;
        }
        ItemStack inner = held(holding);
        if (inner.isEmpty()) return InteractionResult.PASS;
        player.setItemInHand(context.getHand(), inner.copy());
        InteractionResult result;
        try { result = player.getItemInHand(context.getHand()).getItem().useOn(new UseOnContext(player, context.getHand(), new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), false))); }
        finally {
            ItemStack changed = player.getItemInHand(context.getHand());
            if (!changed.isEmpty() && !changed.is(holding.getItem())) {
                NonNullList<ItemStack> list = contents(holding); list.set(selected(holding), changed.copy()); save(holding, list);
            }
            player.setItemInHand(context.getHand(), holding);
        }
        return result;
    }
    @Override public void inventoryTick(ItemStack stack, ServerLevel level, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;
        ItemStack inner = held(stack);
        if (!inner.isEmpty()) inner.getItem().inventoryTick(inner, level, player, slot);
    }
}
