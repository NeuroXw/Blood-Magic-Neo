package wayoftime.bloodmagic.common.item.sigil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class ItemSigilMagnetism extends ItemSigilToggleableBase {
    public ItemSigilMagnetism(Properties properties) { super(properties, "magnetism", 50); }
    @Override protected void onSigilUpdate(ItemStack stack, ServerLevel level, Player player) {
        AABB area = player.getBoundingBox().inflate(5, 5, 5);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) if (item.isAlive()) item.playerTouch(player);
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area)) if (orb.isAlive()) orb.playerTouch(player);
    }
}
