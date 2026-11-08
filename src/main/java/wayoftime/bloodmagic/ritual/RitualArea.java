package wayoftime.bloodmagic.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Mutable rectangular ritual area using upstream's inclusive minimum / exclusive maximum convention. */
public final class RitualArea {
    public static final Codec<RitualArea> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("min").forGetter(RitualArea::minimum),
            BlockPos.CODEC.fieldOf("max").forGetter(RitualArea::maximum)
    ).apply(instance, RitualArea::new));

    private BlockPos minimum;
    private BlockPos maximum;

    public RitualArea(BlockPos minimum, BlockPos maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
        normalize();
    }

    public RitualArea(BlockPos minimum, int sizeX, int sizeY, int sizeZ) {
        this(minimum, minimum.offset(sizeX, sizeY, sizeZ));
    }

    public RitualArea(BlockPos minimum, int size) {
        this(minimum, size, size, size);
    }

    public static RitualArea between(BlockPos first, BlockPos second) {
        BlockPos min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()));
        BlockPos max = new BlockPos(Math.max(first.getX(), second.getX()) + 1,
                Math.max(first.getY(), second.getY()) + 1, Math.max(first.getZ(), second.getZ()) + 1);
        return new RitualArea(min, max);
    }

    public RitualArea copy() {
        return new RitualArea(minimum, maximum);
    }

    public BlockPos minimum() {
        return minimum;
    }

    public BlockPos maximum() {
        return maximum;
    }

    public int volume() {
        long volume = (long) (maximum.getX() - minimum.getX()) * (maximum.getY() - minimum.getY())
                * (maximum.getZ() - minimum.getZ());
        return (int) Math.clamp(volume, 0, Integer.MAX_VALUE);
    }

    public List<BlockPos> positions(BlockPos center) {
        List<BlockPos> result = new ArrayList<>(volume());
        for (int y = minimum.getY(); y < maximum.getY(); y++)
            for (int x = minimum.getX(); x < maximum.getX(); x++)
                for (int z = minimum.getZ(); z < maximum.getZ(); z++)
                    result.add(center.offset(x, y, z));
        return result;
    }

    public AABB aabb(BlockPos center) {
        return new AABB(center.getX() + minimum.getX(), center.getY() + minimum.getY(), center.getZ() + minimum.getZ(),
                center.getX() + maximum.getX(), center.getY() + maximum.getY(), center.getZ() + maximum.getZ());
    }

    public boolean isWithinLimits(int verticalLimit, int horizontalLimit) {
        return minimum.getY() >= -verticalLimit && maximum.getY() <= verticalLimit + 1
                && minimum.getX() >= -horizontalLimit && maximum.getX() <= horizontalLimit + 1
                && minimum.getZ() >= -horizontalLimit && maximum.getZ() <= horizontalLimit + 1;
    }

    public boolean resizeByBlockPositions(BlockPos first, BlockPos second) {
        BlockPos min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()));
        BlockPos max = new BlockPos(Math.max(first.getX(), second.getX()) + 1,
                Math.max(first.getY(), second.getY()) + 1, Math.max(first.getZ(), second.getZ()) + 1);
        long volume = (long) (max.getX() - min.getX()) * (max.getY() - min.getY()) * (max.getZ() - min.getZ());
        if (volume > Integer.MAX_VALUE) return false;
        minimum = min;
        maximum = max;
        return true;
    }

    private void normalize() {
        BlockPos min = new BlockPos(Math.min(minimum.getX(), maximum.getX()), Math.min(minimum.getY(), maximum.getY()),
                Math.min(minimum.getZ(), maximum.getZ()));
        BlockPos max = new BlockPos(Math.max(minimum.getX(), maximum.getX()), Math.max(minimum.getY(), maximum.getY()),
                Math.max(minimum.getZ(), maximum.getZ()));
        if (max.getX() <= min.getX() || max.getY() <= min.getY() || max.getZ() <= min.getZ()) {
            max = min.offset(1, 1, 1);
        }
        minimum = min;
        maximum = max;
    }
}

