package wayoftime.bloodmagic.api.compat;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Upstream api/compat/EnumDemonWillType; serialized names match upstream NBT ("default", "corrosive", ...). */
public enum EnumDemonWillType implements StringRepresentable {
    DEFAULT, CORROSIVE, DESTRUCTIVE, VENGEFUL, STEADFAST;
    public static final Codec<EnumDemonWillType> CODEC = StringRepresentable.fromEnum(EnumDemonWillType::values);
    public static final StreamCodec<ByteBuf, EnumDemonWillType> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            i -> values()[Math.floorMod(i, values().length)], Enum::ordinal);
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    @Override public String toString() { return getSerializedName(); }
    public static EnumDemonWillType getType(String type) {
        for (EnumDemonWillType t : values()) if (t.name().equalsIgnoreCase(type)) return t;
        return null;
    }
}
