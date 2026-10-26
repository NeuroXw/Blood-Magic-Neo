package wayoftime.bloodmagic;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.core.RegistrarBloodMagic;

@Mod(BloodMagic.MODID)
public final class BloodMagic {
    public static final String MODID = "bloodmagic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BloodMagic(IEventBus modEventBus) {
        RegistrarBloodMagic.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        LOGGER.info("Blood Magic stage 6 entry point loaded");
    }

    public static Identifier rl(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Blood Magic stage 6 common setup complete: {} blocks, {} items, {} block entity types, {} data components",
                BloodMagicBlocks.BLOCKS.getEntries().size() + BloodMagicBlocks.BASICBLOCKS.getEntries().size(),
                BloodMagicItems.ITEMS.getEntries().size() + BloodMagicItems.BASICITEMS.getEntries().size(),
                BloodMagicBlockEntities.BLOCK_ENTITY_TYPES.getEntries().size(),
                BloodMagicDataComponents.DATA_COMPONENTS.getEntries().size());
    }
}

