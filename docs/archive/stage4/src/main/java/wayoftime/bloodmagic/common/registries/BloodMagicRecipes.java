package wayoftime.bloodmagic.common.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.recipe.BloodAltarRecipe;

public final class BloodMagicRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, BloodMagic.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, BloodMagic.MODID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<BloodAltarRecipe>> ALTAR_TYPE = TYPES.register("altar",
            () -> new RecipeType<>() {
                @Override public String toString() { return "bloodmagic:altar"; }
            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BloodAltarRecipe>> ALTAR_SERIALIZER =
            SERIALIZERS.register("altar", () -> new RecipeSerializer<>(BloodAltarRecipe.CODEC, BloodAltarRecipe.STREAM_CODEC));

    private BloodMagicRecipes() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
}
