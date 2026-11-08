package wayoftime.bloodmagic.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.menu.MachineMenu;

/** Draws the upstream alchemytable.png / soulforge.png, using upstream ScreenAlchemyTable/ScreenSoulForge UVs. */
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final Identifier TABLE = BloodMagic.rl("textures/gui/alchemytable.png");
    private static final Identifier FORGE = BloodMagic.rl("textures/gui/soulforge.png");
    private static final int TEX = 256;
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 205); inventoryLabelY = 111; }
    private Identifier texture() { return menu.isTable() ? TABLE : FORGE; }
    private int barX() { return menu.isTable() ? 106 : 115; }
    private void sprite(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture(), leftPos + x, topPos + y, u, v, w, h, TEX, TEX);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float partial) {
        super.extractBackground(g, x, y, partial);
        sprite(g, 0, 0, 0, 0, imageWidth, imageHeight);
        int l = (int) Math.clamp((long) menu.progress() * 90 / Math.max(1, menu.duration()), 0, 90);
        if (l > 0) sprite(g, barX(), 14 + 90 - l, 176, 90 - l, 18, l);
        int status = menu.status();
        if (menu.isTable()) {
            if (status == 1) sprite(g, 106, 24, 194, 55, 18, 18);      // Missing / low-tier orb.
            else if (status == 2) sprite(g, 106, 24, 194, 73, 18, 18); // Not enough LP.
        } else if (status == 1 || status == 2) sprite(g, 116, 51, 194, 0, 16, 16); // Not enough will.
    }
    @Override protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (!isHovering(barX(), 14, 18, 90, mouseX, mouseY)) return;
        var resource = menu.isTable() ? Component.translatable("gui.bloodmagic.machine.lp", menu.resourceAmount())
                : Component.translatable("gui.bloodmagic.machine.will", String.format(java.util.Locale.ROOT, "%.2f", menu.resourceAmount() / 100.0));
        g.setTooltipForNextFrame(resource.append(" | ").append(Component.translatable("gui.bloodmagic.machine.status." + menu.status())), mouseX, mouseY);
    }
}
