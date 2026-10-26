package wayoftime.bloodmagic.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Frame-local data: submission never reads a live block entity or inventory. */
public final class AltarRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public float rotation;
}
