package wayoftime.bloodmagic.common.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import wayoftime.bloodmagic.common.block.entity.*;
public final class BlockMasterRitual extends BaseEntityBlock {
    public static final MapCodec<BlockMasterRitual> CODEC=simpleCodec(BlockMasterRitual::new);
    public BlockMasterRitual(Properties p){super(p);}
    @Override protected MapCodec<? extends BlockMasterRitual> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MasterRitualBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,BloodMagicBlockEntities.MASTER_RITUAL.get(),MasterRitualBlockEntity::serverTick);}
}
