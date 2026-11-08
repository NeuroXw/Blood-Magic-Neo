package wayoftime.bloodmagic.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;

public record AlchemyArrayRecipe(Ingredient baseInput, Ingredient addedInput, ItemStackTemplate result,
        Identifier texture, int ticks, int lpCost) implements Recipe<ArrayRecipeInput> {
    public static final Identifier DEFAULT_TEXTURE = BloodMagic.rl("textures/models/alchemyarrays/sightsigil.png");
    public static final MapCodec<AlchemyArrayRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("baseinput").forGetter(AlchemyArrayRecipe::baseInput),
            Ingredient.CODEC.fieldOf("addedinput").forGetter(AlchemyArrayRecipe::addedInput),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(AlchemyArrayRecipe::result),
            Identifier.CODEC.optionalFieldOf("texture", DEFAULT_TEXTURE).forGetter(AlchemyArrayRecipe::texture),
            Codec.intRange(1, 72000).optionalFieldOf("ticks", 200).forGetter(AlchemyArrayRecipe::ticks),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("lpCost", 0).forGetter(AlchemyArrayRecipe::lpCost)
    ).apply(i, AlchemyArrayRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyArrayRecipe> STREAM_CODEC = StreamCodec.of(
            (b, r) -> { Ingredient.CONTENTS_STREAM_CODEC.encode(b, r.baseInput); Ingredient.CONTENTS_STREAM_CODEC.encode(b, r.addedInput);
                ItemStackTemplate.STREAM_CODEC.encode(b, r.result); b.writeIdentifier(r.texture); b.writeVarInt(r.ticks); b.writeVarInt(r.lpCost); },
            b -> new AlchemyArrayRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(b), Ingredient.CONTENTS_STREAM_CODEC.decode(b),
                    ItemStackTemplate.STREAM_CODEC.decode(b), b.readIdentifier(), b.readVarInt(), b.readVarInt()));
    public AlchemyArrayRecipe {
        if (baseInput == null || addedInput == null || result == null || result.count() < 1 || result.item().value() == Items.AIR
                || texture == null || ticks < 1 || ticks > 72000 || lpCost < 0) throw new IllegalArgumentException("Invalid array recipe");
    }
    @Override public boolean matches(ArrayRecipeInput in, Level level) { return baseInput.test(in.base()) && addedInput.test(in.catalyst()); }
    @Override public ItemStack assemble(ArrayRecipeInput in) { return result.create(); }
    public boolean sameDefinition(AlchemyArrayRecipe other) {
        return other != null && RecipeSupport.same(baseInput, other.baseInput) && RecipeSupport.same(addedInput, other.addedInput)
                && result.equals(other.result) && texture.equals(other.texture) && ticks == other.ticks && lpCost == other.lpCost;
    }
    @Override public RecipeSerializer<AlchemyArrayRecipe> getSerializer() { return BloodMagicRecipes.ARRAY_SERIALIZER.get(); }
    @Override public RecipeType<AlchemyArrayRecipe> getType() { return BloodMagicRecipes.ARRAY_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
