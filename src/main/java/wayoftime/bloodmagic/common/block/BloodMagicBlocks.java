package wayoftime.bloodmagic.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.block.enums.BloodRuneType;

public final class BloodMagicBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BloodMagic.MODID);
    public static final DeferredRegister.Blocks BASICBLOCKS = DeferredRegister.createBlocks(BloodMagic.MODID);
    public static final DeferredRegister<MapCodec<? extends Block>> BLOCK_TYPES =
            DeferredRegister.create(Registries.BLOCK_TYPE, BloodMagic.MODID);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockBloodRune>> BLOOD_RUNE_TYPE =
            BLOCK_TYPES.register("blood_rune", () -> BlockBloodRune.CODEC);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockAltar>> ALTAR_BLOCK_TYPE =
            BLOCK_TYPES.register("altar", () -> BlockAltar.CODEC);

    // Upstream EFFICIENCY has no registered block. Do not invent an efficiencyrune ID.
    public static final DeferredBlock<BlockBloodRune> BLANK_RUNE = registerRune("blankrune", BloodRuneType.BLANK, 1);
    public static final DeferredBlock<BlockBloodRune> SPEED_RUNE = registerRune("speedrune", BloodRuneType.SPEED, 1);
    public static final DeferredBlock<BlockBloodRune> SACRIFICE_RUNE = registerRune("sacrificerune", BloodRuneType.SACRIFICE, 1);
    public static final DeferredBlock<BlockBloodRune> SELF_SACRIFICE_RUNE = registerRune("selfsacrificerune", BloodRuneType.SELF_SACRIFICE, 1);
    public static final DeferredBlock<BlockBloodRune> DISPLACEMENT_RUNE = registerRune("dislocationrune", BloodRuneType.DISPLACEMENT, 1);
    public static final DeferredBlock<BlockBloodRune> CAPACITY_RUNE = registerRune("altarcapacityrune", BloodRuneType.CAPACITY, 1);
    public static final DeferredBlock<BlockBloodRune> AUGMENTED_CAPACITY_RUNE = registerRune("bettercapacityrune", BloodRuneType.AUGMENTED_CAPACITY, 1);
    public static final DeferredBlock<BlockBloodRune> ORB_RUNE = registerRune("orbcapacityrune", BloodRuneType.ORB, 1);
    public static final DeferredBlock<BlockBloodRune> ACCELERATION_RUNE = registerRune("accelerationrune", BloodRuneType.ACCELERATION, 1);
    public static final DeferredBlock<BlockBloodRune> CHARGING_RUNE = registerRune("chargingrune", BloodRuneType.CHARGING, 1);
    public static final DeferredBlock<BlockBloodRune> SELF_SACRIFICE_RUNE_2 = registerRune("selfsacrificerune2", BloodRuneType.SELF_SACRIFICE, 2);
    public static final DeferredBlock<BlockBloodRune> SPEED_RUNE_2 = registerRune("speedrune2", BloodRuneType.SPEED, 2);
    public static final DeferredBlock<BlockBloodRune> SACRIFICE_RUNE_2 = registerRune("sacrificerune2", BloodRuneType.SACRIFICE, 2);
    public static final DeferredBlock<BlockBloodRune> DISPLACEMENT_RUNE_2 = registerRune("dislocationrune2", BloodRuneType.DISPLACEMENT, 2);
    public static final DeferredBlock<BlockBloodRune> CAPACITY_RUNE_2 = registerRune("altarcapacityrune2", BloodRuneType.CAPACITY, 2);
    public static final DeferredBlock<BlockBloodRune> AUGMENTED_CAPACITY_RUNE_2 = registerRune("bettercapacityrune2", BloodRuneType.AUGMENTED_CAPACITY, 2);
    public static final DeferredBlock<BlockBloodRune> ORB_RUNE_2 = registerRune("orbcapacityrune2", BloodRuneType.ORB, 2);
    public static final DeferredBlock<BlockBloodRune> ACCELERATION_RUNE_2 = registerRune("accelerationrune2", BloodRuneType.ACCELERATION, 2);
    public static final DeferredBlock<BlockBloodRune> CHARGING_RUNE_2 = registerRune("chargingrune2", BloodRuneType.CHARGING, 2);

    public static final DeferredBlock<Block> BLOODSTONE =
            BASICBLOCKS.registerSimpleBlock("largebloodstonebrick", BloodMagicBlocks::stoneProperties);
    public static final DeferredBlock<Block> BLOODSTONE_BRICK =
            BASICBLOCKS.registerSimpleBlock("bloodstonebrick", BloodMagicBlocks::stoneProperties);
    // Required by the upstream tier-five capstones and archmage orb recipe.
    public static final DeferredBlock<Block> HELLFORGED_BLOCK =
            BASICBLOCKS.registerSimpleBlock("dungeon_metal", properties -> properties
                    .strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<BlockAltar> BLOOD_ALTAR =
            BLOCKS.registerBlock("altar", BlockAltar::new, properties -> stoneProperties(properties).noOcclusion());

    // TODO: Follow-up - migrate remaining machines, ritual/dungeon blocks and their menus with their subsystems.
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockAlchemyArray>> ARRAY_BLOCK_TYPE =
            BLOCK_TYPES.register("alchemyarray", () -> BlockAlchemyArray.CODEC);
    public static final DeferredBlock<BlockAlchemyArray> ALCHEMY_ARRAY = BLOCKS.registerBlock("alchemyarray", BlockAlchemyArray::new,
            p -> p.strength(0.2F).noCollision().noOcclusion().noLootTable());
    private BloodMagicBlocks() {
    }
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockAlchemyTable>> TABLE_BLOCK_TYPE = BLOCK_TYPES.register("alchemytable", () -> BlockAlchemyTable.CODEC);
    public static final DeferredBlock<BlockAlchemyTable> ALCHEMY_TABLE = BLOCKS.registerBlock("alchemytable", BlockAlchemyTable::new, p -> stoneProperties(p).noOcclusion());
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<BlockHellfireForge>> FORGE_BLOCK_TYPE = BLOCK_TYPES.register("soulforge", () -> BlockHellfireForge.CODEC);
    public static final DeferredBlock<BlockHellfireForge> HELLFIRE_FORGE = BLOCKS.registerBlock("soulforge", BlockHellfireForge::new,
            properties -> stoneProperties(properties).noOcclusion());
    public static final DeferredBlock<Block> RITUAL_STONE = BASICBLOCKS.registerSimpleBlock("ritualstone",BloodMagicBlocks::stoneProperties);
    public static final DeferredBlock<Block> WATER_RITUAL_STONE = BASICBLOCKS.registerSimpleBlock("waterritualstone",BloodMagicBlocks::stoneProperties);
    public static final DeferredBlock<Block> EARTH_RITUAL_STONE = BASICBLOCKS.registerSimpleBlock("earthritualstone",BloodMagicBlocks::stoneProperties);
    public static final DeferredBlock<Block> AIR_RITUAL_STONE = BASICBLOCKS.registerSimpleBlock("airritualstone",BloodMagicBlocks::stoneProperties);
    public static final DeferredBlock<Block> FIRE_RITUAL_STONE = BASICBLOCKS.registerSimpleBlock("fireritualstone",BloodMagicBlocks::stoneProperties);
    public static final DeferredBlock<Block> DUSK_RITUAL_STONE = BASICBLOCKS.registerSimpleBlock("duskritualstone",BloodMagicBlocks::stoneProperties);
    public static final DeferredHolder<MapCodec<? extends Block>,MapCodec<BlockMasterRitual>> MASTER_RITUAL_TYPE=BLOCK_TYPES.register("masterritualstone",()->BlockMasterRitual.CODEC);
    public static final DeferredBlock<BlockMasterRitual> MASTER_RITUAL_STONE=BLOCKS.registerBlock("masterritualstone",BlockMasterRitual::new,BloodMagicBlocks::stoneProperties);

    private static DeferredBlock<BlockBloodRune> registerRune(String name, BloodRuneType type, int strength) {
        // registerBlock sets Properties.setId before calling this constructor.
        return BASICBLOCKS.registerBlock(name,
                properties -> new BlockBloodRune(properties, type, strength), BloodMagicBlocks::stoneProperties);
    }

    private static BlockBehaviour.Properties stoneProperties(BlockBehaviour.Properties properties) {
        return properties.strength(2.0F, 5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_TYPES.register(modEventBus);
        BLOCKS.register(modEventBus);
        BASICBLOCKS.register(modEventBus);
    }
}
