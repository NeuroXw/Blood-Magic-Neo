package wayoftime.bloodmagic.common.item;

import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.block.BlockBloodRune;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.item.block.ItemBlockBloodRune;

public final class BloodMagicItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BloodMagic.MODID);
    public static final DeferredRegister.Items BASICITEMS = DeferredRegister.createItems(BloodMagic.MODID);

    public static final DeferredItem<ItemBlockBloodRune> BLANK_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.BLANK_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> SPEED_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.SPEED_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> SACRIFICE_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.SACRIFICE_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> SELF_SACRIFICE_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.SELF_SACRIFICE_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> DISPLACEMENT_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.DISPLACEMENT_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> CAPACITY_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.CAPACITY_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> AUGMENTED_CAPACITY_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.AUGMENTED_CAPACITY_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> ORB_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.ORB_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> ACCELERATION_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.ACCELERATION_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> CHARGING_RUNE_ITEM = registerRuneItem(BloodMagicBlocks.CHARGING_RUNE);
    public static final DeferredItem<ItemBlockBloodRune> SELF_SACRIFICE_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.SELF_SACRIFICE_RUNE_2);
    public static final DeferredItem<ItemBlockBloodRune> SPEED_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.SPEED_RUNE_2);
    public static final DeferredItem<ItemBlockBloodRune> SACRIFICE_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.SACRIFICE_RUNE_2);
    public static final DeferredItem<ItemBlockBloodRune> DISPLACEMENT_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.DISPLACEMENT_RUNE_2);
    public static final DeferredItem<ItemBlockBloodRune> CAPACITY_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.CAPACITY_RUNE_2);
    public static final DeferredItem<ItemBlockBloodRune> AUGMENTED_CAPACITY_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.AUGMENTED_CAPACITY_RUNE_2);
    public static final DeferredItem<ItemBlockBloodRune> ORB_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.ORB_RUNE_2);
    public static final DeferredItem<ItemBlockBloodRune> ACCELERATION_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.ACCELERATION_RUNE_2);
    public static final DeferredItem<ItemBlockBloodRune> CHARGING_RUNE_2_ITEM = registerRuneItem(BloodMagicBlocks.CHARGING_RUNE_2);

    public static final DeferredItem<BlockItem> BLOODSTONE_ITEM = ITEMS.registerSimpleBlockItem(BloodMagicBlocks.BLOODSTONE);
    public static final DeferredItem<BlockItem> BLOODSTONE_BRICK_ITEM = ITEMS.registerSimpleBlockItem(BloodMagicBlocks.BLOODSTONE_BRICK);
    public static final DeferredItem<BlockItem> HELLFORGED_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(BloodMagicBlocks.HELLFORGED_BLOCK);
    public static final DeferredItem<BlockItem> BLOOD_ALTAR_ITEM = ITEMS.registerSimpleBlockItem(BloodMagicBlocks.BLOOD_ALTAR,
            properties -> ItemBase.withDescription(properties, "stage2.altar"));

    // All active upstream ItemBase registrations, with original IDs and stack limits.
    public static final DeferredItem<ItemBase> SLATE = BASICITEMS.registerItem("blankslate", ItemBase::new);
    public static final DeferredItem<ItemBase> REINFORCED_SLATE = BASICITEMS.registerItem("reinforcedslate", ItemBase::new);
    public static final DeferredItem<ItemBase> IMBUED_SLATE = BASICITEMS.registerItem("infusedslate", ItemBase::new);
    public static final DeferredItem<ItemBase> DEMONIC_SLATE = BASICITEMS.registerItem("demonslate", ItemBase::new);
    public static final DeferredItem<ItemBase> ETHEREAL_SLATE = BASICITEMS.registerItem("etherealslate", ItemBase::new);
    public static final DeferredItem<ItemBase> WEAK_BLOOD_SHARD = BASICITEMS.registerItem("weakbloodshard", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_WATER = BASICITEMS.registerItem("reagentwater", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_LAVA = BASICITEMS.registerItem("reagentlava", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_VOID = BASICITEMS.registerItem("reagentvoid", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_GROWTH = BASICITEMS.registerItem("reagentgrowth", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_FAST_MINER = BASICITEMS.registerItem("reagentfastminer", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_MAGNETISM = BASICITEMS.registerItem("reagentmagnetism", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_AIR = BASICITEMS.registerItem("reagentair", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_BLOOD_LIGHT = BASICITEMS.registerItem("reagentbloodlight", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_SIGHT = BASICITEMS.registerItem("reagentsight", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_BINDING = BASICITEMS.registerItem("reagentbinding", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_HOLDING = BASICITEMS.registerItem("reagentholding", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_SUPPRESSION = BASICITEMS.registerItem("reagentsuppression", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_TELEPOSITION = BASICITEMS.registerItem("reagentteleposition", ItemBase::new);
    public static final DeferredItem<ItemBase> REAGENT_FROST = BASICITEMS.registerItem("reagentfrost", ItemBase::new);
    public static final DeferredItem<ItemBase> HELLFORGED_PARTS = BASICITEMS.registerItem("hellforgedparts", properties -> new ItemBase(properties, "hellforgedparts"));
    public static final DeferredItem<ItemBase> SULFUR = BASICITEMS.registerItem("sulfur", ItemBase::new);
    public static final DeferredItem<ItemBase> SALTPETER = BASICITEMS.registerItem("saltpeter", ItemBase::new);
    public static final DeferredItem<ItemBase> PLANT_OIL = BASICITEMS.registerItem("plantoil", ItemBase::new);
    public static final DeferredItem<ItemBase> TAU_OIL = BASICITEMS.registerItem("tauoil", ItemBase::new);
    public static final DeferredItem<ItemBase> SIMPLE_CATALYST = BASICITEMS.registerItem("simplecatalyst", ItemBase::new);
    public static final DeferredItem<ItemBase> STRENGTHENED_CATALYST = BASICITEMS.registerItem("strengthenedcatalyst", ItemBase::new);
    public static final DeferredItem<ItemBase> MUNDANE_POWER_CATALYST = BASICITEMS.registerItem("mundanepowercatalyst", ItemBase::new);
    public static final DeferredItem<ItemBase> MUNDANE_LENGTHENING_CATALYST = BASICITEMS.registerItem("mundanelengtheningcatalyst", ItemBase::new);
    public static final DeferredItem<ItemBase> COMBINATIONAL_CATALYST = BASICITEMS.registerItem("combinationalcatalyst", ItemBase::new);
    public static final DeferredItem<ItemBase> WEAK_FILLING_AGENT = BASICITEMS.registerItem("weakfillingagent", ItemBase::new);
    public static final DeferredItem<ItemBase> CYCLING_CATALYST = BASICITEMS.registerItem("cyclingcatalyst", ItemBase::new);
    public static final DeferredItem<ItemBase> AVERAGE_POWER_CATALYST = BASICITEMS.registerItem("averagepowercatalyst", ItemBase::new);
    public static final DeferredItem<ItemBase> AVERAGE_LENGTHENING_CATALYST = BASICITEMS.registerItem("averagelengtheningcatalyst", ItemBase::new);
    public static final DeferredItem<ItemBase> AVERAGE_FILLING_AGENT = BASICITEMS.registerItem("standardfillingagent", ItemBase::new);
    public static final DeferredItem<ItemBase> SLATE_VIAL = ITEMS.registerItem("slate_vial", properties -> new ItemBase(properties, 16, "slate_vial"));
    public static final DeferredItem<ItemBase> IRON_FRAGMENT = BASICITEMS.registerItem("ironfragment", ItemBase::new);
    public static final DeferredItem<ItemBase> GOLD_FRAGMENT = BASICITEMS.registerItem("goldfragment", ItemBase::new);
    public static final DeferredItem<ItemBase> COPPER_FRAGMENT = BASICITEMS.registerItem("copperfragment", ItemBase::new);
    public static final DeferredItem<ItemBase> NETHERITE_SCRAP_FRAGMENT = BASICITEMS.registerItem("fragment_netherite_scrap", ItemBase::new);
    public static final DeferredItem<ItemBase> DEMONITE_FRAGMENT = BASICITEMS.registerItem("demonitefragment", ItemBase::new);
    public static final DeferredItem<ItemBase> IRON_GRAVEL = BASICITEMS.registerItem("irongravel", ItemBase::new);
    public static final DeferredItem<ItemBase> GOLD_GRAVEL = BASICITEMS.registerItem("goldgravel", ItemBase::new);
    public static final DeferredItem<ItemBase> COPPER_GRAVEL = BASICITEMS.registerItem("coppergravel", ItemBase::new);
    public static final DeferredItem<ItemBase> NETHERITE_SCRAP_GRAVEL = BASICITEMS.registerItem("gravel_netherite_scrap", ItemBase::new);
    public static final DeferredItem<ItemBase> DEMONITE_GRAVEL = BASICITEMS.registerItem("demonitegravel", ItemBase::new);
    public static final DeferredItem<ItemBase> IRON_SAND = BASICITEMS.registerItem("ironsand", ItemBase::new);
    public static final DeferredItem<ItemBase> GOLD_SAND = BASICITEMS.registerItem("goldsand", ItemBase::new);
    public static final DeferredItem<ItemBase> COPPER_SAND = BASICITEMS.registerItem("coppersand", ItemBase::new);
    public static final DeferredItem<ItemBase> COAL_SAND = BASICITEMS.registerItem("coalsand", ItemBase::new);
    public static final DeferredItem<ItemBase> NETHERITE_SCRAP_SAND = BASICITEMS.registerItem("sand_netherite", ItemBase::new);
    public static final DeferredItem<ItemBase> CORRUPTED_DUST = BASICITEMS.registerItem("corrupted_dust", ItemBase::new);
    public static final DeferredItem<ItemBase> CORRUPTED_DUST_TINY = BASICITEMS.registerItem("corrupted_tinydust", ItemBase::new);
    public static final DeferredItem<ItemBase> HELLFORGED_SAND = BASICITEMS.registerItem("sand_hellforged", ItemBase::new);
    public static final DeferredItem<ItemBase> DEMONITE_RAW = BASICITEMS.registerItem("rawdemonite", ItemBase::new);
    public static final DeferredItem<ItemBase> HELLFORGED_INGOT = BASICITEMS.registerItem("ingot_hellforged", ItemBase::new);

    // TODO: Follow-up - migrate the BloodOrb registry and soul-network interactions.
    public static final DeferredItem<ItemBloodOrb> WEAK_BLOOD_ORB = registerOrb("weakbloodorb", 1);
    public static final DeferredItem<ItemBloodOrb> APPRENTICE_BLOOD_ORB = registerOrb("apprenticebloodorb", 2);
    public static final DeferredItem<ItemBloodOrb> MAGICIAN_BLOOD_ORB = registerOrb("magicianbloodorb", 3);
    public static final DeferredItem<ItemBloodOrb> MASTER_BLOOD_ORB = registerOrb("masterbloodorb", 4);
    public static final DeferredItem<ItemBloodOrb> ARCHMAGE_BLOOD_ORB = registerOrb("archmagebloodorb", 5);

    // TODO: Follow-up - migrate sigils, equipment, fluid buckets, routing and dungeon items with their subsystems.
    private BloodMagicItems() {
    }

    private static DeferredItem<ItemBlockBloodRune> registerRuneItem(DeferredBlock<BlockBloodRune> block) {
        // Read only the holder ID here; defer block.get() until the item registration event.
        return ITEMS.registerItem(block.getId().getPath(),
                properties -> new ItemBlockBloodRune(block.get(), properties.useBlockDescriptionPrefix()));
    }

    private static DeferredItem<ItemBloodOrb> registerOrb(String name, int tier) {
        return BASICITEMS.registerItem(name, properties -> new ItemBloodOrb(properties, tier));
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        BASICITEMS.register(modEventBus);
    }
}
