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
}
