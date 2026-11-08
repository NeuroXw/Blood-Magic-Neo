package wayoftime.bloodmagic.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

/** Immutable replacement for the old mutable binding NBT compound. UUID is the identity. */
public record Binding(UUID ownerId, String ownerName) {
    public static final Codec<Binding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Binding::ownerId),
            Codec.string(0, 64).fieldOf("name").forGetter(Binding::ownerName)
    ).apply(instance, Binding::new));
    public static final StreamCodec<ByteBuf, Binding> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Binding::ownerId,
            ByteBufCodecs.stringUtf8(64), Binding::ownerName,
            Binding::new);

    public Binding {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(ownerName);
        if (ownerName.length() > 64) {
            throw new IllegalArgumentException("Binding name exceeds 64 characters");
        }
    }
}
