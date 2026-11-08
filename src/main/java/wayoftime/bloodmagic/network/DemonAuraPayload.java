package wayoftime.bloodmagic.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.will.DemonWillHolder;

/** Upstream DemonAuraClientPacket: the five aura values of the player's chunk. */
public record DemonAuraPayload(double[] values) implements CustomPacketPayload {
    public static final Type<DemonAuraPayload> TYPE = new Type<>(BloodMagic.rl("demon_aura"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DemonAuraPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { for (double v : p.values) b.writeDouble(v); },
            b -> { double[] v = new double[EnumDemonWillType.values().length]; for (int i = 0; i < v.length; i++) v[i] = b.readDouble(); return new DemonAuraPayload(v); });
    public static DemonAuraPayload of(DemonWillHolder holder) {
        double[] v = new double[EnumDemonWillType.values().length];
        if (holder != null) for (var t : EnumDemonWillType.values()) v[t.ordinal()] = holder.getWill(t);
        return new DemonAuraPayload(v);
    }
    public DemonWillHolder toHolder() {
        var h = new DemonWillHolder();
        for (var t : EnumDemonWillType.values()) { double v = values[t.ordinal()]; if (Double.isFinite(v) && v > 0) h.addWill(t, v); }
        return h;
    }
    @Override public Type<DemonAuraPayload> type() { return TYPE; }
}
