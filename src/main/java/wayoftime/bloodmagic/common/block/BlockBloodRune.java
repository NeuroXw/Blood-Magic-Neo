package wayoftime.bloodmagic.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import wayoftime.bloodmagic.altar.IBloodRune;
import wayoftime.bloodmagic.block.enums.BloodRuneType;

public class BlockBloodRune extends Block implements IBloodRune {
    public static final MapCodec<BlockBloodRune> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BloodRuneType.CODEC.fieldOf("rune_type").forGetter(block -> block.type),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("rune_strength", 1).forGetter(block -> block.strength),
            propertiesCodec()
    ).apply(instance, (type, strength, properties) -> new BlockBloodRune(properties, type, strength)));

    private final BloodRuneType type;
    private final int strength;

    public BlockBloodRune(Properties properties, BloodRuneType type) {
        this(properties, type, 1);
    }

    public BlockBloodRune(Properties properties, BloodRuneType type, int strength) {
        super(properties);
        this.type = type;
        this.strength = strength;
    }

    @Override
    protected MapCodec<? extends BlockBloodRune> codec() {
        return CODEC;
    }

    @Override
    public BloodRuneType getBloodRune(Level world, BlockPos pos) {
        return type;
    }

    @Override
    public int getRuneCount(Level world, BlockPos pos) {
        return strength;
    }
}
