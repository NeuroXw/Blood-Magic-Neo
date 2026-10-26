package wayoftime.bloodmagic.common.block.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.core.data.*;
import wayoftime.bloodmagic.ritual.*;

public final class MasterRitualBlockEntity extends BlockEntity {
    private Binding owner;
    private BasicRitual ritual = BasicRitual.WATER;
    private boolean active;
    private int cooldown;
    public MasterRitualBlockEntity(BlockPos pos,BlockState state) { super(BloodMagicBlockEntities.MASTER_RITUAL.get(),pos,state); }
    public Binding owner() { return owner; }
    public BasicRitual ritual() { return ritual; }
    public boolean active() { return active; }
    public void select(BasicRitual ritual) { if(level instanceof ServerLevel && !active) {this.ritual=ritual;changed();} }
    public boolean activate(Player player,Binding binding) {
        if(!(level instanceof ServerLevel server) || binding==null || !validUser(player))return false;
        if(active)return owner!=null && owner.ownerId().equals(binding.ownerId());
        BasicRitual chosen=ritual;
        if(chosen.scan(level,worldPosition)!=BasicRitual.Scan.COMPLETE) {
            chosen=null;
            for(var candidate:BasicRitual.values())if(candidate.scan(level,worldPosition)==BasicRitual.Scan.COMPLETE){chosen=candidate;break;}
        }
        if(chosen==null || level.hasNeighborSignal(worldPosition))return false;
        if(!player.hasInfiniteMaterials() && !SoulNetworks.get(server).consume(binding.ownerId(),chosen.activationCost))return false;
        owner=binding; ritual=chosen; active=true; cooldown=0;changed();return true;
    }
    public boolean stop(Player player,Binding binding) {
        if(!validUser(player) || owner==null || binding==null || !owner.ownerId().equals(binding.ownerId()))return false;
        active=false;cooldown=0;changed();return true;
    }
    private boolean validUser(Player player) { return player!=null && player.isAlive() && !player.isSpectator() && player.level()==level && player.mayBuild()
            && player.isWithinBlockInteractionRange(worldPosition,0) && level.mayInteract(player,worldPosition); }
    public static void serverTick(Level level,BlockPos pos,BlockState state,MasterRitualBlockEntity master) {
        if(!(level instanceof ServerLevel server) || master.isRemoved() || !master.active || master.owner==null)return;
        var scan=master.ritual.scan(level,pos);
        if(scan==BasicRitual.Scan.UNLOADED)return;
        if(scan==BasicRitual.Scan.INVALID){master.active=false;master.changed();return;}
        if(level.hasNeighborSignal(pos))return;
        if(master.cooldown>0){master.cooldown--;master.setChanged();return;}
        master.cooldown=master.ritual.interval-1;master.setChanged();
        master.perform(server);
    }
    private void perform(ServerLevel server) {
        if(SoulNetworks.get(server).get(owner.ownerId()).currentEssence()<ritual.cost)return;
        ServerPlayer actor=FakePlayerFactory.get(server,new GameProfile(owner.ownerId(),owner.ownerName()));
        // No cached Level/Player references; each invocation uses the current dimension's fake player.
        switch(ritual) {
            case WATER -> {
                BlockPos target=worldPosition.above();
                if(WorldAccess.loaded(server,target) && server.isEmptyBlock(target)
                        && !server.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,target)
                        && WorldAccess.mayPlace(server,target,Blocks.WATER.defaultBlockState(),actor))
                    paid(server,()->server.setBlockAndUpdate(target,Blocks.WATER.defaultBlockState()));
            }
            case GREEN_GROVE -> {
                for(BlockPos p:BlockPos.betweenClosed(worldPosition.offset(-1,2,-1),worldPosition.offset(1,2,1))) {
                    if(!WorldAccess.loaded(server,p))return;
                    BlockState before=server.getBlockState(p);
                    if(!(before.getBlock() instanceof CropBlock crop) || crop.isMaxAge(before) || server.getRandom().nextDouble()>=.3)continue;
                    if(!WorldAccess.mayPlace(server,p,before,actor))continue;
                    paid(server,()->{before.randomTick(server,p,server.getRandom());return !server.getBlockState(p).equals(before);});
                }
            }
            case CRUSHING -> {
                // Upstream default range: 3x3x3 directly underneath the master; at most one break per refresh.
                for(BlockPos p:BlockPos.betweenClosed(worldPosition.offset(-1,-3,-1),worldPosition.offset(1,-1,1))) {
                    if(!WorldAccess.loaded(server,p))return;
                    BlockState before=server.getBlockState(p);
                    if(before.isAir() || !before.getFluidState().isEmpty() || before.getDestroySpeed(server,p)<0 || before.hasBlockEntity()
                            || RitualRune.isRune(before) || before.is(BloodMagicBlocks.MASTER_RITUAL_STONE.get()))continue;
                    if(!WorldAccess.mayBreak(server,p,actor))continue;
                    var drops=Block.getDrops(before,server,p,null,actor,new ItemStack(Items.DIAMOND_PICKAXE));
                    if(paid(server,()->server.destroyBlock(p,false,actor))) {for(var stack:drops)deliver(server,stack);break;}
                }
            }
        }
    }
    private boolean paid(ServerLevel server,java.util.function.BooleanSupplier action) {
        var networks=SoulNetworks.get(server);
        if(!networks.consume(owner.ownerId(),ritual.cost))return false;
        boolean success=false;
        try {success=action.getAsBoolean();return success;}
        finally {if(!success)networks.fill(owner.ownerId(),ritual.cost,Integer.MAX_VALUE);}
    }
    private void deliver(ServerLevel server,ItemStack stack) {
        BlockPos target=worldPosition.above();
        if(WorldAccess.loaded(server,target) && server.getBlockEntity(target) instanceof Container container) {
            int[] slots=container instanceof WorldlyContainer sided?sided.getSlotsForFace(Direction.DOWN):java.util.stream.IntStream.range(0,container.getContainerSize()).toArray();
            for(int slot:slots) {
                if(!container.canPlaceItem(slot,stack) || container instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot,stack,Direction.DOWN))continue;
                ItemStack existing=container.getItem(slot);
                if(!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,stack))continue;
                int count=Math.min(stack.getCount(),Math.min(container.getMaxStackSize(stack),stack.getMaxStackSize())-existing.getCount());
                if(count<=0)continue;
                if(existing.isEmpty())container.setItem(slot,stack.copyWithCount(count));else existing.grow(count);
                stack.shrink(count);container.setChanged();if(stack.isEmpty())return;
            }
        }
        Containers.dropItemStack(server,target.getX()+.5,target.getY()+.5,target.getZ()+.5,stack);
    }
    private void changed(){setChanged();if(level instanceof ServerLevel)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",Binding.CODEC,owner);out.store("ritual",BasicRitual.CODEC,ritual);out.putBoolean("active",active);out.putInt("cooldown",cooldown);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",Binding.CODEC).orElse(null);var saved=in.read("ritual",BasicRitual.CODEC);ritual=saved.orElse(BasicRitual.WATER);active=in.getBooleanOr("active",false)&&owner!=null&&saved.isPresent();cooldown=Math.clamp(in.getIntOr("cooldown",0),0,ritual.interval);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
