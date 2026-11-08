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
    public BasicRitual selected(ItemStack stack){int i=stack.getOrDefault(BloodMagicDataComponents.RITUAL_SELECTION.get(),0);return BasicRitual.values()[Math.floorMod(i,BasicRitual.values().length)];}
    public boolean canPerform(BasicRitual ritual) {
        if (ritual==null) return false;
        if (dusk) return true;
        for (var part:ritual.components()) if (part.rune()==RitualRune.DUSK) return false;
        return true;
    }
    private InteractionResult cycle(Player player,ItemStack stack){
        if(player.level().isClientSide())return InteractionResult.SUCCESS;
        int current=selected(stack).ordinal();
        for(int step=1;step<=BasicRitual.values().length;step++) {
            BasicRitual candidate=BasicRitual.values()[(current+step)%BasicRitual.values().length];
            if(!canPerform(candidate)) continue;
            stack.set(BloodMagicDataComponents.RITUAL_SELECTION.get(),candidate.ordinal());
            player.sendOverlayMessage(Component.translatable("message.bloodmagic.ritual.selected",Component.translatable("ritual.bloodmagic."+candidate.getSerializedName())));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.FAIL;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){return player.isAlive()&&!player.isSpectator()?cycle(player,player.getItemInHand(hand)):InteractionResult.FAIL;}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
        if(player==null||!player.isAlive()||player.isSpectator()||!player.mayBuild()||!player.isWithinBlockInteractionRange(pos,0)||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos) instanceof MasterRitualBlockEntity master))return InteractionResult.PASS;
        if(player.isShiftKeyDown())return cycle(player,context.getItemInHand());
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        if(master.active())return InteractionResult.FAIL;
        var ritual=selected(context.getItemInHand());
        if(!canPerform(ritual)) {
            player.sendOverlayMessage(Component.translatable("message.bloodmagic.ritual.diviner.requiresDusk"));
            return InteractionResult.FAIL;
        }
        master.select(ritual);
        for(var part:ritual.components()) {
            BlockPos target=pos.offset(part.offset());if(!WorldAccess.loaded(server,target))return InteractionResult.FAIL;
            var before=level.getBlockState(target);
            if(before.is(part.rune().block()))continue;
            if(!before.isAir()&&!RitualRune.isRune(before))return InteractionResult.FAIL;
            if(!player.mayUseItemAt(target,context.getClickedFace(),context.getItemInHand())||!WorldAccess.mayPlace(server,target,part.rune().block().defaultBlockState(),player))return InteractionResult.FAIL;
            ItemStack stone=ItemStack.EMPTY;
            if(before.isAir()&&!player.hasInfiniteMaterials()) {
                for(int i=0;i<player.getInventory().getContainerSize();i++){var candidate=player.getInventory().getItem(i);if(candidate.is(BloodMagicBlocks.RITUAL_STONE.get().asItem())){stone=candidate;break;}}
                if(stone.isEmpty()) {
                    player.sendOverlayMessage(Component.translatable("message.bloodmagic.ritual.diviner.missingStone"));
                    return InteractionResult.FAIL;
                }
            }
            if(!level.setBlockAndUpdate(target,part.rune().block().defaultBlockState()))return InteractionResult.FAIL;
            if(!stone.isEmpty())stone.shrink(1);
            player.getInventory().setChanged();
            return InteractionResult.SUCCESS_SERVER;
        }
        player.sendOverlayMessage(Component.translatable("message.bloodmagic.ritual.ready"));return InteractionResult.SUCCESS_SERVER;
    }
}
