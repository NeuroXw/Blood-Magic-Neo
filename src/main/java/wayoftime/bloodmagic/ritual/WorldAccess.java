package wayoftime.bloodmagic.ritual;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
public final class WorldAccess {
    private WorldAccess() {}
    public static boolean loaded(LevelReader reader, BlockPos pos) {
        return reader instanceof Level level && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }
    public static boolean allowed(ServerLevel level, BlockPos pos, Player player) {
        return loaded(level,pos) && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player,pos) && player.mayBuild();
    }
    public static boolean mayPlace(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        if (!allowed(level,pos,player)) return false;
        var event = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(),level,pos),level.getBlockState(pos),player) {
            @Override public BlockState getPlacedBlock() { return state; }
        };
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }
    public static boolean mayBreak(ServerLevel level, BlockPos pos, Player player) {
        return allowed(level,pos,player) && !NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(level,pos,level.getBlockState(pos),player)).isCanceled();
    }
}
