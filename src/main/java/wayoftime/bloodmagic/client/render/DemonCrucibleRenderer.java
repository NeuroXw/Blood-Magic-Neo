package wayoftime.bloodmagic.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import wayoftime.bloodmagic.common.block.entity.DemonCrucibleBlockEntity;

public final class DemonCrucibleRenderer implements BlockEntityRenderer<DemonCrucibleBlockEntity, DemonCrucibleRenderState> {
    private final ItemModelResolver itemModels;
    public DemonCrucibleRenderer(BlockEntityRendererProvider.Context context) { itemModels = context.itemModelResolver(); }
    @Override public DemonCrucibleRenderState createRenderState() { return new DemonCrucibleRenderState(); }
    @Override public void extractRenderState(DemonCrucibleBlockEntity crucible, DemonCrucibleRenderState state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        state.item.clear(); state.rotation = 0;
        if (crucible == null || crucible.isRemoved() || crucible.getLevel() == null) return;
        Level level = crucible.getLevel();
        if (level.isClientSide()) {
            state.lightCoords = LevelRenderer.getLightCoords(level, crucible.getBlockPos().above());
            if (!crucible.getItem(0).isEmpty()) {
                state.rotation = (level.getGameTime() % 360) + partialTick;
                itemModels.updateForTopItem(state.item, crucible.getItem(0), ItemDisplayContext.FIXED, level, null,
                        crucible.getBlockPos().hashCode());
            }
        }
    }
    @Override public void submit(DemonCrucibleRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.item.isEmpty()) return;
        poses.pushPose();
        poses.translate(.5, 1.15, .5);
        poses.mulPose(Axis.YP.rotationDegrees(state.rotation));
        poses.scale(.45F, .45F, .45F);
        state.item.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poses.popPose();
    }
}
