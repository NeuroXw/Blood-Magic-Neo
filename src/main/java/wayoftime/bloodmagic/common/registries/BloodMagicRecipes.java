package wayoftime.bloodmagic.common.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.recipe.BloodAltarRecipe;
import wayoftime.bloodmagic.recipe.AlchemyArrayRecipe;
import wayoftime.bloodmagic.recipe.AlchemyTableRecipe;
import wayoftime.bloodmagic.recipe.HellfireForgeRecipe;

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

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlchemyArrayRecipe>> ARRAY_TYPE = TYPES.register("array",
            () -> new RecipeType<>() { @Override public String toString() { return "bloodmagic:array"; } });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlchemyArrayRecipe>> ARRAY_SERIALIZER = SERIALIZERS.register(
            "array", () -> new RecipeSerializer<>(AlchemyArrayRecipe.CODEC, AlchemyArrayRecipe.STREAM_CODEC));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
    public static final DeferredHolder<RecipeType<?>, RecipeType<AlchemyTableRecipe>> TABLE_TYPE = TYPES.register("alchemytable",
            () -> new RecipeType<>() { @Override public String toString() { return "bloodmagic:alchemytable"; } });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlchemyTableRecipe>> TABLE_SERIALIZER = SERIALIZERS.register(
            "alchemytable", () -> new RecipeSerializer<>(AlchemyTableRecipe.CODEC, AlchemyTableRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeType<?>, RecipeType<HellfireForgeRecipe>> FORGE_TYPE = TYPES.register("soulforge", () -> new RecipeType<>() { @Override public String toString() { return "bloodmagic:soulforge"; } });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HellfireForgeRecipe>> FORGE_SERIALIZER = SERIALIZERS.register("soulforge", () -> new RecipeSerializer<>(HellfireForgeRecipe.CODEC, HellfireForgeRecipe.STREAM_CODEC));
}
