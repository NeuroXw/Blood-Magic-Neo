package wayoftime.bloodmagic.core.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import wayoftime.bloodmagic.BloodMagic;

public final class SoulNetworkPayment {
    public static final ResourceKey<DamageType> SACRIFICE = ResourceKey.create(Registries.DAMAGE_TYPE, BloodMagic.rl("sacrifice"));
    private SoulNetworkPayment() {}

    /** Upstream fallback charges the user the entire cost, leaving insufficient network LP alone. */
    public static boolean pay(ServerLevel level, Player user, Binding owner, int cost) {
        if (owner == null || user == null || cost < 0 || user.level() != level || !user.isAlive() || user.isSpectator()) return false;
        if (user.hasInfiniteMaterials() || cost == 0) return true;
        if (SoulNetworks.get(level).consume(owner.ownerId(), cost)) return true;
        return sacrifice(level, user, cost) >= cost;
    }

    /** Refund a failed world operation; no free placement and no LP charge on a rejected operation. */
    public static boolean perform(ServerLevel level, Player user, Binding owner, int cost,
            java.util.function.BooleanSupplier effect) {
        if (owner == null || user == null || user.level() != level || !user.isAlive() || user.isSpectator()) return false;
        SoulNetworks networks = SoulNetworks.get(level);
        int beforeLP = networks.get(owner.ownerId()).currentEssence();
        float beforeHealth = user.getHealth();
        float beforeAbsorption = user.getAbsorptionAmount();
        if (!pay(level, user, owner, cost)) return false;
        int spentLP = Math.max(0, beforeLP - networks.get(owner.ownerId()).currentEssence());
        float spentHealth = Math.max(0, beforeHealth - user.getHealth());
        float spentAbsorption = Math.max(0, beforeAbsorption - user.getAbsorptionAmount());
        boolean success = false;
        try {
            success = effect.getAsBoolean();
            return success;
        } finally {
            if (!success) {
                networks.fill(owner.ownerId(), spentLP, Integer.MAX_VALUE);
                if (spentHealth > 0) user.setHealth(user.getHealth() + spentHealth);
                if (spentAbsorption > 0) user.setAbsorptionAmount(user.getAbsorptionAmount() + spentAbsorption);
            }
        }
    }

    /** Returns actually paid LP. Rejected damage cannot create LP or enable a free effect. */
    public static int sacrifice(ServerLevel level, Player user, int lp) {
        if (lp <= 0 || user == null || user.level() != level || !user.isAlive() || user.isSpectator()) return 0;
        if (user.hasInfiniteMaterials()) return lp;
        float before = user.getHealth() + user.getAbsorptionAmount();
        float hearts = (float) Math.ceil(lp / 100.0);
        // Deliberate port safety: do not kill the user to fund an item action.
        if (before <= hearts) return 0;
        var source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SACRIFICE));
        if (!user.hurtServer(level, source, hearts)) return 0;
        float paid = Math.max(0, before - user.getHealth() - user.getAbsorptionAmount());
        return Math.min(lp, Math.round(paid * 100));
    }
}
