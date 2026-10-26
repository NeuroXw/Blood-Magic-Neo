package wayoftime.bloodmagic.client.render;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import wayoftime.bloodmagic.recipe.AlchemyArrayRecipe;
public final class ArrayRenderState extends BlockEntityRenderState {
    public Identifier texture = AlchemyArrayRecipe.DEFAULT_TEXTURE;
    public final ItemStackRenderState base = new ItemStackRenderState();
    public final ItemStackRenderState catalyst = new ItemStackRenderState();
    public float rotation;
    public boolean visible;
}
