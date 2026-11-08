package wayoftime.bloodmagic.common.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import wayoftime.bloodmagic.common.block.entity.*;
import wayoftime.bloodmagic.common.item.ItemRitualDiviner;

public final class BlockMasterRitual extends BaseEntityBlock {
    public static final MapCodec<BlockMasterRitual> CODEC=simpleCodec(BlockMasterRitual::new);
    public BlockMasterRitual(Properties p){super(p);}
    @Override protected MapCodec<? extends BlockMasterRitual> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MasterRitualBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,BloodMagicBlockEntities.MASTER_RITUAL.get(),MasterRitualBlockEntity::serverTick);}
    /** 26.1 keeps held-item use explicit; never let special master-stone menu dispatch shadow the diviner. */
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (stack.getItem() instanceof ItemRitualDiviner diviner) {
            return diviner.useOn(new UseOnContext(player,hand,hit));
        }
        return super.useItemOn(stack,state,level,pos,player,hand,hit);
    }
}
