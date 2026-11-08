package wayoftime.bloodmagic.will;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;

/** Upstream will/DemonWillHolder. Mutable; owners must mark themselves dirty after changes. */
public final class DemonWillHolder {
    public static final Codec<DemonWillHolder> CODEC = Codec.unboundedMap(EnumDemonWillType.CODEC, Codec.DOUBLE)
            .xmap(DemonWillHolder::new, h -> Map.copyOf(h.willMap));
    public final EnumMap<EnumDemonWillType, Double> willMap = new EnumMap<>(EnumDemonWillType.class);
    public DemonWillHolder() {}
    private DemonWillHolder(Map<EnumDemonWillType, Double> values) {
        values.forEach((k, v) -> { if (Double.isFinite(v) && v > 0) willMap.put(k, v); });
    }
    public DemonWillHolder copy() { return new DemonWillHolder(willMap); }
    /** Returns the amount that fits below max; upstream still adds the full amount. Kept bounded here. */
    public double addWill(EnumDemonWillType type, double amount, double max) {
        double added = Math.max(0, Math.min(max - getWill(type), amount));
        addWill(type, added);
        return added;
    }
    public void addWill(EnumDemonWillType type, double amount) {
        if (!(amount > 0) || !Double.isFinite(amount)) return;
        willMap.merge(type, amount, Double::sum);
    }
    public double drainWill(EnumDemonWillType type, double amount) {
        double current = getWill(type);
        double reduced = Math.max(0, Math.min(current, amount));
        if (reduced >= current) willMap.remove(type); else willMap.put(type, current - reduced);
        return reduced;
    }
    public double getWill(EnumDemonWillType type) { return willMap.getOrDefault(type, 0.0); }
    public boolean isEmpty() { return willMap.isEmpty(); }
    public void clearWill() { willMap.clear(); }
}
