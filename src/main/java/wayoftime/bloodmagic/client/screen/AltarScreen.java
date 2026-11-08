package wayoftime.bloodmagic.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.menu.AltarMenu;

/** 26.1 extracts GUI render state; it no longer overrides the old renderBg/renderLabels hooks. */
public final class AltarScreen extends AbstractContainerScreen<AltarMenu> {
    private static final Identifier BACKGROUND = BloodMagic.rl("textures/gui/altar.png");
    private static final int TEXT = 0xFF403536;
    private static final int PROGRESS_WIDTH = 104;
    private static final int TANK_WIDTH = 160;

    public AltarScreen(AltarMenu menu, Inventory inventory, Component title) {
        // imageWidth/imageHeight are final in 26.1: supply them to the superclass constructor.
        super(menu, inventory, title, 176, 204);
        inventoryLabelY = AltarMenu.INVENTORY_Y - 12;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        // Texture UV dimensions match the actual PNG, with no implicit 256x256 assumption.
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos,
                0, 0, imageWidth, imageHeight, 176, 204);
        int progress = menu.progressPixels(PROGRESS_WIDTH);
        int blood = menu.bloodPixels(TANK_WIDTH);
        if (progress > 0) {
            graphics.fill(leftPos + 64, topPos + 43, leftPos + 64 + progress, topPos + 51, 0xFFB52C41);
            graphics.fill(leftPos + 64, topPos + 43, leftPos + 64 + progress, topPos + 45, 0xFFE57179);
        }
        if (blood > 0) {
            graphics.fill(leftPos + 8, topPos + 96, leftPos + 8 + blood, topPos + 102, 0xFF8C132B);
            graphics.fill(leftPos + 8, topPos + 96, leftPos + 8 + blood, topPos + 98, 0xFFD14C60);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // AbstractContainerScreen already translates label coordinates to leftPos/topPos.
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("gui.bloodmagic.altar.tier", menu.tier()), 8, 20, TEXT, false);
        graphics.text(font, Component.translatable(statusKey()), 64, 30, TEXT, false);
        graphics.text(font, menu.progressPixels(100) + "%", 64, 55, TEXT, false);
        graphics.text(font, Component.translatable("gui.bloodmagic.altar.lp"), 8, 70, TEXT, false);
        drawFittingText(graphics, menu.blood() + " / " + menu.capacity(), 8, 82, TANK_WIDTH);
    }

    private String statusKey() {
        if (!menu.active()) return "gui.bloodmagic.altar.idle";
        return menu.blood() == 0 ? "gui.bloodmagic.altar.starved" : "gui.bloodmagic.altar.crafting";
    }

    private void drawFittingText(GuiGraphicsExtractor graphics, String text, int x, int y, int width) {
        // Even two Integer.MAX_VALUE values fit the panel; tooltips retain full-size text.
        float scale = Math.min(1.0F, (float) width / Math.max(1, font.width(text)));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, TEXT, false);
        graphics.pose().popMatrix();
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY); // Preserve normal slot/item/component tooltips.
        if (isHovering(64, 43, PROGRESS_WIDTH, 8, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.translatable("gui.bloodmagic.altar.progress",
                    menu.progress(), menu.requiredBlood()), mouseX, mouseY);
        } else if (isHovering(8, 80, TANK_WIDTH, 23, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.translatable("gui.bloodmagic.altar.stored",
                    menu.blood(), menu.capacity()), mouseX, mouseY);
        }
    }
}
