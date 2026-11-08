package wayoftime.bloodmagic.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.ritual.WorldAccess;

/**
 * Port of upstream ItemSacrificialDagger: one use costs 2 health (1 heart) and fills a nearby altar
 * with 200 LP before self-sacrifice rune bonuses. Search box matches PlayerSacrificeHelper.getAltar.
 * Deliberate port safety: no damage is dealt when no altar can accept the blood, and it never kills.
 */
public final class ItemSacrificialDagger extends Item {
    public ItemSacrificialDagger(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator() || player instanceof FakePlayer) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BloodAltarBlockEntity altar = findAltar(server, player.blockPosition());
        if (altar == null) return InteractionResult.FAIL;
        // sacrificeHealth validates range, structure, free capacity and health > 2 before any damage.
        if (altar.sacrificeHealth(player) <= 0) return InteractionResult.FAIL;
        float pitch = 2.6F + (server.getRandom().nextFloat() - server.getRandom().nextFloat()) * 0.8F;
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, pitch);
        server.sendParticles(DustParticleOptions.REDSTONE, player.getX(), player.getY() + 1, player.getZ(), 8, 0.5, 0.5, 0.5, 0);
        return InteractionResult.SUCCESS_SERVER;
    }

    public static BloodAltarBlockEntity findAltar(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 1; y++) for (int z = -2; z <= 2; z++) {
            BlockPos pos = origin.offset(x, y, z);
            if (WorldAccess.loaded(level, pos) && level.getBlockEntity(pos) instanceof BloodAltarBlockEntity altar && !altar.isRemoved()) return altar;
        }
        return null;
    }
}
