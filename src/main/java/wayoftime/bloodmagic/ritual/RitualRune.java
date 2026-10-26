package wayoftime.bloodmagic.ritual;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
public enum RitualRune {
    BLANK, WATER, EARTH, FIRE, AIR, DUSK;
    public Block block() { return switch (this) {
        case BLANK -> BloodMagicBlocks.RITUAL_STONE.get(); case WATER -> BloodMagicBlocks.WATER_RITUAL_STONE.get();
        case EARTH -> BloodMagicBlocks.EARTH_RITUAL_STONE.get(); case FIRE -> BloodMagicBlocks.FIRE_RITUAL_STONE.get();
        case AIR -> BloodMagicBlocks.AIR_RITUAL_STONE.get(); case DUSK -> BloodMagicBlocks.DUSK_RITUAL_STONE.get();
    }; }
    public static boolean isRune(BlockState state) { for (var rune : values()) if (state.is(rune.block())) return true; return false; }
}
