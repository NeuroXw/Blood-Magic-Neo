package wayoftime.bloodmagic.altar;

import java.util.Map;
import wayoftime.bloodmagic.block.enums.BloodRuneType;

/** Rune strengths, not just block counts. Formulae match upstream BloodAltar.checkTier. */
public record AltarUpgrades(Map<BloodRuneType, Integer> levels) {
    public static final AltarUpgrades EMPTY = new AltarUpgrades(Map.of());

    public AltarUpgrades {
        levels = Map.copyOf(levels);
    }

    public int level(BloodRuneType rune) {
        return Math.max(0, levels.getOrDefault(rune, 0));
    }

    public double speedMultiplier() {
        return 1 + 0.20 * level(BloodRuneType.SPEED);
    }

    public double capacityMultiplier() {
        return (1 + 0.20 * level(BloodRuneType.CAPACITY)) * Math.pow(1.075, level(BloodRuneType.AUGMENTED_CAPACITY));
    }

    public int capacity() {
        return scale(AltarState.BASE_CAPACITY, capacityMultiplier());
    }

    public int bufferCapacity() {
        return scale(AltarState.BASE_BUFFER_CAPACITY, capacityMultiplier());
    }

    public int consumption(int base) {
        return scale(base, speedMultiplier());
    }

    public int decay(int base) {
        return scale(base, Math.pow(0.85, level(BloodRuneType.EFFICIENCY)));
    }

    public int sacrifice(int base, boolean selfSacrifice) {
        return scale(base, 1 + 0.10 * level(selfSacrifice ? BloodRuneType.SELF_SACRIFICE : BloodRuneType.SACRIFICE));
    }

    private static int scale(int base, double multiplier) {
        // Float multiplication matches upstream and avoids e.g. 1504.9999999999998 truncating to 1504.
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, base) * (float) multiplier);
    }
}
