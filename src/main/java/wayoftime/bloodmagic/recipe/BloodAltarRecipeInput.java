package wayoftime.bloodmagic.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Tier belongs to the matching context, not a global singleton altar. */
public record BloodAltarRecipeInput(ItemStack stack, int tier) implements RecipeInput {
    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) throw new IllegalArgumentException("An altar has one slot");
        return stack;
    }

    @Override
    public int size() {
        return 1;
    }
}
