package wayoftime.bloodmagic.common.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;

public final class BloodMagicBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BloodMagic.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloodAltarBlockEntity>> BLOOD_ALTAR =
            BLOCK_ENTITY_TYPES.register("altar", () -> new BlockEntityType<>(
                    BloodAltarBlockEntity::new, BloodMagicBlocks.BLOOD_ALTAR.get()));

    private BloodMagicBlockEntities() {
    }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlchemyArrayBlockEntity>> ALCHEMY_ARRAY =
            BLOCK_ENTITY_TYPES.register("alchemyarray", () -> new BlockEntityType<>(AlchemyArrayBlockEntity::new, BloodMagicBlocks.ALCHEMY_ARRAY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlchemyTableBlockEntity>> ALCHEMY_TABLE =
            BLOCK_ENTITY_TYPES.register("alchemytable", () -> new BlockEntityType<>(AlchemyTableBlockEntity::new, BloodMagicBlocks.ALCHEMY_TABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HellfireForgeBlockEntity>> HELLFIRE_FORGE = BLOCK_ENTITY_TYPES.register("soulforge", () -> new BlockEntityType<>(HellfireForgeBlockEntity::new, BloodMagicBlocks.HELLFIRE_FORGE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MasterRitualBlockEntity>> MASTER_RITUAL=BLOCK_ENTITY_TYPES.register("masterritualstone",()->new BlockEntityType<>(MasterRitualBlockEntity::new,BloodMagicBlocks.MASTER_RITUAL_STONE.get()));
}
