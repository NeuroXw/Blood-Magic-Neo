package wayoftime.bloodmagic.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.common.registries.BloodMagicMenus;

/** One shared input/output slot, followed by 27 inventory and 9 hotbar slots. */
public final class AltarMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 6;
    public static final int INVENTORY_Y = 122;
    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = 37;
    private final Container altar;
    private final ContainerData data;

    /** Client factory: no world lookup or block-entity packet ordering dependency. */
    public AltarMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
    }

    public AltarMenu(int containerId, Inventory inventory, BloodAltarBlockEntity altar) {
        this(containerId, inventory, altar, new ContainerData() {
            @Override
            public int get(int index) {
                var state = altar.getAltarState();
                return switch (index) {
                    case 0 -> state.blood();
                    case 1 -> state.capacity();
                    case 2 -> state.tier();
                    case 3 -> state.progress();
                    case 4 -> state.requiredBlood();
                    case 5 -> state.active() ? 1 : 0;
                    default -> throw new IndexOutOfBoundsException(index);
                };
            }

            @Override
            public void set(int index, int value) {
                // Server state is authoritative. Menu data never writes LP/progress back to the BE.
            }

            @Override
            public int getCount() { return DATA_COUNT; }
        });
    }

    private AltarMenu(int containerId, Inventory inventory, Container altar, ContainerData data) {
        super(BloodMagicMenus.ALTAR.get(), containerId);
        checkContainerSize(altar, 1);
        checkContainerDataCount(data, DATA_COUNT);
        this.altar = altar;
        this.data = data;
        addSlot(new Slot(altar, 0, 26, 42));
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        // DataSlot still exists in 26.1. NeoForge's negotiated AdvancedContainerSetDataPayload
        // carries a full int (vanilla's fallback packet still carries a signed short).
        // This mod requires NeoForge on both sides; do not truncate or cast these LP values.
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isSpectator() && player.isAlive() && altar.stillValid(player);
    }

    @Override
    public void clicked(int index, int button, ContainerInput input, Player player) {
        // Also guard direct calls: a stale menu must never access a removed/replaced altar.
        if (stillValid(player)) super.clicked(index, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack moving = source.getItem();
        ItemStack original = moving.copy();
        boolean moved = index == 0
                ? moveItemStackTo(moving, PLAYER_START, PLAYER_END, true)
                : moveItemStackTo(moving, 0, 1, false);
        if (!moved || moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (moving.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged(); // Container.getItem is a live stack; this commits partial transfers.
        source.onTake(player, moving);
        return original;
    }

    public int blood() { return Math.max(0, data.get(0)); }
    public int capacity() { return Math.max(0, data.get(1)); }
    public int tier() { return Math.clamp(data.get(2), 1, 6); }
    public int progress() { return Math.max(0, data.get(3)); }
    public int requiredBlood() { return Math.max(0, data.get(4)); }
    public boolean active() { return data.get(5) != 0 && requiredBlood() > 0; }

    public int bloodPixels(int width) { return scaled(blood(), capacity(), width); }
    public int progressPixels(int width) { return scaled(progress(), requiredBlood(), width); }

    private static int scaled(int value, int maximum, int width) {
        if (maximum <= 0 || width <= 0) return 0;
        // Long intermediate avoids overflow for high-capacity rune configurations and datapacks.
        return (int) Math.clamp((long) value * width / maximum, 0L, width);
    }
}
