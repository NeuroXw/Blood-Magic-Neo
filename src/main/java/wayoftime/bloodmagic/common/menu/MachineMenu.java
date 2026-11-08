package wayoftime.bloodmagic.common.menu;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.block.entity.MachineInventory;
import wayoftime.bloodmagic.common.item.ItemBloodOrb;
import wayoftime.bloodmagic.common.registries.BloodMagicMenus;

/** Shared eight/six-slot layout, with server inventory authority and native menu sync. */
public final class MachineMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final boolean table;
    private final int size;
    public static MachineMenu table(int id, Inventory inv) { return new MachineMenu(id, inv, new SimpleContainer(8), new SimpleContainerData(4), true); }
    public static MachineMenu forge(int id, Inventory inv) { return new MachineMenu(id, inv, new SimpleContainer(6), new SimpleContainerData(4), false); }
    public MachineMenu(int id, Inventory inv, MachineInventory machine, boolean table) {
        this(id, inv, machine, new ContainerData() {
            @Override public int get(int i) { return switch (i) { case 0 -> machine.progress(); case 1 -> machine.duration(); case 2 -> machine.status(); case 3 -> machine.resourceAmount(); default -> 0; }; }
            @Override public void set(int i, int value) {}
            @Override public int getCount() { return 4; }
        }, table);
    }
    private MachineMenu(int id, Inventory inv, Container container, ContainerData data, boolean table) {
        super(table ? BloodMagicMenus.ALCHEMY_TABLE.get() : BloodMagicMenus.HELLFIRE_FORGE.get(), id);
        this.container = container; this.data = data; this.table = table; size = table ? 8 : 6;
        checkContainerSize(container, size); checkContainerDataCount(data, 4);
        // Upstream ContainerAlchemyTable / ContainerSoulForge coordinates, matching the original GUI PNGs.
        int[][] inputs = table ? new int[][]{{62, 15}, {80, 51}, {62, 87}, {26, 87}, {8, 51}, {26, 15}}
                : new int[][]{{8, 15}, {80, 15}, {8, 87}, {80, 87}};
        for (int i = 0; i < size - 2; i++) {
            final int index = i;
            addSlot(new Slot(container, i, inputs[i][0], inputs[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(index, stack); }
            });
        }
        addSlot(new OrbBindingSlot(container, size - 2, table ? 143 : 152, table ? 24 : 51, inv.player) { @Override public boolean mayPlace(ItemStack stack) { return isCatalyst(stack) && container.canPlaceItem(size - 2, stack); } });
        addSlot(new Slot(container, size - 1, 44, 51) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        addStandardInventorySlots(inv, 8, 123);
        addDataSlots(data);
    }
    public boolean isTable() { return table; }
    public int progress() { return data.get(0); }
    public int duration() { return data.get(1); }
    public int status() { return data.get(2); }
    public int resourceAmount() { return data.get(3); }
    private boolean isCatalyst(ItemStack stack) { return table ? stack.getItem() instanceof ItemBloodOrb : stack.getItem() instanceof wayoftime.bloodmagic.common.item.ItemDemonWill; }
    @Override public boolean stillValid(Player player) { return player.isAlive() && !player.isSpectator() && container.stillValid(player); }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) { if (stillValid(player)) super.clicked(slot, button, input, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack moving = source.getItem(), original = moving.copy();
        boolean moved = index < size ? moveItemStackTo(moving, size, size + 36, true)
                : isCatalyst(moving) ? moveItemStackTo(moving, size - 2, size - 1, false) : moveItemStackTo(moving, 0, size - 2, false);
        if (!moved) return ItemStack.EMPTY;
        if (moving.isEmpty()) source.setByPlayer(ItemStack.EMPTY); else source.setChanged();
        source.onTake(player, moving); container.setChanged();
        return original;
    }
}
