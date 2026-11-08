package wayoftime.bloodmagic.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class RecipeSupport {
    private RecipeSupport() {}
    public static boolean same(Ingredient a, Ingredient b) {
        return a.equals(b) || (!a.isCustom() && !b.isCustom() && a.getValues().unwrap().equals(b.getValues().unwrap()));
    }
    public static boolean same(List<Ingredient> a, List<Ingredient> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!same(a.get(i), b.get(i))) return false;
        return true;
    }
    /** One occupied slot per ingredient, including repeated ingredients. Backtracking handles overlapping tags. */
    public static boolean matches(List<Ingredient> ingredients, List<ItemStack> slots) {
        List<ItemStack> occupied = slots.stream().filter(s -> !s.isEmpty()).toList();
        return occupied.size() == ingredients.size() && assign(ingredients, occupied, 0, new boolean[occupied.size()]);
    }
    private static boolean assign(List<Ingredient> ingredients, List<ItemStack> slots, int index, boolean[] used) {
        if (index == ingredients.size()) return true;
        for (int slot = 0; slot < slots.size(); slot++) {
            if (!used[slot] && ingredients.get(index).test(slots.get(slot))) {
                used[slot] = true;
                if (assign(ingredients, slots, index + 1, used)) return true;
                used[slot] = false;
            }
        }
        return false;
    }
}
