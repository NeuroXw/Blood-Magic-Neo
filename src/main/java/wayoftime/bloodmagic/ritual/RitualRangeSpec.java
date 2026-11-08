package wayoftime.bloodmagic.ritual;

/** Immutable defaults and limits for a configurable ritual area. */
public record RitualRangeSpec(String name, RitualArea area, int maxVolume, int maxVertical, int maxHorizontal) {
}
