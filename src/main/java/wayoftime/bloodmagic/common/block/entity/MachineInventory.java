package wayoftime.bloodmagic.common.block.entity;

import java.util.ArrayList;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Common server-owned inventory. Removal side effects run once in the 26.1 BE lifecycle. */
public abstract class MachineInventory extends BlockEntity implements WorldlyContainer {
    protected final List<ItemStack> items;
    private List<ItemStack> snapshot;
    protected int progress;
    private int syncClock;
    protected MachineInventory(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) items.add(ItemStack.EMPTY);
        snapshot = copies();
    }
    protected boolean serverSide() { return level == null || !level.isClientSide(); }
    protected List<ItemStack> copies() { return items.stream().map(ItemStack::copy).toList(); }
    protected void inventoryChanged() { progress = 0; }
    public int progress() { return progress; }
    public abstract int duration();
    public abstract int outputSlot();
    public abstract int catalystSlot();
    public int status() { return 0; }
    public int resourceAmount() { return 0; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (!serverSide() || slot < 0 || slot >= items.size()) return;
        ItemStack copy = stack.copy();
        copy.limitSize(getMaxStackSize(copy));
        items.set(slot, copy);
        setChanged();
    }
    @Override public ItemStack removeItem(int slot, int count) {
        if (!serverSide() || count <= 0) return ItemStack.EMPTY;
        ItemStack removed = getItem(slot).split(count);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, Integer.MAX_VALUE); }
    @Override public void clearContent() {
        if (!serverSide()) return;
        items.replaceAll(s -> ItemStack.EMPTY);
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return level != null && player.level() == level && !isRemoved() && player.isAlive() && !player.isSpectator()
                && Container.stillValidBlockEntity(this, player);
    }
    @Override public void setChanged() {
        boolean changed = false;
        if (items != null && snapshot != null && serverSide()) {
            for (int i = 0; i < items.size(); i++) changed |= !ItemStack.matches(items.get(i), snapshot.get(i));
            if (changed) { snapshot = copies(); inventoryChanged(); }
        }
        super.setChanged();
        if (changed || ++syncClock >= 5) { syncClock = 0; sync(); }
    }
    protected void sync() {
        if (level instanceof ServerLevel) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    protected boolean canAcceptOutput(ItemStack result) {
        ItemStack existing = getItem(outputSlot());
        return !result.isEmpty() && result.getCount() <= result.getMaxStackSize() && (existing.isEmpty()
                || ItemStack.isSameItemSameComponents(existing, result) && (long) existing.getCount() + result.getCount() <= existing.getMaxStackSize());
    }
    protected void insertOutput(ItemStack result) {
        if (getItem(outputSlot()).isEmpty()) items.set(outputSlot(), result.copy());
        else getItem(outputSlot()).grow(result.getCount());
    }
    protected void consumeInputs(int count) {
        for (int i = 0; i < count; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            var template = stack.getCraftingRemainder();
            stack.shrink(1);
            if (template != null) {
                ItemStack remainder = template.create();
                if (stack.isEmpty()) items.set(i, remainder);
                else if (ItemStack.isSameItemSameComponents(stack, remainder) && stack.getCount() + remainder.getCount() <= stack.getMaxStackSize()) stack.grow(remainder.getCount());
                else if (level instanceof ServerLevel) Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5, remainder);
            }
        }
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = in.read("slot_" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            stack.limitSize(getMaxStackSize(stack));
            items.set(i, stack);
        }
        snapshot = copies();
        progress = Math.clamp(in.getIntOr("progress", 0), 0, 72000);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        for (int i = 0; i < items.size(); i++) out.store("slot_" + i, ItemStack.OPTIONAL_CODEC, items.get(i));
        out.putInt("progress", progress);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState newState) {
        if (level instanceof ServerLevel) {
            for (ItemStack stack : items) if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack);
            items.replaceAll(s -> ItemStack.EMPTY);
        }
        super.preRemoveSideEffects(pos, newState);
    }
    @Override public int[] getSlotsForFace(Direction side) {
        if (outputSlot() < 0) return new int[0];
        return side == Direction.DOWN ? new int[]{outputSlot()} : java.util.stream.IntStream.range(0, catalystSlot()).toArray();
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == outputSlot(); }
}
