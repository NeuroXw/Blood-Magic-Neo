package wayoftime.bloodmagic.common.registries;

import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemDemonWill;

public final class BloodMagicCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BloodMagic.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOODMAGIC =
            CREATIVE_TABS.register("bloodmagic", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bloodmagic.creativeTab"))
                    .icon(() -> BloodMagicItems.BLOOD_ALTAR_ITEM.get().getDefaultInstance())
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((parameters, output) -> {
                        BloodMagicItems.ITEMS.getEntries().forEach(entry -> addVariants(entry.get(), output::accept));
                        BloodMagicItems.BASICITEMS.getEntries().forEach(entry -> addVariants(entry.get(), output::accept));
                        wayoftime.bloodmagic.common.block.DungeonDecorativeBlocks.ITEM_BY_ID.values().forEach(entry -> output.accept(entry.get()));
                    })
                    .build());

    // TODO: Follow-up - restore bloodmagic_upgrades, bloodmagic_decorative and component-based variants.
    private static void addVariants(Item item, Consumer<ItemStack> output) {
        if (item instanceof ItemDemonWill will) {
            if (will.isGem()) {
                for (EnumDemonWillType type : EnumDemonWillType.values()) {
                    ItemStack stack = new ItemStack(item);
                    ItemDemonWill.setWill(stack, type, will.capacity());
                    output.accept(stack);
                }
            } else {
                ItemStack stack = new ItemStack(item);
                ItemDemonWill.setWill(stack, ItemDemonWill.type(stack), 5);
                output.accept(stack);
            }
            return;
        }
        output.accept(item.getDefaultInstance());
    }

    private BloodMagicCreativeTabs() {
    }
}
