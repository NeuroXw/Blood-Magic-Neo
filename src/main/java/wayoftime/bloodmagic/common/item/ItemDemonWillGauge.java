package wayoftime.bloodmagic.common.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;
import wayoftime.bloodmagic.network.DemonAuraPayload;

/** Upstream ItemDemonWillGauge (IDemonWillViewer, resolution 100). Sends the chunk aura every 50 ticks. */
public final class ItemDemonWillGauge extends ItemBase {
    public static final int RESOLUTION = 100;
    public ItemDemonWillGauge(Properties p) { super(p.stacksTo(1), "willGauge"); }
    @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof ServerPlayer player && entity.tickCount % 50 == 0) sendAura(player);
    }
    public static void sendAura(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, DemonAuraPayload.of(WorldDemonWillHandler.getWillHolder(player.level(), player.blockPosition())));
    }
    /** Upstream Utils.canPlayerSeeDemonWill: active inventories are the two hands. */
    public static boolean canSee(Player player) {
        if (player == null) return false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).getItem() instanceof ItemDemonWillGauge) return true;
        return false;
    }
}
