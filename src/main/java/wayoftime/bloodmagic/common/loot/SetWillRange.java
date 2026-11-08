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

/** Upstream set_will_range: set a raw will item to a random amount. */
public final class SetWillRange extends LootItemConditionalFunction {
    public static final MapCodec<SetWillRange> CODEC = RecordCodecBuilder.mapCodec(i ->
            commonFields(i).and(UniformGenerator.MAP_CODEC.fieldOf("damage").forGetter(f -> f.range))
                    .apply(i, SetWillRange::new));
    private final UniformGenerator range;
    public SetWillRange(List<LootItemCondition> conditions, UniformGenerator range) { super(conditions); this.range = range; }
    @Override public MapCodec<? extends LootItemConditionalFunction> codec() { return CODEC; }
    @Override protected ItemStack run(ItemStack stack, LootContext context) {
        if (stack.getItem() instanceof ItemDemonWill) ItemDemonWill.setAmount(stack, Math.clamp(range.getFloat(context), 0, ((ItemDemonWill) stack.getItem()).capacity()));
        return stack;
    }
}
