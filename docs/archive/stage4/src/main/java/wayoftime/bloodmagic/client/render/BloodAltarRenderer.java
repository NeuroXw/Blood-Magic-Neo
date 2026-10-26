package wayoftime.bloodmagic.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;

/**
 * 26.1.2 uses extractRenderState + submit, not the old render/ItemRenderer.renderStatic API.
 * Signatures and item submission follow the exact patched JAR's CampfireRenderer.
 * Compiled against 26.1.2.122; visual appearance still requires a graphical client check.
 */
public final class BloodAltarRenderer implements BlockEntityRenderer<BloodAltarBlockEntity, AltarRenderState> {
    private final ItemModelResolver itemModels;

    public BloodAltarRenderer(BlockEntityRendererProvider.Context context) {
        itemModels = context.itemModelResolver();
    }

    @Override
    public AltarRenderState createRenderState() { return new AltarRenderState(); }

    @Override
    public void extractRenderState(BloodAltarBlockEntity altar, AltarRenderState state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        // Clear reused state first so removal/empty slots cannot leave a ghost item.
        state.item.clear();
        state.rotation = 0;
        if (altar == null || altar.isRemoved()) return;
        BlockEntityRenderer.super.extractRenderState(altar, state, partialTick, cameraPosition, breakProgress);
        Level level = altar.getLevel();
        if (level == null || !level.isClientSide()) return;
        ItemStack stack = altar.getStoredItem(); // Defensive snapshot, synchronized by vanilla BE packets.
        if (stack.isEmpty()) return;
        state.rotation = ((level.getGameTime() % 360) + partialTick) % 360;
        itemModels.updateForTopItem(state.item, stack, ItemDisplayContext.GROUND, level, null,
                (int) altar.getBlockPos().asLong());
    }

    @Override
    public void submit(AltarRenderState state, PoseStack poses, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (state.item.isEmpty()) return;
        poses.pushPose();
        try {
            poses.translate(0.5F, 1.25F, 0.5F);
            poses.mulPose(Axis.YP.rotationDegrees(state.rotation)); // One revolution per 18 seconds at 20 TPS.
            state.item.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        } finally {
            poses.popPose();
        }
    }
}
