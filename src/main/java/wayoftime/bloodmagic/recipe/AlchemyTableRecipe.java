package wayoftime.bloodmagic.recipe;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;

public record AlchemyTableRecipe(List<Ingredient> input, ItemStackTemplate result, int syphon, int ticks, int minimumTier)
        implements Recipe<MachineRecipeInput> {
    public static final MapCodec<AlchemyTableRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.listOf(1, 6).fieldOf("input").forGetter(AlchemyTableRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(AlchemyTableRecipe::result),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("syphon").forGetter(AlchemyTableRecipe::syphon),
            Codec.intRange(1, 72000).fieldOf("ticks").forGetter(AlchemyTableRecipe::ticks),
            Codec.intRange(1, 5).fieldOf("upgradeLevel").forGetter(AlchemyTableRecipe::minimumTier)
    ).apply(i, AlchemyTableRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyTableRecipe> STREAM_CODEC = StreamCodec.of(
            (b, r) -> { b.writeVarInt(r.input.size()); r.input.forEach(ing -> Ingredient.CONTENTS_STREAM_CODEC.encode(b, ing));
                ItemStackTemplate.STREAM_CODEC.encode(b, r.result); b.writeVarInt(r.syphon); b.writeVarInt(r.ticks); b.writeVarInt(r.minimumTier); },
            b -> new AlchemyTableRecipe(readIngredients(b), ItemStackTemplate.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    private static List<Ingredient> readIngredients(RegistryFriendlyByteBuf b) {
        int count = b.readVarInt();
        if (count < 1 || count > 6) throw new IllegalArgumentException("Invalid ingredient count");
        var result = new java.util.ArrayList<Ingredient>();
        for (int i = 0; i < count; i++) result.add(Ingredient.CONTENTS_STREAM_CODEC.decode(b));
        return result;
    }
    public AlchemyTableRecipe {
        input = List.copyOf(input);
        if (input.isEmpty() || input.size() > 6 || result == null || result.count() < 1 || result.item().value() == Items.AIR
                || syphon < 0 || ticks < 1 || ticks > 72000 || minimumTier < 1 || minimumTier > 5) throw new IllegalArgumentException("Invalid table recipe");
    }
    @Override public boolean matches(MachineRecipeInput in, Level level) { return RecipeSupport.matches(input, in.items()); }
    @Override public ItemStack assemble(MachineRecipeInput in) { return result.create(); }
    public boolean sameDefinition(AlchemyTableRecipe other) {
        return other != null && RecipeSupport.same(input, other.input) && result.equals(other.result)
                && syphon == other.syphon && ticks == other.ticks && minimumTier == other.minimumTier;
    }
    @Override public RecipeSerializer<AlchemyTableRecipe> getSerializer() { return BloodMagicRecipes.TABLE_SERIALIZER.get(); }
    @Override public RecipeType<AlchemyTableRecipe> getType() { return BloodMagicRecipes.TABLE_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
