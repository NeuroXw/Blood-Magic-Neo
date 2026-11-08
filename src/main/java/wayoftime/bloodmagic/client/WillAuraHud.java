package wayoftime.bloodmagic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.item.ItemDemonWillGauge;
import wayoftime.bloodmagic.will.ClientAura;

/** Compact replacement for upstream ElementDemonAura. */
@EventBusSubscriber(modid = BloodMagic.MODID, value = Dist.CLIENT)
public final class WillAuraHud {
    private static final Identifier LAYER = BloodMagic.rl("demon_will_aura");
    private static final EnumDemonWillType[] ORDER = { EnumDemonWillType.DEFAULT, EnumDemonWillType.CORROSIVE,
            EnumDemonWillType.STEADFAST, EnumDemonWillType.DESTRUCTIVE, EnumDemonWillType.VENGEFUL };
    private static final String[] LABELS = { "RAW", "COR", "STF", "DST", "VNG" };
    private static final int[] COLORS = { 0xffe9e9e9, 0xff74d65a, 0xff58cbd8, 0xffd86442, 0xffc75acb };
    private WillAuraHud() {}

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER, (graphics, delta) -> render(graphics));
    }
    private static void render(GuiGraphicsExtractor graphics) {
        var player = Minecraft.getInstance().player;
        if (!ItemDemonWillGauge.canSee(player)) return;
        var aura = ClientAura.current;
        int x = 8, y = 8, barWidth = 44;
        graphics.fill(x, y, x + 94, y + 57, 0x990b0b0b);
        graphics.text(Minecraft.getInstance().font, "Demon Will", x + 4, y + 2, 0xffeeeeee, true);
        for (int i = 0; i < ORDER.length; i++) {
            double value = aura == null ? 0 : aura.getWill(ORDER[i]);
            int rowY = y + 13 + i * 8;
            graphics.text(Minecraft.getInstance().font, LABELS[i], x + 4, rowY, COLORS[i], true);
            graphics.fill(x + 30, rowY + 1, x + 30 + barWidth, rowY + 6, 0xff303030);
            int width = (int)Math.round(barWidth * Math.clamp(value / ItemDemonWillGauge.RESOLUTION, 0, 1));
            if (width > 0) graphics.fill(RenderPipelines.GUI, x + 30, rowY + 1, x + 30 + width, rowY + 6, COLORS[i]);
            if (player.isShiftKeyDown()) graphics.text(Minecraft.getInstance().font, Integer.toString((int)value), x + 77, rowY, 0xffeeeeee, true);
        }
    }
}
