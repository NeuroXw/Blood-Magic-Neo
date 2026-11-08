package wayoftime.bloodmagic.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;

/** Native 26.1 recipe. Legacy upgradeLevel is zero based; runtime tiers are 1..6. */
public record BloodAltarRecipe(Ingredient input, ItemStackTemplate result, int minimumTier,
                               int syphon, int consumptionRate, int drainRate)
        implements Recipe<BloodAltarRecipeInput> {
    public static final MapCodec<BloodAltarRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(BloodAltarRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(BloodAltarRecipe::result),
            Codec.intRange(0, 5).xmap(tier -> tier + 1, tier -> tier - 1)
                    .fieldOf("upgradeLevel").forGetter(BloodAltarRecipe::minimumTier),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("altarSyphon").forGetter(BloodAltarRecipe::syphon),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("consumptionRate").forGetter(BloodAltarRecipe::consumptionRate),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("drainRate").forGetter(BloodAltarRecipe::drainRate)
    ).apply(instance, BloodAltarRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BloodAltarRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
                ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result);
                buffer.writeVarInt(recipe.minimumTier);
                buffer.writeVarInt(recipe.syphon);
                buffer.writeVarInt(recipe.consumptionRate);
                buffer.writeVarInt(recipe.drainRate);
            }, buffer -> new BloodAltarRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                    ItemStackTemplate.STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt()));

    public BloodAltarRecipe {
        // Item defaults are unavailable while datapacks load in 26.1. Do not create an ItemStack here.
        if (input == null || result == null || result.count() < 1 || result.item().value() == Items.AIR
                || minimumTier < 1 || minimumTier > 6 || syphon < 1 || consumptionRate < 1 || drainRate < 0) {
            throw new IllegalArgumentException("Invalid blood altar recipe");
        }
    }

    public BloodAltarRecipe(Ingredient input, ItemStack output, int minimumTier, int syphon, int consumptionRate, int drainRate) {
        this(input, ItemStackTemplate.fromNonEmptyStack(output), minimumTier, syphon, consumptionRate, drainRate);
    }

    public ItemStack output() {
        return result.create();
    }

    @Override
    public boolean matches(BloodAltarRecipeInput context, Level level) {
        int count = context.stack().getCount();
        return count > 0 && context.tier() >= minimumTier && input.test(context.stack())
                && (long) count * syphon <= Integer.MAX_VALUE
                && (long) count * result.count() <= output().getMaxStackSize();
    }

    public int totalBlood(BloodAltarRecipeInput context) {
        return Math.multiplyExact(context.stack().getCount(), syphon);
    }

    @Override
    public ItemStack assemble(BloodAltarRecipeInput context) {
        int count = Math.multiplyExact(context.stack().getCount(), result.count());
        ItemStack output = output();
        return count > 0 && count <= output.getMaxStackSize() ? output.copyWithCount(count) : ItemStack.EMPTY;
    }

    public boolean sameDefinition(BloodAltarRecipe other) {
        return other != null && sameIngredient(other.input) && result.equals(other.result)
                && minimumTier == other.minimumTier && syphon == other.syphon
                && consumptionRate == other.consumptionRate && drainRate == other.drainRate;
    }

    private boolean sameIngredient(Ingredient other) {
        if (input.equals(other)) return true;
        // A reload constructs new Named HolderSets. Compare tag keys, not their object identities.
        return !input.isCustom() && !other.isCustom() && input.getValues().unwrap().equals(other.getValues().unwrap());
    }

    @Override public RecipeSerializer<BloodAltarRecipe> getSerializer() { return BloodMagicRecipes.ALTAR_SERIALIZER.get(); }
    @Override public RecipeType<BloodAltarRecipe> getType() { return BloodMagicRecipes.ALTAR_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
