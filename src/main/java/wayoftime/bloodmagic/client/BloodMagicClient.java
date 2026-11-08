package wayoftime.bloodmagic.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import wayoftime.bloodmagic.client.render.BloodAltarRenderer;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.client.screen.AltarScreen;
import wayoftime.bloodmagic.common.registries.BloodMagicMenus;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;

/** Scanned only on the physical client; common registration never loads screen classes. */
@EventBusSubscriber(modid = BloodMagic.MODID, value = Dist.CLIENT)
public final class BloodMagicClient {
    private BloodMagicClient() {}

    @SubscribeEvent
    public static void registerItemProperties(net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent event) {
        event.register(BloodMagic.rl("sigil_active"), SigilActiveProperty.TYPE);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BloodMagicBlockEntities.BLOOD_ALTAR.get(), BloodAltarRenderer::new);
        event.registerBlockEntityRenderer(BloodMagicBlockEntities.ALCHEMY_ARRAY.get(), wayoftime.bloodmagic.client.render.AlchemyArrayRenderer::new);
        event.registerBlockEntityRenderer(BloodMagicBlockEntities.DEMON_CRUCIBLE.get(), wayoftime.bloodmagic.client.render.DemonCrucibleRenderer::new);
        event.registerEntityRenderer(wayoftime.bloodmagic.common.registries.BloodMagicWill.SOUL_SNARE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BloodMagicMenus.ALTAR.get(), AltarScreen::new);
        event.register(BloodMagicMenus.ALCHEMY_TABLE.get(), wayoftime.bloodmagic.client.screen.MachineScreen::new);
        event.register(BloodMagicMenus.HELLFIRE_FORGE.get(), wayoftime.bloodmagic.client.screen.MachineScreen::new);
        event.register(BloodMagicMenus.ARC_CONTAINER.get(), wayoftime.bloodmagic.client.screen.ArcScreen::new);
    }
}
