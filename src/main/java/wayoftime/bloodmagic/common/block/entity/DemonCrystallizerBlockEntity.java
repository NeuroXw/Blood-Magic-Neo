package wayoftime.bloodmagic.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.EnumMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.api.compat.IDemonWillConduit;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;

/** Converts 99 aura of the strongest type into a crystal cluster after 1000 server ticks. */
public final class DemonCrystallizerBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements IDemonWillConduit {
    private double progress;
    private final EnumMap<EnumDemonWillType, Double> stored = new EnumMap<>(EnumDemonWillType.class);
    public DemonCrystallizerBlockEntity(BlockPos pos, BlockState state) { super(BloodMagicBlockEntities.DEMON_CRYSTALLIZER.get(), pos, state); }
    public static void serverTick(Level level, BlockPos pos, BlockState state, DemonCrystallizerBlockEntity machine) {
        if (!(level instanceof ServerLevel) || machine.isRemoved() || !level.isEmptyBlock(pos.above())) return;
        EnumDemonWillType type = WorldDemonWillHandler.getHighestDemonWillType(level, pos);
        double available = WorldDemonWillHandler.getCurrentWill(level, pos, type);
        if (available < 99) return;
        if (++machine.progress < 1000) { if (machine.progress % 20 == 0) machine.setChanged(); return; }
        var block = switch (type) {
            case CORROSIVE -> BloodMagicBlocks.CORROSIVE_DEMON_CRYSTAL.get();
            case DESTRUCTIVE -> BloodMagicBlocks.DESTRUCTIVE_DEMON_CRYSTAL.get();
            case STEADFAST -> BloodMagicBlocks.STEADFAST_DEMON_CRYSTAL.get();
            case VENGEFUL -> BloodMagicBlocks.VENGEFUL_DEMON_CRYSTAL.get();
            case DEFAULT -> BloodMagicBlocks.RAW_DEMON_CRYSTAL.get();
        };
        if (level.setBlockAndUpdate(pos.above(), block.defaultBlockState().setValue(wayoftime.bloodmagic.common.block.BlockDemonCrystal.ATTACHED, Direction.UP))) {
            WorldDemonWillHandler.drainWill(level, pos, type, 99, true);
            machine.progress = 0; machine.setChanged();
        }
    }
    @Override public int getWeight() { return 10; }
    @Override public boolean canFill(EnumDemonWillType type) { return true; }
    @Override public boolean canDrain(EnumDemonWillType type) { return true; }
    @Override public double getCurrentWill(EnumDemonWillType type) { return stored.getOrDefault(type, 0.0); }
    @Override public double fillDemonWill(EnumDemonWillType type, double amount, boolean doFill) {
        double accepted = Math.max(0, Math.min(100 - getCurrentWill(type), amount));
        if (doFill && accepted > 0) { stored.merge(type, accepted, Double::sum); setChanged(); }
        return accepted;
    }
    @Override public double drainDemonWill(EnumDemonWillType type, double amount, boolean doDrain) {
        double drained = Math.max(0, Math.min(getCurrentWill(type), amount));
        if (doDrain && drained > 0) { double left = getCurrentWill(type) - drained; if (left < 1e-9) stored.remove(type); else stored.put(type, left); setChanged(); }
        return drained;
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); out.putDouble("progress", progress);
        var holder = new wayoftime.bloodmagic.will.DemonWillHolder(); stored.forEach(holder::addWill);
        out.store("will", wayoftime.bloodmagic.will.DemonWillHolder.CODEC, holder);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); progress = Math.clamp(in.getDoubleOr("progress", 0), 0, 1000); stored.clear();
        in.read("will", wayoftime.bloodmagic.will.DemonWillHolder.CODEC).ifPresent(holder -> stored.putAll(holder.willMap));
    }
}
