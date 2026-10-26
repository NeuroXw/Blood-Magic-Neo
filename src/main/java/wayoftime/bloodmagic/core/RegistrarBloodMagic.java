package wayoftime.bloodmagic.core;

import net.neoforged.bus.api.IEventBus;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.registries.BloodMagicCreativeTabs;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.common.registries.BloodMagicMenus;
import wayoftime.bloodmagic.common.registries.BloodMagicBloodOrbs;
import wayoftime.bloodmagic.network.BloodMagicNetwork;

/** The single mod-bus registration entry point for the staged NeoForge port. */
public final class RegistrarBloodMagic {
    private RegistrarBloodMagic() {
    }

    public static void register(IEventBus modEventBus) {
        BloodMagicBloodOrbs.ORBS.register(modEventBus);
        BloodMagicDataComponents.DATA_COMPONENTS.register(modEventBus);
        BloodMagicBlocks.register(modEventBus);
        BloodMagicBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        BloodMagicItems.register(modEventBus);
        BloodMagicCreativeTabs.CREATIVE_TABS.register(modEventBus);
        BloodMagicRecipes.register(modEventBus);
        BloodMagicMenus.MENUS.register(modEventBus);
        wayoftime.bloodmagic.common.registries.BloodMagicWill.ENTITIES.register(modEventBus);
        wayoftime.bloodmagic.common.registries.BloodMagicWill.EFFECTS.register(modEventBus);
        modEventBus.addListener(BloodMagicNetwork::registerPayloads);
        // TODO: Follow-up - fluid registration.
    }
}
