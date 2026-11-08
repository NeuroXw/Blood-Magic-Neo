package wayoftime.bloodmagic.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import wayoftime.bloodmagic.common.item.ItemDemonWill;

/** Upstream set_will_fraction: set a gem's remaining charge from a random loss fraction. */
public final class SetWillFraction extends LootItemConditionalFunction {
    public static final MapCodec<SetWillFraction> CODEC = RecordCodecBuilder.mapCodec(i ->
            commonFields(i).and(UniformGenerator.MAP_CODEC.fieldOf("damage").forGetter(f -> f.range))
                    .apply(i, SetWillFraction::new));
    private final UniformGenerator range;
    public SetWillFraction(List<LootItemCondition> conditions, UniformGenerator range) { super(conditions); this.range = range; }
    @Override public MapCodec<? extends LootItemConditionalFunction> codec() { return CODEC; }
    @Override protected ItemStack run(ItemStack stack, LootContext context) {
        if (stack.getItem() instanceof ItemDemonWill gem && gem.isGem()) {
            double remaining = gem.capacity() * (1 - Math.clamp(range.getFloat(context), 0, 1));
            ItemDemonWill.setAmount(stack, remaining);
        }
        return stack;
    }
}
