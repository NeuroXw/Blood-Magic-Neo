package wayoftime.bloodmagic.common.item;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.MasterRitualBlockEntity;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.ritual.*;
public final class ItemRitualDiviner extends Item {
    private final boolean dusk;
    public ItemRitualDiviner(Properties p,boolean dusk){super(p.stacksTo(1));this.dusk=dusk;}
    public BasicRitual selected(ItemStack stack){int i=stack.getOrDefault(BloodMagicDataComponents.RITUAL_SELECTION.get(),0);return BasicRitual.values()[Math.floorMod(i,dusk?3:2)];}
    private InteractionResult cycle(Player player,ItemStack stack){
        if(player.level().isClientSide())return InteractionResult.SUCCESS;
        stack.set(BloodMagicDataComponents.RITUAL_SELECTION.get(),(selected(stack).ordinal()+1)%(dusk?3:2));
        player.sendOverlayMessage(Component.translatable("message.bloodmagic.ritual.selected",Component.translatable("ritual.bloodmagic."+selected(stack).getSerializedName())));return InteractionResult.SUCCESS_SERVER;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){return player.isAlive()&&!player.isSpectator()?cycle(player,player.getItemInHand(hand)):InteractionResult.FAIL;}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
        if(player==null||!player.isAlive()||player.isSpectator()||!player.mayBuild()||!player.isWithinBlockInteractionRange(pos,0)||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos) instanceof MasterRitualBlockEntity master))return InteractionResult.PASS;
        if(player.isShiftKeyDown())return cycle(player,context.getItemInHand());
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        if(master.active())return InteractionResult.FAIL;
        var ritual=selected(context.getItemInHand());master.select(ritual);
        for(var part:ritual.components()) {
            BlockPos target=pos.offset(part.offset());if(!WorldAccess.loaded(server,target))return InteractionResult.FAIL;
            var before=level.getBlockState(target);if(before.is(part.rune().block()))continue;
            if(!before.isAir()&&!RitualRune.isRune(before))return InteractionResult.FAIL;
            if(!player.mayUseItemAt(target,context.getClickedFace(),context.getItemInHand())||!WorldAccess.mayPlace(server,target,part.rune().block().defaultBlockState(),player))return InteractionResult.FAIL;
            ItemStack stone=ItemStack.EMPTY;
            if(before.isAir()&&!player.hasInfiniteMaterials()) {
                for(int i=0;i<player.getInventory().getContainerSize();i++){var candidate=player.getInventory().getItem(i);if(candidate.is(BloodMagicBlocks.RITUAL_STONE.get().asItem())){stone=candidate;break;}}
                if(stone.isEmpty())return InteractionResult.FAIL;
            }
            if(!level.setBlockAndUpdate(target,part.rune().block().defaultBlockState()))return InteractionResult.FAIL;
            if(!stone.isEmpty())stone.shrink(1);return InteractionResult.SUCCESS_SERVER;
        }
        player.sendOverlayMessage(Component.translatable("message.bloodmagic.ritual.ready"));return InteractionResult.SUCCESS_SERVER;
    }
}
