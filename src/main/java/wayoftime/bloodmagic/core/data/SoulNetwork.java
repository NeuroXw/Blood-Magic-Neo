package wayoftime.bloodmagic.core.data;

import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

/** Read-only snapshot: mutations go through SoulNetworks so every change is saved. */
public record SoulNetwork(UUID ownerId, int currentEssence, int orbTier) {
    public static final Codec<SoulNetwork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(SoulNetwork::ownerId),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("lp").forGetter(SoulNetwork::currentEssence),
            Codec.intRange(0, 5).fieldOf("orb_tier").forGetter(SoulNetwork::orbTier)
    ).apply(instance, SoulNetwork::new));

    public SoulNetwork {
        java.util.Objects.requireNonNull(ownerId);
        if (currentEssence < 0 || orbTier < 0 || orbTier > 5) throw new IllegalArgumentException("Invalid soul network");
    }
}
