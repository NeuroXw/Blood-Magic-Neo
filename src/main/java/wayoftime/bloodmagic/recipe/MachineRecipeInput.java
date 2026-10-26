package wayoftime.bloodmagic.recipe;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
public record MachineRecipeInput(List<ItemStack> items) implements RecipeInput {
    public MachineRecipeInput { items = List.copyOf(items); }
    @Override public int size() { return items.size(); }
    @Override public ItemStack getItem(int index) { return items.get(index); }
}
