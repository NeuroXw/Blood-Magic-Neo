package wayoftime.bloodmagic.test;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.block.*;
import wayoftime.bloodmagic.common.block.entity.*;
import wayoftime.bloodmagic.common.item.*;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.core.data.*;
import wayoftime.bloodmagic.ritual.*;

@EventBusSubscriber(modid=BloodMagic.MODID)
public final class RitualChecks {
    private static final BlockPos POS=new BlockPos(536,180,520);
    private static final BlockPos SAVE=new BlockPos(620,180,600);
    private static final BlockPos GROW=POS.offset(16,0,0);
    private static java.util.concurrent.CompletableFuture<?> lighting;
    private static long lightDeadline;
    private static final UUID OWNER=UUID.fromString("e98430df-96db-4a9f-85e1-883eeea00061");
    private static final Binding BINDING=new Binding(OWNER,"Ritual fixture");
    private static boolean denyPlace,denyBreak;
    static boolean denyArrayDrop;
    private static int assertions;
    private static ServerPlayer player;
    private RitualChecks(){}
    @SubscribeEvent public static void placeDenied(BlockEvent.EntityPlaceEvent event){if(denyPlace)event.setCanceled(true);}
    @SubscribeEvent public static void breakDenied(BreakBlockEvent event){if(denyBreak)event.setCanceled(true);}
    @SubscribeEvent public static void arrayDropDenied(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(denyArrayDrop&&event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().is(Items.DIAMOND))event.setCanceled(true);
    }
    static int begin(ServerLevel level){
        int start=assertions;level.getChunk(SAVE);
        if(Stage6SmokeChecks.restartExpected){
            var saved=(MasterRitualBlockEntity)level.getBlockEntity(SAVE);
            check(saved!=null&&saved.active()&&saved.ritual()==BasicRitual.WATER&&BINDING.equals(saved.owner()),"world restart retains active ritual and bound owner");
        }
        clearArea(level,SAVE);clearArea(level,POS);
        player=MenuTestPlayer.create(level,"S6Ritual");player.setGameMode(GameType.SURVIVAL);move(POS);
        place(level,GROW,BasicRitual.GREEN_GROVE,true);
        level.setBlockAndUpdate(GROW.above(),Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE,7));
        level.setBlockAndUpdate(GROW.above(2),Blocks.WHEAT.defaultBlockState());
        level.setBlockAndUpdate(GROW.above(3),Blocks.GLOWSTONE.defaultBlockState());
        var engine=level.getChunkSource().getLightEngine();
        lighting=engine.waitForPendingTasks(GROW.getX()>>4,GROW.getZ()>>4);
        engine.tryScheduleUpdate();lightDeadline=System.nanoTime()+120_000_000_000L;
        return assertions-start;
    }
    static boolean ready(){
        if(System.nanoTime()>lightDeadline&&!lighting.isDone())throw new IllegalStateException("Ritual fixture light update timeout");
        if(!lighting.isDone())return false;
        lighting.join();return true;
    }
    static int run(ServerLevel level){
        int start=assertions;
        check(BasicRitual.WATER.components().size()==4&&BasicRitual.GREEN_GROVE.components().size()==8&&BasicRitual.CRUSHING.components().size()==16,"upstream ritual rune counts 4/8/16");
        int chunks=level.getChunkSource().getLoadedChunksCount();
        check(BasicRitual.CRUSHING.scan(level,new BlockPos(400000,180,400000))==BasicRitual.Scan.UNLOADED&&chunks==level.getChunkSource().getLoadedChunksCount(),"ritual scan never loads absent chunks");
        var master=place(level,POS,BasicRitual.WATER,false);balance(level,1000);
        check(!master.activate(player,BINDING)&&balance(level)==1000,"incomplete ritual cannot activate or spend LP");
        var diviner=new ItemStack(BloodMagicItems.RITUAL_DIVINER.get());player.setItemInHand(InteractionHand.MAIN_HAND,diviner);
        player.getInventory().setItem(1,new ItemStack(BloodMagicItems.RITUAL_STONE.get(),8));
        denyPlace=true;
        try{check(!diviner.getItem().useOn(context(POS)).consumesAction()&&player.getInventory().getItem(1).getCount()==8,"denied diviner placement preserves stone");}finally{denyPlace=false;}
        for(int i=0;i<4;i++)check(diviner.getItem().useOn(context(POS)).consumesAction(),"diviner places component "+i);
        check(BasicRitual.WATER.scan(level,POS)==BasicRitual.Scan.COMPLETE&&player.getInventory().getItem(1).getCount()==4,"diviner consumes exactly four blank stones");
        diviner.getItem().useOn(context(POS));check(player.getInventory().getItem(1).getCount()==4,"completed diviner does not consume extra stones");
        balance(level,499);check(!master.activate(player,BINDING)&&balance(level)==499,"ritual activation fee all or nothing");
        balance(level,1000);check(master.activate(player,BINDING)&&balance(level)==500&&master.active(),"water ritual activates for 500 LP");
        check(master.activate(player,BINDING)&&balance(level)==500,"repeated activation cannot double debit");
        denyPlace=true;try{tick(master,3);check(level.isEmptyBlock(POS.above())&&balance(level)==500,"protected water target produces no effect or debit");}finally{denyPlace=false;}
        tick(master,1);check(level.getBlockState(POS.above()).is(Blocks.WATER)&&balance(level)==475,"water ritual places source for 25 LP");
        tick(master,3);check(balance(level)==475,"already filled target has no ongoing fee");
        level.setBlockAndUpdate(POS.above(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(POS.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        tick(master,3);check(level.isEmptyBlock(POS.above())&&balance(level)==475&&master.active(),"redstone pauses active ritual without charge");
        level.setBlockAndUpdate(POS.east(),Blocks.AIR.defaultBlockState());tick(master,1);check(balance(level)==450,"removing redstone resumes without activation fee");
        level.setBlockAndUpdate(POS.above(),Blocks.AIR.defaultBlockState());balance(level,24);tick(master,3);
        check(balance(level)==24&&level.isEmptyBlock(POS.above())&&master.active(),"empty network pauses effect without health fallback");
        balance(level,25);tick(master,1);check(balance(level)==0&&level.getBlockState(POS.above()).is(Blocks.WATER),"refill resumes using final 25 LP");
        var roundTrip=ArrayChecks.roundTrip(level,master,MasterRitualBlockEntity.class);
        check(roundTrip.active()&&BINDING.equals(roundTrip.owner())&&roundTrip.ritual()==BasicRitual.WATER,"ritual NBT retains activation and identity");
        level.setBlockAndUpdate(POS.offset(-1,0,-1),Blocks.AIR.defaultBlockState());balance(level,1000);tick(master,1);
        check(!master.active()&&balance(level)==1000,"missing rune deactivates before effect");
        master=place(level,POS,BasicRitual.WATER,true);
        var crystal=new ItemStack(BloodMagicItems.ACTIVATION_CRYSTAL.get());player.setItemInHand(InteractionHand.OFF_HAND,crystal);
        check(crystal.getItem().use(level,player,InteractionHand.OFF_HAND).consumesAction()&&ItemBindable.binding(crystal).ownerId().equals(player.getUUID()),"activation crystal binds through real item use");
        crystal.set(BloodMagicDataComponents.BINDING.get(),BINDING);
        var offContext=new UseOnContext(player,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(POS),Direction.UP,POS,false));
        check(crystal.getItem().useOn(offContext).consumesAction()&&master.active()&&balance(level)==500,"offhand bound activation crystal reaches owner network");
        player.setShiftKeyDown(true);check(crystal.getItem().useOn(offContext).consumesAction()&&!master.active()&&balance(level)==500,"sneak activation crystal stops without extra debit");player.setShiftKeyDown(false);
        var scribe=new ItemStack(BloodMagicItems.FIRE_SCRIBE.get());player.setItemInHand(InteractionHand.MAIN_HAND,scribe);
        check(scribe.getItem().useOn(context(POS.offset(-1,0,-1))).consumesAction()&&scribe.getDamageValue()==1,"inscription tool retunes rune with one durability");
        check(!scribe.getItem().useOn(context(POS.offset(-1,0,-1))).consumesAction()&&scribe.getDamageValue()==1,"identical inscription costs no durability");
        testCrushing(level);testGrowth(level);
        // Base diviner cannot select a ritual requiring dusk runes; upgraded diviner can.
        player.setItemInHand(InteractionHand.MAIN_HAND,diviner);
        for(int i=0;i<5;i++){diviner.getItem().use(level,player,InteractionHand.MAIN_HAND);check(((ItemRitualDiviner)diviner.getItem()).selected(diviner)!=BasicRitual.CRUSHING,"base diviner excludes dusk "+i);}
        var dusk=new ItemStack(BloodMagicItems.DUSK_RITUAL_DIVINER.get());player.setItemInHand(InteractionHand.MAIN_HAND,dusk);
        dusk.getItem().use(level,player,InteractionHand.MAIN_HAND);dusk.getItem().use(level,player,InteractionHand.MAIN_HAND);
        check(((ItemRitualDiviner)dusk.getItem()).selected(dusk)==BasicRitual.CRUSHING,"dusk diviner selects mining pattern");
        clearArea(level,POS);return assertions-start;
    }
    private static void testCrushing(ServerLevel level){
        var master=place(level,POS,BasicRitual.CRUSHING,true);balance(level,6000);
        check(master.activate(player,BINDING)&&balance(level)==1000,"mining activation costs 5000 LP");
        BlockPos target=POS.offset(-1,-3,-1);level.setBlockAndUpdate(target,Blocks.DIAMOND_ORE.defaultBlockState());
        level.setBlockAndUpdate(POS.above(),Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)level.getBlockEntity(POS.above());
        denyBreak=true;try{tick(master,40);check(level.getBlockState(target).is(Blocks.DIAMOND_ORE)&&balance(level)==1000&&chest.isEmpty(),"new BreakBlockEvent cancellation prevents mining and payment");}finally{denyBreak=false;}
        tick(master,40);check(level.isEmptyBlock(target)&&balance(level)==993,"mining one ore costs 7 LP");
        check(chest.countItem(Items.DIAMOND)==1,"mining applies diamond tool loot and routes to chest");
        level.setBlockAndUpdate(target,Blocks.BEDROCK.defaultBlockState());tick(master,40);check(balance(level)==993&&level.getBlockState(target).is(Blocks.BEDROCK),"unbreakable blocks are skipped free");
        level.setBlockAndUpdate(target,Blocks.CHEST.defaultBlockState());tick(master,40);check(balance(level)==993&&level.getBlockEntity(target) instanceof ChestBlockEntity,"mining preserves block entity inventories");
        var rune=BasicRitual.CRUSHING.components().getFirst();level.setBlockAndUpdate(POS.offset(rune.offset()),Blocks.STONE.defaultBlockState());tick(master,1);
        check(!master.active()&&balance(level)==993,"wrong rune stops mining immediately");
    }
    private static void testGrowth(ServerLevel level){
        var master=(MasterRitualBlockEntity)level.getBlockEntity(GROW);move(GROW);balance(level,6000);
        check(master.activate(player,BINDING)&&balance(level)==1000,"green grove activation costs 5000 LP");
        BlockPos crop=GROW.above(2);level.setBlockAndUpdate(crop,Blocks.WHEAT.defaultBlockState());
        check(level.getRawBrightness(crop,0)>=9,"threaded light engine completed the real crop fixture");
        for(int i=0;i<8000&&level.getBlockState(crop).getValue(CropBlock.AGE)==0;i++)tick(master,1);
        check(level.getBlockState(crop).getValue(CropBlock.AGE)>0&&balance(level)==980,"successful native crop growth costs exactly 20 LP");
        level.setBlockAndUpdate(crop,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));tick(master,100);
        check(balance(level)==980,"mature crop does not drain LP");
        clearArea(level,GROW);move(POS);
    }
    static int finish(ServerLevel level) throws java.io.IOException{
        var saved=place(level,SAVE,BasicRitual.WATER,true);move(SAVE);balance(level,1000);
        check(saved.activate(player,BINDING),"prepare active ritual restart fixture");
        // Block its target, so a later natural tick cannot spend LP before saving.
        level.setBlockAndUpdate(SAVE.above(),Blocks.STONE.defaultBlockState());Files.writeString(Path.of("stage6-ritual-expected.txt"),"water / active\n");return 1;
    }
    static MasterRitualBlockEntity place(ServerLevel level,BlockPos pos,BasicRitual ritual,boolean runes){
        clearArea(level,pos);level.setBlockAndUpdate(pos,BloodMagicBlocks.MASTER_RITUAL_STONE.get().defaultBlockState());
        var master=(MasterRitualBlockEntity)level.getBlockEntity(pos);master.select(ritual);
        if(runes)for(var part:ritual.components())level.setBlockAndUpdate(pos.offset(part.offset()),part.rune().block().defaultBlockState());
        return master;
    }
    static void clearArea(ServerLevel level,BlockPos pos){
        for(BlockPos p:BlockPos.betweenClosed(pos.offset(-3,-3,-3),pos.offset(3,4,3))){level.getChunk(p);if(level.getBlockEntity(p) instanceof Container c)c.clearContent();level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());}
    }
    static void move(BlockPos pos){player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+2);}
    static UseOnContext context(BlockPos pos){return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    static void balance(ServerLevel level,int amount){var n=SoulNetworks.get(level);n.consume(OWNER,n.get(OWNER).currentEssence());n.fill(OWNER,amount,20000);}
    static int balance(ServerLevel level){return SoulNetworks.get(level).get(OWNER).currentEssence();}
    static void tick(MasterRitualBlockEntity master,int n){for(int i=0;i<n;i++)MasterRitualBlockEntity.serverTick(master.getLevel(),master.getBlockPos(),master.getBlockState(),master);}
    static void check(boolean condition,String message){if(!condition)throw new IllegalStateException("STAGE6_SMOKE FAIL: "+message);assertions++;}
}
