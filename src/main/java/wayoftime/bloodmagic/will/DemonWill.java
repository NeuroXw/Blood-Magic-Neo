package wayoftime.bloodmagic.will;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Immutable raw Demon Will component; fractional amounts survive saves and network sync. */
public record DemonWill(double amount) {
    public static final DemonWill EMPTY = new DemonWill(0);
    public static final Codec<DemonWill> CODEC = Codec.DOUBLE.validate(n -> valid(n) ? DataResult.success(n)
            : DataResult.error(() -> "Will must be finite and nonnegative, at most 4096")).xmap(DemonWill::new, DemonWill::amount);
    public static final StreamCodec<RegistryFriendlyByteBuf, DemonWill> STREAM_CODEC = StreamCodec.of((b, w) -> b.writeDouble(w.amount), b -> new DemonWill(b.readDouble()));
    public static boolean valid(double value) { return Double.isFinite(value) && value >= 0 && value <= 4096; }
    public DemonWill { if (!valid(amount)) throw new IllegalArgumentException("Invalid demon will"); }
}
