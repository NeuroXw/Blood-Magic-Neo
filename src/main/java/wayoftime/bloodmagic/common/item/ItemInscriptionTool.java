package wayoftime.bloodmagic.common.item;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import wayoftime.bloodmagic.ritual.*;
public final class ItemInscriptionTool extends Item {
    private final RitualRune rune;
    public ItemInscriptionTool(Properties p,RitualRune rune){super(p.durability(40));this.rune=rune;}
    @Override public InteractionResult useOn(UseOnContext c){
        var player=c.getPlayer();var level=c.getLevel();var pos=c.getClickedPos();
        if(player==null||!player.isAlive()||player.isSpectator()||!player.mayBuild()||!player.isWithinBlockInteractionRange(pos,0)||!WorldAccess.loaded(level,pos))return InteractionResult.FAIL;
        var state=level.getBlockState(pos);if(!RitualRune.isRune(state)||state.is(rune.block()))return InteractionResult.FAIL;
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        if(!WorldAccess.mayPlace(server,pos,rune.block().defaultBlockState(),player)||!level.setBlockAndUpdate(pos,rune.block().defaultBlockState()))return InteractionResult.FAIL;
        if(!player.hasInfiniteMaterials())c.getItemInHand().hurtAndBreak(1,player,c.getHand());return InteractionResult.SUCCESS_SERVER;
    }
}
