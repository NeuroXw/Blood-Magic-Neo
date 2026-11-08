package wayoftime.bloodmagic.demonaura;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.registries.BloodMagicAttachments;
import wayoftime.bloodmagic.ritual.WorldAccess;
import wayoftime.bloodmagic.will.DemonWillHolder;

/** Upstream WorldDemonWillHandler API over the chunk attachment. Never loads absent chunks. */
public final class WorldDemonWillHandler {
    private WorldDemonWillHandler() {}
    public static DemonWillHolder getWillHolder(Level level, BlockPos pos) {
        if (level == null || level.isClientSide() || !WorldAccess.loaded(level, pos)) return null;
        return level.getChunkAt(pos).getData(BloodMagicAttachments.CHUNK_WILL);
    }
    private static void dirty(Level level, BlockPos pos) { level.getChunkAt(pos).markUnsaved(); }
    public static double getCurrentWill(Level level, BlockPos pos, EnumDemonWillType type) {
        var holder = getWillHolder(level, pos); return holder == null ? 0 : holder.getWill(type);
    }
    public static EnumDemonWillType getHighestDemonWillType(Level level, BlockPos pos) {
        var holder = getWillHolder(level, pos);
        EnumDemonWillType best = EnumDemonWillType.DEFAULT; double max = 0;
        if (holder != null) for (var t : EnumDemonWillType.values()) if (holder.getWill(t) > max) { max = holder.getWill(t); best = t; }
        return best;
    }
    public static double drainWill(Level level, BlockPos pos, EnumDemonWillType type, double amount, boolean doDrain) {
        var holder = getWillHolder(level, pos);
        if (holder == null || !(amount > 0)) return 0;
        double drained = Math.min(holder.getWill(type), amount);
        if (doDrain && drained > 0) { holder.drainWill(type, drained); dirty(level, pos); }
        return drained;
    }
    public static double fillWillToMaximum(Level level, BlockPos pos, EnumDemonWillType type, double amount, double max, boolean doFill) {
        var holder = getWillHolder(level, pos);
        if (holder == null) return 0;
        double fill = Math.min(max - holder.getWill(type), amount);
        if (!(fill > 0)) return 0;
        if (doFill) { holder.addWill(type, fill); dirty(level, pos); }
        return fill;
    }
    public static double fillWill(Level level, BlockPos pos, EnumDemonWillType type, double amount, boolean doFill) {
        var holder = getWillHolder(level, pos);
        if (holder == null || !(amount > 0)) return 0;
        if (doFill) { holder.addWill(type, amount); dirty(level, pos); }
        return amount;
    }
}
