package wayoftime.bloodmagic.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import wayoftime.bloodmagic.common.menu.MachineMenu;

public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); inventoryLabelY = 92; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float partial) {
        super.extractBackground(g, x, y, partial);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC5B8A8);
        g.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFFE1D8CA);
        for (var slot : menu.slots) {
            int sx = leftPos + slot.x, sy = topPos + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF51433D);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFFAA9B8A);
        }
        int width = (int) Math.clamp((long) menu.progress() * 160 / Math.max(1, menu.duration()), 0, 160);
        g.fill(leftPos + 8, topPos + 84, leftPos + 168, topPos + 88, 0xFF51433D);
        g.fill(leftPos + 8, topPos + 84, leftPos + 8 + width, topPos + 88, 0xFFA82E45);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int x, int y) {
        super.extractLabels(g, x, y);
        var label = menu.isTable() ? Component.translatable("gui.bloodmagic.machine.lp", menu.resourceAmount())
                : Component.translatable("gui.bloodmagic.machine.will", String.format(java.util.Locale.ROOT, "%.2f", menu.resourceAmount() / 100.0));
        g.text(font, label, 8, 65, 0xFF403536, false);
        g.text(font, Component.translatable("gui.bloodmagic.machine.status." + menu.status()), 8, 75, 0xFF403536, false);
    }
}
