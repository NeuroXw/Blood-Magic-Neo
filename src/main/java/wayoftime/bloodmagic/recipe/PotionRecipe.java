package wayoftime.bloodmagic.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.item.potion.ItemAlchemyFlask;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;

/** Unified 26.1 potion/flask operation backed by POTION_CONTENTS data components. */
public record PotionRecipe(Operation operation, List<Ingredient> input, Optional<Identifier> effect,
                           List<EffectSpec> inputEffects, List<EffectSpec> outputEffects,
                           Optional<ItemStackTemplate> output, int baseDuration, int amplifier,
                           double durationModifier, double amplifierDurationModifier, int maxEffects, int count,
                           int syphon, int ticks, int minimumTier) implements Recipe<MachineRecipeInput> {
    public enum Operation implements StringRepresentable {
        EFFECT("effect"), FILL("fill"), TRANSFORM("transform"), LENGTH("length"), POTENCY("potency"), FLASK("flask"), CYCLE("cycle");
        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);
        private final String name;
        Operation(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public record EffectSpec(Identifier effect, int baseDuration, int amplifier) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("effect").forGetter(EffectSpec::effect),
                Codec.intRange(0, 1_000_000).optionalFieldOf("baseDuration", 1800).forGetter(EffectSpec::baseDuration),
                Codec.intRange(0, 255).optionalFieldOf("amplifier", 0).forGetter(EffectSpec::amplifier)
        ).apply(i, EffectSpec::new));
    }

    public static final MapCodec<PotionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Operation.CODEC.fieldOf("operation").forGetter(PotionRecipe::operation),
            Ingredient.CODEC.listOf(1, 5).fieldOf("input").forGetter(PotionRecipe::input),
            Identifier.CODEC.optionalFieldOf("effect").forGetter(PotionRecipe::effect),
            Identifier.CODEC.listOf().optionalFieldOf("inputEffect", List.of()).xmap(
                    ids -> ids.stream().map(id -> new EffectSpec(id, 1, 0)).toList(), specs -> specs.stream().map(EffectSpec::effect).toList()).forGetter(PotionRecipe::inputEffects),
            EffectSpec.CODEC.listOf().optionalFieldOf("outputEffect", List.of()).forGetter(PotionRecipe::outputEffects),
            ItemStackTemplate.CODEC.optionalFieldOf("output").forGetter(PotionRecipe::output),
            Codec.intRange(0, 1_000_000).optionalFieldOf("baseDuration", 1800).forGetter(PotionRecipe::baseDuration),
            Codec.intRange(0, 255).optionalFieldOf("amplifier", 0).forGetter(PotionRecipe::amplifier),
            Codec.DOUBLE.optionalFieldOf("lengthDurMod", 1.0D).forGetter(PotionRecipe::durationModifier),
            Codec.DOUBLE.optionalFieldOf("ampDurMod", 1.0D).forGetter(PotionRecipe::amplifierDurationModifier),
            Codec.intRange(1, 64).optionalFieldOf("max", 1).forGetter(PotionRecipe::maxEffects),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(PotionRecipe::count),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("syphon").forGetter(PotionRecipe::syphon),
            Codec.intRange(1, 72000).optionalFieldOf("ticks", 200).forGetter(PotionRecipe::ticks),
            Codec.intRange(0, 5).optionalFieldOf("upgradeLevel", 0).forGetter(PotionRecipe::minimumTier)
    ).apply(i, PotionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PotionRecipe> STREAM_CODEC = StreamCodec.of(
            (b, r) -> {
                b.writeUtf(r.operation.getSerializedName()); b.writeVarInt(r.input.size()); r.input.forEach(i -> Ingredient.CONTENTS_STREAM_CODEC.encode(b, i));
                writeOptionalId(b, r.effect); writeSpecs(b, r.inputEffects); writeSpecs(b, r.outputEffects); b.writeBoolean(r.output.isPresent());
                if (r.output.isPresent()) ItemStackTemplate.STREAM_CODEC.encode(b, r.output.get());
                b.writeVarInt(r.baseDuration); b.writeVarInt(r.amplifier); b.writeDouble(r.durationModifier); b.writeDouble(r.amplifierDurationModifier);
                b.writeVarInt(r.maxEffects); b.writeVarInt(r.count); b.writeVarInt(r.syphon); b.writeVarInt(r.ticks); b.writeVarInt(r.minimumTier);
            },
            b -> {
                Operation operation = Operation.valueOf(b.readUtf().toUpperCase()); int size = b.readVarInt(); var ingredients = new ArrayList<Ingredient>();
                for (int i = 0; i < size; i++) ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(b));
                Optional<Identifier> effect = readOptionalId(b); List<EffectSpec> inputEffects = readSpecs(b), outputEffects = readSpecs(b);
                Optional<ItemStackTemplate> output = b.readBoolean() ? Optional.of(ItemStackTemplate.STREAM_CODEC.decode(b)) : Optional.empty();
                return new PotionRecipe(operation, ingredients, effect, inputEffects, outputEffects, output, b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
            });

    public PotionRecipe {
        input = List.copyOf(input); inputEffects = List.copyOf(inputEffects); outputEffects = List.copyOf(outputEffects);
        if (input.size() > 5 || syphon < 0 || ticks < 1 || minimumTier < 0 || minimumTier > 5 || durationModifier <= 0 || amplifierDurationModifier < 0) throw new IllegalArgumentException("Invalid potion recipe");
    }

    private static void writeOptionalId(RegistryFriendlyByteBuf b, Optional<Identifier> id) { b.writeBoolean(id.isPresent()); if (id.isPresent()) Identifier.STREAM_CODEC.encode(b, id.get()); }
    private static Optional<Identifier> readOptionalId(RegistryFriendlyByteBuf b) { return b.readBoolean() ? Optional.of(Identifier.STREAM_CODEC.decode(b)) : Optional.empty(); }
    private static void writeSpecs(RegistryFriendlyByteBuf b, List<EffectSpec> specs) { b.writeVarInt(specs.size()); for (EffectSpec s : specs) { Identifier.STREAM_CODEC.encode(b, s.effect()); b.writeVarInt(s.baseDuration()); b.writeVarInt(s.amplifier()); } }
    private static List<EffectSpec> readSpecs(RegistryFriendlyByteBuf b) { var result = new ArrayList<EffectSpec>(); int n = b.readVarInt(); for (int i = 0; i < n; i++) result.add(new EffectSpec(Identifier.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt())); return result; }

    @Override public boolean matches(MachineRecipeInput in, Level level) { return findFlaskSlot(in.items()) >= 0 && greedilyMatches(in.items()); }
    private boolean greedilyMatches(List<ItemStack> items) {
        boolean[] used = new boolean[items.size()];
        for (Ingredient ingredient : input) {
            boolean found = false;
            for (int i = 0; i < items.size(); i++) if (!used[i] && !isFlask(items.get(i)) && ingredient.test(items.get(i))) { used[i] = true; found = true; break; }
            if (!found) return false;
        }
        return true;
    }
    private int findFlaskSlot(List<ItemStack> items) { for (int i = 0; i < items.size(); i++) if (isFlask(items.get(i))) return i; return -1; }
    public static boolean isFlask(ItemStack stack) {
        return stack.getItem() instanceof ItemAlchemyFlask || stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    @Override public ItemStack assemble(MachineRecipeInput in) {
        int flaskSlot = findFlaskSlot(in.items()); if (flaskSlot < 0) return ItemStack.EMPTY;
        ItemStack flask = in.getItem(flaskSlot).copyWithCount(1);
        PotionContents contents = flask.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        List<MobEffectInstance> effects = new ArrayList<>(); contents.getAllEffects().forEach(effects::add);
        ItemStack result = flask;
        switch (operation) {
            case FILL -> {
                ItemStack source = in.items().stream().filter(s -> s != flask && s.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)).findFirst().orElse(ItemStack.EMPTY);
                if (!source.isEmpty()) contents = source.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                effects.clear(); for (MobEffectInstance effectInstance : contents.getAllEffects()) { if (effects.size() >= maxEffects) break; effects.add(effectInstance); }
                result = flask.is(Items.POTION) || flask.is(Items.SPLASH_POTION) || flask.is(Items.LINGERING_POTION) ? new ItemStack(wayoftime.bloodmagic.common.item.BloodMagicItems.ALCHEMY_FLASK.get()) : flask.copy();
            }
            case EFFECT -> { if (effect.isPresent()) applyEffect(effects, new EffectSpec(effect.get(), baseDuration, amplifier)); }
            case LENGTH -> effects.replaceAll(e -> e.getEffect().value() == effectValue() ? new MobEffectInstance(e.getEffect(), Math.max(1, (int) (e.getDuration() * durationModifier)), e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()) : e);
            case POTENCY -> effects.replaceAll(e -> e.getEffect().value() == effectValue() ? new MobEffectInstance(e.getEffect(), Math.max(1, (int) (e.getDuration() * amplifierDurationModifier)), Math.min(255, e.getAmplifier() + amplifier), e.isAmbient(), e.isVisible(), e.showIcon()) : e);
            case TRANSFORM -> {
                effects.removeIf(e -> inputEffects.stream().anyMatch(s -> e.getEffect().is(BuiltInRegistries.MOB_EFFECT.get(s.effect()).orElse(null))));
                outputEffects.forEach(s -> applyEffect(effects, s));
            }
            case FLASK -> { if (output.isPresent()) { result = output.get().create(); result.set(net.minecraft.core.component.DataComponents.DAMAGE, flask.getDamageValue()); } }
            case CYCLE -> { if (effects.size() > 1) java.util.Collections.rotate(effects, -Math.floorMod(count, effects.size())); }
        }
        int color = contents.getColorOr(PotionContents.BASE_POTION_COLOR);
        var next = new PotionContents(Optional.empty(), Optional.of(color), effects, Optional.empty());
        result.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, next);
        result.set(wayoftime.bloodmagic.common.registries.BloodMagicDataComponents.FLASK_FILLED.get(), true);
        return result;
    }

    private net.minecraft.world.effect.MobEffect effectValue() { return effect.map(id -> BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null)).orElse(null); }
    private static void applyEffect(List<MobEffectInstance> effects, EffectSpec spec) {
        BuiltInRegistries.MOB_EFFECT.get(spec.effect()).ifPresent(holder -> effects.add(new MobEffectInstance(holder, spec.baseDuration(), spec.amplifier())));
    }

    public void consumeIngredients(List<ItemStack> items) {
        boolean[] consumed = new boolean[items.size()];
        for (Ingredient ingredient : input) for (int i = 0; i < items.size(); i++) {
            if (!consumed[i] && !isFlask(items.get(i)) && ingredient.test(items.get(i))) { items.get(i).shrink(1); consumed[i] = true; break; }
        }
    }
    public boolean sameDefinition(PotionRecipe other) {
        return other != null && operation == other.operation && input.equals(other.input) && effect.equals(other.effect)
                && inputEffects.equals(other.inputEffects) && outputEffects.equals(other.outputEffects) && output.equals(other.output)
                && baseDuration == other.baseDuration && amplifier == other.amplifier && maxEffects == other.maxEffects && count == other.count
                && syphon == other.syphon && ticks == other.ticks && minimumTier == other.minimumTier;
    }
    @Override public RecipeSerializer<PotionRecipe> getSerializer() {
        return switch (operation) {
            case EFFECT -> BloodMagicRecipes.POTION_EFFECT_SERIALIZER.get();
            case FILL -> BloodMagicRecipes.POTION_FILL_SERIALIZER.get();
            case TRANSFORM -> BloodMagicRecipes.POTION_TRANSFORM_SERIALIZER.get();
            case LENGTH -> BloodMagicRecipes.POTION_LENGTH_SERIALIZER.get();
            case POTENCY -> BloodMagicRecipes.POTION_POTENCY_SERIALIZER.get();
            case FLASK -> BloodMagicRecipes.POTION_FLASK_TRANSFORM_SERIALIZER.get();
            case CYCLE -> BloodMagicRecipes.POTION_CYCLE_SERIALIZER.get();
        };
    }
    @Override public RecipeType<PotionRecipe> getType() { return BloodMagicRecipes.POTION_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}




