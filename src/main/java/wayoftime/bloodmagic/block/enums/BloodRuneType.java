package wayoftime.bloodmagic.block.enums;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum BloodRuneType implements StringRepresentable {
    BLANK,
    SPEED,
    EFFICIENCY,
    SACRIFICE,
    SELF_SACRIFICE,
    DISPLACEMENT,
    CAPACITY,
    AUGMENTED_CAPACITY,
    ORB,
    ACCELERATION,
    CHARGING;

    public static final EnumCodec<BloodRuneType> CODEC = StringRepresentable.fromEnum(BloodRuneType::values);

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return toString();
    }
}
