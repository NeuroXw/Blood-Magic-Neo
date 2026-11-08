package wayoftime.bloodmagic.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.block.BlockDemonCrystal;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;

/** Upstream TileDemonCrystal growth, catalyst reservoir and manual single-crystal harvest. */
public final class DemonCrystalBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final double SAME_WILL_CONVERSION_RATE = 45, DEFAULT_WILL_CONVERSION_RATE = 90, WRONG_WILL_DELAY = .6;
    public double progressToNextCrystal, injectedWill, speedModifier = 1, appliedConversionRate = SAME_WILL_CONVERSION_RATE;
    public int internalCounter;
    public Direction placement = Direction.UP;
    public DemonCrystalBlockEntity(BlockPos pos, BlockState state) { super(BloodMagicBlockEntities.DEMON_CRYSTAL.get(), pos, state); }
    public EnumDemonWillType getWillType() { return ((BlockDemonCrystal)getBlockState().getBlock()).willType(); }
    public double getInjectedWill() { return injectedWill; }
    public int getCrystalCount() { return getBlockState().getValue(BlockDemonCrystal.AGE) + 1; }
    public void setCrystalCount(int count) {
        if (level == null || count < 1 || count > 7) return;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockDemonCrystal.AGE, count - 1));
        setChanged();
    }
    public void applyCatalyst(double added, double speed, double conversion) {
        if (speedModifier < speed) speedModifier = speed;
        if (appliedConversionRate > conversion) appliedConversionRate = conversion;
        injectedWill += added; setChanged();
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, DemonCrystalBlockEntity crystal) {
        if (!(level instanceof ServerLevel) || crystal.isRemoved()) return;
        crystal.internalCounter++;
        if (crystal.internalCounter % 20 != 0 || crystal.getCrystalCount() >= 7) return;
        EnumDemonWillType type = crystal.getWillType();
        double value = WorldDemonWillHandler.getCurrentWill(level, pos, type);
        if (value >= .5) {
            double progress = crystal.growthRate(value);
            double bufferRate = SAME_WILL_CONVERSION_RATE - crystal.appliedConversionRate;
            double conversion = Math.min(crystal.appliedConversionRate, SAME_WILL_CONVERSION_RATE);
            if (crystal.injectedWill > 0 && bufferRate > 0) progress = Math.min(progress, crystal.injectedWill / bufferRate);
            double drained = WorldDemonWillHandler.drainWill(level, pos, type, progress * conversion, true);
            progress = Math.min(progress, drained / conversion);
            crystal.progressToNextCrystal += progress;
            if (crystal.injectedWill > 0 && bufferRate > 0) {
                crystal.injectedWill = Math.max(0, crystal.injectedWill - progress * bufferRate);
                if (crystal.injectedWill <= 0) { crystal.appliedConversionRate = SAME_WILL_CONVERSION_RATE; crystal.speedModifier = 1; }
            }
        } else if (type != EnumDemonWillType.DEFAULT) {
            double raw = WorldDemonWillHandler.getCurrentWill(level, pos, EnumDemonWillType.DEFAULT);
            if (raw > .5) {
                double progress = crystal.growthRate(raw) * WRONG_WILL_DELAY;
                crystal.progressToNextCrystal += WorldDemonWillHandler.drainWill(level, pos, EnumDemonWillType.DEFAULT, progress * DEFAULT_WILL_CONVERSION_RATE, true) / DEFAULT_WILL_CONVERSION_RATE;
            }
        }
        crystal.growIfReady(); crystal.setChanged();
    }
    private double growthRate(double will) { return (1.0 / 200) * Math.sqrt(will / 200) * Math.max(.01, speedModifier); }
    private void growIfReady() {
        if (progressToNextCrystal >= 1 && internalCounter % 100 == 0 && getCrystalCount() < 7) {
            progressToNextCrystal--; setCrystalCount(getCrystalCount() + 1);
        }
    }
    public boolean dropSingleCrystal() {
        if (level == null || level.isClientSide() || getCrystalCount() <= 1) return false;
        setCrystalCount(getCrystalCount() - 1);
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), BlockDemonCrystal.crystalItem(getWillType()));
        return true;
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); out.putInt("placement", placement.get3DDataValue()); out.putDouble("progress", progressToNextCrystal);
        out.putDouble("injected_will", injectedWill); out.putDouble("speed_modifier", speedModifier); out.putDouble("conversion_rate", appliedConversionRate); out.putInt("counter", internalCounter);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); placement = Direction.from3DDataValue(Math.floorMod(in.getIntOr("placement", 1), 6));
        progressToNextCrystal = Math.clamp(in.getDoubleOr("progress", 0), 0, 1); injectedWill = Math.clamp(in.getDoubleOr("injected_will", 0), 0, 400);
        speedModifier = Math.clamp(in.getDoubleOr("speed_modifier", 1), .01, 100); appliedConversionRate = Math.clamp(in.getDoubleOr("conversion_rate", SAME_WILL_CONVERSION_RATE), 1, SAME_WILL_CONVERSION_RATE);
        internalCounter = Math.max(0, in.getIntOr("counter", 0));
    }
}
