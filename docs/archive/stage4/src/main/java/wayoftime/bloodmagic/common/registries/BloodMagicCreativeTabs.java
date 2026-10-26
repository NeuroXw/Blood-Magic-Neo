package wayoftime.bloodmagic.common.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.item.BloodMagicItems;

public final class BloodMagicCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BloodMagic.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOODMAGIC =
            CREATIVE_TABS.register("bloodmagic", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bloodmagic.creativeTab"))
                    .icon(() -> BloodMagicItems.BLOOD_ALTAR_ITEM.get().getDefaultInstance())
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((parameters, output) -> {
                        BloodMagicItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                        BloodMagicItems.BASICITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                    })
                    .build());

    // TODO: Follow-up - restore bloodmagic_upgrades, bloodmagic_decorative and component-based variants.
    private BloodMagicCreativeTabs() {
    }
}
