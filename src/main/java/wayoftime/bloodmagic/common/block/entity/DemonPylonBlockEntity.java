package wayoftime.bloodmagic.common.block.entity;

import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.api.compat.IDemonWillConduit;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;

/** Upstream pylon pulls at most one will/tick from higher-aura points 16 blocks away. */
public final class DemonPylonBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements IDemonWillConduit {
    private final EnumMap<EnumDemonWillType, Double> stored = new EnumMap<>(EnumDemonWillType.class);
    public DemonPylonBlockEntity(BlockPos pos, BlockState state) { super(BloodMagicBlockEntities.DEMON_PYLON.get(), pos, state); }
    public double will(EnumDemonWillType type) { return stored.getOrDefault(type, 0.0); }
    @Override public int getWeight() { return 10; }
    @Override public boolean canFill(EnumDemonWillType type) { return true; }
    @Override public boolean canDrain(EnumDemonWillType type) { return true; }
    @Override public double getCurrentWill(EnumDemonWillType type) { return will(type); }
    @Override public double fillDemonWill(EnumDemonWillType type, double amount, boolean doFill) {
        double accepted = Math.max(0, Math.min(100 - will(type), amount));
        if (doFill && accepted > 0) { stored.merge(type, accepted, Double::sum); setChanged(); }
        return accepted;
    }
    @Override public double drainDemonWill(EnumDemonWillType type, double amount, boolean doDrain) {
        double drained = Math.max(0, Math.min(will(type), amount));
        if (doDrain && drained > 0) { double left = will(type) - drained; if (left < 1e-9) stored.remove(type); else stored.put(type, left); setChanged(); }
        return drained;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, DemonPylonBlockEntity pylon) {
        if (!(level instanceof ServerLevel) || pylon.isRemoved()) return;
        boolean changed = false;
        for (EnumDemonWillType type : EnumDemonWillType.values()) {
            double localAura = WorldDemonWillHandler.getCurrentWill(level, pos, type);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos source = pos.relative(side, 16);
                double difference = WorldDemonWillHandler.getCurrentWill(level, source, type) - localAura;
                if (difference <= 0) continue;
                double transfer = Math.min(1, difference / 2);
                double drained = WorldDemonWillHandler.drainWill(level, source, type, transfer, true);
                if (drained > 0) {
                    WorldDemonWillHandler.fillWill(level, pos, type, drained, true); changed = true; localAura += drained;
                }
            }
        }
        if (changed) { pylon.setChanged(); level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS); }
    }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); var h = new wayoftime.bloodmagic.will.DemonWillHolder(); stored.forEach(h::addWill); out.store("will", wayoftime.bloodmagic.will.DemonWillHolder.CODEC, h); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); stored.clear(); in.read("will", wayoftime.bloodmagic.will.DemonWillHolder.CODEC).ifPresent(h -> stored.putAll(h.willMap)); }
}
