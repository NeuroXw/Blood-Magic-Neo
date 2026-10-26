package wayoftime.bloodmagic.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import wayoftime.bloodmagic.client.render.BloodAltarRenderer;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.client.screen.AltarScreen;
import wayoftime.bloodmagic.common.registries.BloodMagicMenus;

/** Scanned only on the physical client; common registration never loads screen classes. */
@EventBusSubscriber(modid = BloodMagic.MODID, value = Dist.CLIENT)
public final class BloodMagicClient {
    private BloodMagicClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BloodMagicBlockEntities.BLOOD_ALTAR.get(), BloodAltarRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BloodMagicMenus.ALTAR.get(), AltarScreen::new);
    }
}
