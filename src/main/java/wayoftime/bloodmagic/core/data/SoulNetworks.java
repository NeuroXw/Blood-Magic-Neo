package wayoftime.bloodmagic.core.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import wayoftime.bloodmagic.BloodMagic;

/** Server-thread-only, world-owned storage. No static player/world cache; offline owners work too. */
public final class SoulNetworks extends SavedData {
    public static final Codec<SoulNetworks> CODEC = SoulNetwork.CODEC.listOf().fieldOf("networks").codec()
            .xmap(SoulNetworks::new, data -> List.copyOf(data.networks.values()));
    // 26.1.2 uses Identifier + Codec SavedDataType, not the old CompoundTag save override.
    public static final SavedDataType<SoulNetworks> TYPE = new SavedDataType<>(BloodMagic.rl("soul_networks"),
            () -> new SoulNetworks(List.of()), CODEC);
    private final Map<UUID, SoulNetwork> networks = new HashMap<>();

    private SoulNetworks(List<SoulNetwork> entries) {
        for (SoulNetwork entry : entries) networks.put(entry.ownerId(), entry);
    }

    public static SoulNetworks get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public SoulNetwork get(UUID owner) {
        return networks.getOrDefault(owner, new SoulNetwork(owner, 0, 0));
    }

    public void recordOrb(UUID owner, int tier) {
        SoulNetwork old = get(owner);
        int next = Math.max(old.orbTier(), Math.clamp(tier, 0, 5));
        if (next != old.orbTier()) put(new SoulNetwork(owner, old.currentEssence(), next));
    }

    /** Lower-capacity orbs never delete an existing larger balance; arithmetic cannot overflow. */
    public int fill(UUID owner, int amount, int capacity) {
        SoulNetwork old = get(owner);
        int accepted = (int) Math.min(Math.max(0, amount), Math.max(0L, (long) capacity - old.currentEssence()));
        if (accepted > 0) put(new SoulNetwork(owner, old.currentEssence() + accepted, old.orbTier()));
        return accepted;
    }

    /** All or nothing, matching upstream syphon. A failed payment leaves the balance intact. */
    public boolean consume(UUID owner, int amount) {
        SoulNetwork old = get(owner);
        if (amount < 0 || old.currentEssence() < amount) return false;
        if (amount > 0) put(new SoulNetwork(owner, old.currentEssence() - amount, old.orbTier()));
        return true;
    }

    private void put(SoulNetwork network) {
        networks.put(network.ownerId(), network);
        setDirty();
    }
}
