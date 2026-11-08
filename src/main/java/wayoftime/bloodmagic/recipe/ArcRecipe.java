package wayoftime.bloodmagic.recipe;

import java.util.List;
import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
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

/** Data-compatible port of upstream bloodmagic:arc recipes. */
public record ArcRecipe(Ingredient input, int inputSize, Ingredient tool, Optional<FluidSpec> inputFluid,
                        ItemStackTemplate output, Optional<FluidSpec> outputFluid, boolean consumeIngredient,
                        List<ExtraOutput> extraOutputs) implements Recipe<MachineRecipeInput> {
    public record FluidSpec(Identifier fluid, int amount) {
        public static final Codec<FluidSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("fluid").forGetter(FluidSpec::fluid),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidSpec::amount)
        ).apply(i, FluidSpec::new));
    }

    public record ExtraOutput(ItemStackTemplate stack, double chance, double mainChance) {
        public static final Codec<ExtraOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStackTemplate.CODEC.fieldOf("type").forGetter(ExtraOutput::stack),
                Codec.DOUBLE.optionalFieldOf("chance", 0.0D).forGetter(ExtraOutput::chance),
                Codec.DOUBLE.optionalFieldOf("mainchance", 0.0D).forGetter(ExtraOutput::mainChance)
        ).apply(i, ExtraOutput::new));
    }

    public static final MapCodec<ArcRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("input").forGetter(ArcRecipe::input),
            Codec.intRange(1, 64).optionalFieldOf("inputsize", 1).forGetter(ArcRecipe::inputSize),
            Ingredient.CODEC.fieldOf("tool").forGetter(ArcRecipe::tool),
            FluidSpec.CODEC.optionalFieldOf("inputFluid").forGetter(ArcRecipe::inputFluid),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(ArcRecipe::output),
            FluidSpec.CODEC.optionalFieldOf("outputFluid").forGetter(ArcRecipe::outputFluid),
            Codec.BOOL.optionalFieldOf("consumeingredient", true).forGetter(ArcRecipe::consumeIngredient),
            ExtraOutput.CODEC.listOf().optionalFieldOf("addedoutput", List.of()).forGetter(ArcRecipe::extraOutputs)
    ).apply(i, ArcRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArcRecipe> STREAM_CODEC = StreamCodec.of(
            (b, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(b, r.input); b.writeVarInt(r.inputSize);
                Ingredient.CONTENTS_STREAM_CODEC.encode(b, r.tool); writeFluid(b, r.inputFluid);
                ItemStackTemplate.STREAM_CODEC.encode(b, r.output); writeFluid(b, r.outputFluid); b.writeBoolean(r.consumeIngredient);
                b.writeVarInt(r.extraOutputs.size());
                for (ExtraOutput extra : r.extraOutputs) { ItemStackTemplate.STREAM_CODEC.encode(b, extra.stack()); b.writeDouble(extra.chance()); b.writeDouble(extra.mainChance()); }
            },
            b -> {
                Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(b); int size = b.readVarInt();
                Ingredient tool = Ingredient.CONTENTS_STREAM_CODEC.decode(b); Optional<FluidSpec> inputFluid = readFluid(b);
                ItemStackTemplate output = ItemStackTemplate.STREAM_CODEC.decode(b); Optional<FluidSpec> outputFluid = readFluid(b); boolean consume = b.readBoolean();
                int count = b.readVarInt(); var extras = new java.util.ArrayList<ExtraOutput>();
                for (int i = 0; i < count; i++) extras.add(new ExtraOutput(ItemStackTemplate.STREAM_CODEC.decode(b), b.readDouble(), b.readDouble()));
                return new ArcRecipe(input, size, tool, inputFluid, output, outputFluid, consume, extras);
            });

    public ArcRecipe {
        if (inputSize < 1 || inputSize > 64 || output == null || output.count() < 1 || output.item().value() == Items.AIR
                || inputFluid.stream().anyMatch(f -> f.amount() < 1) || outputFluid.stream().anyMatch(f -> f.amount() < 1)) {
            throw new IllegalArgumentException("Invalid ARC recipe");
        }
        extraOutputs = List.copyOf(extraOutputs);
    }

    private static void writeFluid(RegistryFriendlyByteBuf b, Optional<FluidSpec> spec) {
        b.writeBoolean(spec.isPresent()); if (spec.isPresent()) { Identifier.STREAM_CODEC.encode(b, spec.get().fluid()); b.writeVarInt(spec.get().amount()); }
    }
    private static Optional<FluidSpec> readFluid(RegistryFriendlyByteBuf b) {
        return b.readBoolean() ? Optional.of(new FluidSpec(Identifier.STREAM_CODEC.decode(b), b.readVarInt())) : Optional.empty();
    }

    @Override public boolean matches(MachineRecipeInput in, Level level) {
        return in.size() >= 2 && in.getItem(0).getCount() >= inputSize && input.test(in.getItem(0)) && tool.test(in.getItem(1));
    }
    @Override public ItemStack assemble(MachineRecipeInput in) { return output.create(); }
    public List<ItemStack> rollOutputs(RandomSource random) {
        var outputs = new java.util.ArrayList<ItemStack>(); outputs.add(output.create());
        for (ExtraOutput extra : extraOutputs) if (random.nextDouble() < extra.chance()) outputs.add(extra.stack().create());
        return outputs;
    }
    public boolean sameDefinition(ArcRecipe other) {
        return other != null && inputSize == other.inputSize && output.equals(other.output) && consumeIngredient == other.consumeIngredient
                && input.equals(other.input) && tool.equals(other.tool) && inputFluid.equals(other.inputFluid) && outputFluid.equals(other.outputFluid)
                && extraOutputs.equals(other.extraOutputs);
    }
    @Override public RecipeSerializer<ArcRecipe> getSerializer() { return BloodMagicRecipes.ARC_SERIALIZER.get(); }
    @Override public RecipeType<ArcRecipe> getType() { return BloodMagicRecipes.ARC_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
