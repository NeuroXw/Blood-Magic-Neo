package wayoftime.bloodmagic.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import wayoftime.bloodmagic.common.block.entity.AlchemyArrayBlockEntity;

/** All live world reads happen in extraction. Deferred geometry captures only frame-local values. */
public final class AlchemyArrayRenderer implements BlockEntityRenderer<AlchemyArrayBlockEntity, ArrayRenderState> {
    private final ItemModelResolver models;
    public AlchemyArrayRenderer(BlockEntityRendererProvider.Context context) { models = context.itemModelResolver(); }
    @Override public ArrayRenderState createRenderState() { return new ArrayRenderState(); }
    @Override public void extractRenderState(AlchemyArrayBlockEntity array, ArrayRenderState state, float partial,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        state.visible = false; state.base.clear(); state.catalyst.clear(); state.rotation = 0;
        if (array == null || array.isRemoved() || array.getLevel() == null || !array.getLevel().isClientSide()) return;
        BlockEntityRenderer.super.extractRenderState(array, state, partial, camera, overlay);
        state.visible = true; state.texture = array.texture();
        state.rotation = -array.rotation().toYRot() + (array.progress() > 0 ? ((array.getLevel().getGameTime() % 360) + partial) * 2 : 0);
        models.updateForTopItem(state.base, array.getItem(0).copy(), ItemDisplayContext.GROUND, array.getLevel(), null, 0);
        models.updateForTopItem(state.catalyst, array.getItem(1).copy(), ItemDisplayContext.GROUND, array.getLevel(), null, 1);
    }
    @Override public void submit(ArrayRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) return;
        poses.pushPose();
        try {
            poses.translate(.5, .02, .5);
            poses.mulPose(Axis.YP.rotationDegrees(state.rotation));
            collector.submitCustomGeometry(poses, RenderTypes.entityTranslucent(state.texture), (pose, vertices) -> {
                vertex(vertices, pose, -.48F, -.48F, 0, 0); vertex(vertices, pose, -.48F, .48F, 0, 1);
                vertex(vertices, pose, .48F, .48F, 1, 1); vertex(vertices, pose, .48F, -.48F, 1, 0);
            });
        } finally { poses.popPose(); }
        poses.pushPose();
        try {
            poses.translate(.35, .18, .5); poses.scale(.55F, .55F, .55F);
            state.base.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.translate(.55, .12, 0);
            state.catalyst.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        } finally { poses.popPose(); }
    }
    private static void vertex(VertexConsumer v, PoseStack.Pose pose, float x, float z, float u, float w) {
        v.addVertex(pose, x, 0, z).setColor(0xFFFFFFFF).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0).setNormal(pose, 0, 1, 0);
    }
}
