package wayoftime.bloodmagic.common.item;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import wayoftime.bloodmagic.common.block.entity.MasterRitualBlockEntity;
public final class ItemActivationCrystal extends ItemBindable {
    public ItemActivationCrystal(Properties p){super(p,"activationcrystal");}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(!canUse(player))return InteractionResult.FAIL;if(level.isClientSide())return InteractionResult.SUCCESS;bindIfAbsent(player,player.getItemInHand(hand));return InteractionResult.SUCCESS_SERVER;}
    @Override public InteractionResult useOn(UseOnContext c){
        var player=c.getPlayer();if(!canUse(player))return InteractionResult.FAIL;
        if(!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof MasterRitualBlockEntity master))return InteractionResult.PASS;
        if(c.getLevel().isClientSide())return InteractionResult.SUCCESS;
        var binding=bindIfAbsent(player,c.getItemInHand());
        boolean success=player.isShiftKeyDown()?master.stop(player,binding):master.activate(player,binding);
        player.sendOverlayMessage(Component.translatable(success?"message.bloodmagic.ritual.changed":"message.bloodmagic.ritual.failed"));
        return success?InteractionResult.SUCCESS_SERVER:InteractionResult.FAIL;
    }
}
