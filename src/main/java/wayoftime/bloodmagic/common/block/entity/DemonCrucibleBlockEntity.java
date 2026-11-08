package wayoftime.bloodmagic.common.block.entity;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.api.compat.IDemonWillConduit;
import wayoftime.bloodmagic.common.item.ItemDemonCrystal;
import wayoftime.bloodmagic.common.item.ItemDemonWill;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;

/** One-slot upstream crucible. Normal mode burns input into chunk aura; powered mode fills a gem. */
public final class DemonCrucibleBlockEntity extends BlockEntity implements WorldlyContainer, IDemonWillConduit {
    private ItemStack item = ItemStack.EMPTY;
    private final EnumMap<EnumDemonWillType, Double> stored = new EnumMap<>(EnumDemonWillType.class);
    private int tick;
    public DemonCrucibleBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super(BloodMagicBlockEntities.DEMON_CRUCIBLE.get(), pos, state);
    }
    public static void serverTick(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, DemonCrucibleBlockEntity crucible) {
        if (!(level instanceof ServerLevel) || crucible.isRemoved()) return;
        if (++crucible.tick % 2 != 0) return;
        if (level.hasNeighborSignal(pos)) crucible.fillGemFromBuffer(); else crucible.operateNormally();
    }
    private void fillGemFromBuffer() {
        if (!(item.getItem() instanceof ItemDemonWill gem) || !gem.isGem()) return;
        boolean changed = false;
        for (var type : EnumDemonWillType.values()) {
            double available = stored.getOrDefault(type, 0.0);
            double moved = Math.min(10, available);
            moved = ItemDemonWill.fill(item, type, moved, true);
            if (moved > 0) {
                double left = available - moved;
                if (left < 1.0e-9) stored.remove(type); else stored.put(type, left);
                changed = true;
            }
        }
        if (changed) setChangedAndSync();
    }
    private void operateNormally() {
        if (item.isEmpty()) return;
        if (item.getItem() instanceof ItemDemonWill gem && gem.isGem()) {
            boolean changed = false;
            for (var type : EnumDemonWillType.values()) {
                double aura = WorldDemonWillHandler.getCurrentWill(level, worldPosition, type);
                double moved = ItemDemonWill.fill(item, type, Math.min(10, aura), false);
                moved = WorldDemonWillHandler.drainWill(level, worldPosition, type, moved, true);
                if (moved > 0) { ItemDemonWill.fill(item, type, moved, true); changed = true; }
            }
            if (changed) setChangedAndSync();
            return;
        }
        if (item.getItem() instanceof ItemDemonCrystal crystal) {
            double current = WorldDemonWillHandler.getCurrentWill(level, worldPosition, crystal.type());
            if (100 - current < ItemDemonCrystal.DISCRETIZATION) return;
            if (ItemDemonCrystal.drainWill(item, ItemDemonCrystal.DISCRETIZATION) > 0) {
                WorldDemonWillHandler.fillWillToMaximum(level, worldPosition, crystal.type(), ItemDemonCrystal.DISCRETIZATION, 100, true);
                setChangedAndSync();
            }
        } else if (item.getItem() instanceof ItemDemonWill will) {
            var type = ItemDemonWill.type(item);
            double amount = Math.min(10, ItemDemonWill.amount(item));
            double moved = WorldDemonWillHandler.fillWillToMaximum(level, worldPosition, type, amount, 100, false);
            moved = ItemDemonWill.drain(item, type, moved, true);
            if (moved > 0) {
                WorldDemonWillHandler.fillWillToMaximum(level, worldPosition, type, moved, 100, true);
                if (!will.isGem() && ItemDemonWill.amount(item) <= 0) item = ItemStack.EMPTY;
                setChangedAndSync();
            }
        }
    }
    public void dropInventory() { if (level instanceof ServerLevel && !item.isEmpty()) Containers.dropItemStack(level, worldPosition.getX()+.5, worldPosition.getY()+.5, worldPosition.getZ()+.5, item); item = ItemStack.EMPTY; }
    public double fillDemonWill(EnumDemonWillType type, double amount, boolean doFill) {
        if (!(amount > 0)) return 0;
        double accepted = Math.min(100 - stored.getOrDefault(type, 0.0), amount);
        if (doFill && accepted > 0) { stored.merge(type, accepted, Double::sum); setChangedAndSync(); }
        return Math.max(0, accepted);
    }
    @Override public int getWeight() { return 10; }
    @Override public boolean canFill(EnumDemonWillType type) { return true; }
    @Override public boolean canDrain(EnumDemonWillType type) { return true; }
    @Override public double getCurrentWill(EnumDemonWillType type) { return stored.getOrDefault(type, 0.0); }
    public double drainDemonWill(EnumDemonWillType type, double amount, boolean doDrain) {
        double available = stored.getOrDefault(type, 0.0), drained = Math.max(0, Math.min(available, amount));
        if (doDrain && drained > 0) { double left = available-drained; if (left < 1e-9) stored.remove(type); else stored.put(type, left); setChangedAndSync(); }
        return drained;
    }
    private void setChangedAndSync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return item.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? item : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { if (slot != 0 || amount <= 0 || item.isEmpty()) return ItemStack.EMPTY; ItemStack result = item.split(amount); if (item.isEmpty()) item = ItemStack.EMPTY; if (!result.isEmpty()) setChangedAndSync(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { if (slot != 0) return ItemStack.EMPTY; ItemStack result = item; item = ItemStack.EMPTY; return result; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot != 0) return; item = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1); setChangedAndSync(); }
    @Override public void clearContent() { item = ItemStack.EMPTY; setChangedAndSync(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && (stack.getItem() instanceof ItemDemonCrystal || stack.getItem() instanceof ItemDemonWill); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); out.store("item", ItemStack.OPTIONAL_CODEC, item);
        out.store("will", wayoftime.bloodmagic.will.DemonWillHolder.CODEC, holder()); out.putInt("tick", tick);
    }
    private wayoftime.bloodmagic.will.DemonWillHolder holder() { var h = new wayoftime.bloodmagic.will.DemonWillHolder(); stored.forEach(h::addWill); return h; }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); item = in.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        stored.clear(); in.read("will", wayoftime.bloodmagic.will.DemonWillHolder.CODEC).ifPresent(h -> stored.putAll(h.willMap)); tick = Math.max(0, in.getIntOr("tick", 0));
    }
    @Override public void preRemoveSideEffects(BlockPos pos, net.minecraft.world.level.block.state.BlockState newState) { dropInventory(); super.preRemoveSideEffects(pos, newState); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
