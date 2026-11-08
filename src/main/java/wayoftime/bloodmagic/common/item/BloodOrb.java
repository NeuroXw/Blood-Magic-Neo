package wayoftime.bloodmagic.common.item;

/** Immutable orb definition; capacities and per-tick rates match upstream 1.20.1. */
public record BloodOrb(int tier, int capacity, int fillRate) {
    public BloodOrb {
        if (tier < 1 || tier > 5 || capacity <= 0 || fillRate <= 0)
            throw new IllegalArgumentException("Invalid blood orb definition");
    }
}
