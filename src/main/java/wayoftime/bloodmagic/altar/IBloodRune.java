package wayoftime.bloodmagic.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import wayoftime.bloodmagic.block.enums.BloodRuneType;

/** Blocks implementing this interface provide runes to the blood altar. */
public interface IBloodRune {
    @Nullable
    BloodRuneType getBloodRune(Level world, BlockPos pos);

    default int getRuneCount(Level world, BlockPos pos) {
        return 1;
    }
}
