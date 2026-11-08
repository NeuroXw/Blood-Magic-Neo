package wayoftime.bloodmagic.recipe;

import java.util.List;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;

public record HellfireForgeRecipe(List<Ingredient> input, ItemStackTemplate result, double minimumWill, double drain, int ticks)
        implements Recipe<MachineRecipeInput> {
    private static final Codec<Double> AMOUNT = Codec.DOUBLE.validate(d -> Double.isFinite(d) && d >= 0 && d <= 20480 ? DataResult.success(d) : DataResult.error(() -> "Invalid will cost"));
    public static final MapCodec<HellfireForgeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.listOf(1, 4).fieldOf("input").forGetter(HellfireForgeRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(HellfireForgeRecipe::result),
            AMOUNT.fieldOf("minimumDrain").forGetter(HellfireForgeRecipe::minimumWill),
            AMOUNT.fieldOf("drain").forGetter(HellfireForgeRecipe::drain),
            Codec.intRange(1, 72000).optionalFieldOf("ticks", 100).forGetter(HellfireForgeRecipe::ticks)
    ).apply(i, HellfireForgeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HellfireForgeRecipe> STREAM_CODEC = StreamCodec.of(
            (b, r) -> { b.writeVarInt(r.input.size()); r.input.forEach(ing -> Ingredient.CONTENTS_STREAM_CODEC.encode(b, ing)); ItemStackTemplate.STREAM_CODEC.encode(b, r.result); b.writeDouble(r.minimumWill); b.writeDouble(r.drain); b.writeVarInt(r.ticks); },
            b -> new HellfireForgeRecipe(read(b), ItemStackTemplate.STREAM_CODEC.decode(b), b.readDouble(), b.readDouble(), b.readVarInt()));
    private static List<Ingredient> read(RegistryFriendlyByteBuf b) {
        int size = b.readVarInt(); if (size < 1 || size > 4) throw new IllegalArgumentException("Invalid forge ingredient count");
        var list = new java.util.ArrayList<Ingredient>(); for (int i = 0; i < size; i++) list.add(Ingredient.CONTENTS_STREAM_CODEC.decode(b)); return list;
    }
    public HellfireForgeRecipe {
        input = List.copyOf(input);
        if (input.isEmpty() || input.size() > 4 || result == null || result.count() < 1 || result.item().value() == Items.AIR
                || !Double.isFinite(minimumWill) || !Double.isFinite(drain) || minimumWill < 0 || drain < 0 || minimumWill > 20480 || drain > 20480
                || ticks < 1 || ticks > 72000) throw new IllegalArgumentException("Invalid forge recipe");
    }
    @Override public boolean matches(MachineRecipeInput in, Level level) { return RecipeSupport.matches(input, in.items()); }
    @Override public ItemStack assemble(MachineRecipeInput in) { return result.create(); }
    public boolean sameDefinition(HellfireForgeRecipe other) { return other != null && RecipeSupport.same(input, other.input) && result.equals(other.result) && minimumWill == other.minimumWill && drain == other.drain && ticks == other.ticks; }
    @Override public RecipeSerializer<HellfireForgeRecipe> getSerializer() { return BloodMagicRecipes.FORGE_SERIALIZER.get(); }
    @Override public RecipeType<HellfireForgeRecipe> getType() { return BloodMagicRecipes.FORGE_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
