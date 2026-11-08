package wayoftime.bloodmagic.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.block.entity.AlchemicalReactionChamberBlockEntity;
import wayoftime.bloodmagic.common.registries.BloodMagicMenus;

/** Nine ARC slots plus progress, status and both tank readings. */
public final class ArcMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 8;
    private final Container container;
    private final ContainerData data;

    public ArcMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(9), new SimpleContainerData(DATA_COUNT)); }
    public ArcMenu(int id, Inventory inventory, AlchemicalReactionChamberBlockEntity arc) {
        this(id, inventory, arc, new ContainerData() {
            @Override public int get(int i) { return switch (i) {
                case 0 -> arc.progress(); case 1 -> arc.duration(); case 2 -> arc.status(); case 3 -> arc.inputFluidAmount();
                case 4 -> arc.tankCapacity(); case 5 -> arc.outputFluidAmount(); case 6 -> arc.inputFluid().equals(net.minecraft.resources.Identifier.withDefaultNamespace("water")) ? 1 : 0;
                case 7 -> arc.outputFluid().equals(net.minecraft.resources.Identifier.withDefaultNamespace("water")) ? 1 : 0; default -> 0;
            }; }
            @Override public void set(int i, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        });
    }
    private ArcMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(BloodMagicMenus.ARC_CONTAINER.get(), id);
        checkContainerSize(container, 9); checkContainerDataCount(data, DATA_COUNT);
        this.container = container; this.data = data;
        addSlot(new Slot(container, AlchemicalReactionChamberBlockEntity.TOOL_SLOT, 35, 51));
        for (int i = 0; i < AlchemicalReactionChamberBlockEntity.NUM_OUTPUTS; i++) {
            int index = AlchemicalReactionChamberBlockEntity.OUTPUT_SLOT + i;
            addSlot(new Slot(container, index, 116, 15 + i * 18) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        }
        addSlot(new Slot(container, AlchemicalReactionChamberBlockEntity.INPUT_SLOT, 71, 15));
        addSlot(new Slot(container, AlchemicalReactionChamberBlockEntity.INPUT_BUCKET_SLOT, 8, 15) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(getContainerSlot(), stack); } });
        addSlot(new Slot(container, AlchemicalReactionChamberBlockEntity.OUTPUT_BUCKET_SLOT, 152, 87) { @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(getContainerSlot(), stack); } });
        addStandardInventorySlots(inventory, 8, 123);
        addDataSlots(data);
    }
    public int progress() { return data.get(0); }
    public int duration() { return Math.max(1, data.get(1)); }
    public int status() { return data.get(2); }
    public int inputAmount() { return data.get(3); }
    public int tankCapacity() { return Math.max(1, data.get(4)); }
    public int outputAmount() { return data.get(5); }
    public boolean inputIsWater() { return data.get(6) == 1; }
    public boolean outputIsWater() { return data.get(7) == 1; }
    @Override public boolean stillValid(Player player) { return player.isAlive() && !player.isSpectator() && container.stillValid(player); }
    @Override public void clicked(int slot, int button, net.minecraft.world.inventory.ContainerInput input, Player player) { if (stillValid(player)) super.clicked(slot, button, input, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index); if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack moving = source.getItem(), original = moving.copy(); boolean moved;
        if (index < 9) moved = moveItemStackTo(moving, 9, 45, true);
        else if (moving.getItem() instanceof net.minecraft.world.item.BucketItem) {
            if (moving.is(net.minecraft.world.item.Items.BUCKET)) moved = moveItemStackTo(moving, 8, 9, false);
            else moved = moveItemStackTo(moving, 7, 8, false);
        } else moved = moveItemStackTo(moving, 6, 7, false);
        if (!moved) return ItemStack.EMPTY;
        if (moving.isEmpty()) source.setByPlayer(ItemStack.EMPTY); else source.setChanged();
        source.onTake(player, moving); container.setChanged(); return original;
    }
}
