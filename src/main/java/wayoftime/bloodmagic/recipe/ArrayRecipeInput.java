package wayoftime.bloodmagic.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record ArrayRecipeInput(ItemStack base, ItemStack catalyst) implements RecipeInput {
    @Override public int size() { return 2; }
    @Override public ItemStack getItem(int slot) {
        return switch (slot) { case 0 -> base; case 1 -> catalyst; default -> throw new IndexOutOfBoundsException(slot); };
    }
}
