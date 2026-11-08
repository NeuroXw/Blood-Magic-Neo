package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.item.BloodMagicItems;

public final class ItemSigilFastMiner extends ItemSigilToggleableBase {
    public ItemSigilFastMiner(Properties properties) { super(properties, "fast_miner", 100); }
    @Override protected void onSigilUpdate(ItemStack stack, ServerLevel level, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 2, 0, true, false));
    }
    @EventBusSubscriber(modid = BloodMagic.MODID)
    public static final class Events {
        private Events() {}
        @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed event) {
            var player = event.getEntity();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(BloodMagicItems.MINING_SIGIL.get()) && ItemSigilToggleableBase.activated(stack)) {
                    event.setNewSpeed(event.getNewSpeed() * 3.0F);
                    return;
                }
            }
        }
    }
}
