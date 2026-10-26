package wayoftime.bloodmagic.will;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.item.*;
import wayoftime.bloodmagic.common.registries.BloodMagicWill;
@EventBusSubscriber(modid = BloodMagic.MODID)
public final class WillDrops {
    private WillDrops() {}
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !mob.hasEffect(BloodMagicWill.SNARED)) return;
        var stack = new ItemStack(BloodMagicItems.RAW_WILL.get());
        ItemDemonWill.setAmount(stack, 1 + level.getRandom().nextDouble() * 4);
        event.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY(), mob.getZ(), stack));
    }
}
