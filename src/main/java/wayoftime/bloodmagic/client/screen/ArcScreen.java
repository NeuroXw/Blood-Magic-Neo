package wayoftime.bloodmagic.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.menu.ArcMenu;

/** Upstream arc_gui layout and 38-pixel progress arrow, with numeric tank tooltips. */
public final class ArcScreen extends AbstractContainerScreen<ArcMenu> {
    private static final Identifier TEXTURE = BloodMagic.rl("textures/gui/arc_gui.png");
    public ArcScreen(ArcMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 205); inventoryLabelY = 111; }
    private void sprite(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) { g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x, topPos + y, u, v, w, h, 256, 256); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float partial) {
        super.extractBackground(g, x, y, partial); sprite(g, 0, 0, 0, 0, imageWidth, imageHeight);
        int progress = Math.clamp((long) menu.progress() * 38 / menu.duration(), 0, 38);
        if (progress > 0) sprite(g, 63, 44, 176, 90, progress, 23);
    }
    @Override protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        Component tip = null;
        if (isHovering(8, 40, 16, 63, mouseX, mouseY)) tip = Component.translatable("gui.bloodmagic.arc.input", menu.inputAmount(), menu.tankCapacity(), menu.inputIsWater() ? Component.translatable("fluid.bloodmagic.water") : Component.translatable("fluid.bloodmagic.empty"));
        else if (isHovering(152, 15, 16, 63, mouseX, mouseY)) tip = Component.translatable("gui.bloodmagic.arc.output", menu.outputAmount(), menu.tankCapacity(), menu.outputIsWater() ? Component.translatable("fluid.bloodmagic.water") : Component.translatable("fluid.bloodmagic.empty"));
        if (tip != null) g.setTooltipForNextFrame(tip, mouseX, mouseY);
    }
}
