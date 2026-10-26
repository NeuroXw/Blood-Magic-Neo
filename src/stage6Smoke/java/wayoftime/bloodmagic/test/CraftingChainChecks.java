package wayoftime.bloodmagic.test;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import wayoftime.bloodmagic.altar.AltarStructure;
import wayoftime.bloodmagic.common.block.*;
import wayoftime.bloodmagic.common.block.entity.*;
import wayoftime.bloodmagic.common.item.*;
import wayoftime.bloodmagic.core.data.SoulNetworks;

/** Real crafting recipes and machine results connect the previously separate survival segments. */
final class CraftingChainChecks {
    private static int assertions;
    static int run(ServerLevel level) {
        var stone=new ItemStack(Items.STONE);
        ItemStack altarItem=craft(level,List.of(stone,ItemStack.EMPTY,stone,stone,new ItemStack(Items.FURNACE),stone,
                new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.GOLD_INGOT)),BloodMagicBlocks.BLOOD_ALTAR.get().asItem());
        BlockPos pos=ArrayChecks.POS.offset(-6,0,0);
        level.getChunk(pos);ArrayChecks.clear(level,pos);
        level.setBlockAndUpdate(pos,((BlockItem)altarItem.getItem()).getBlock().defaultBlockState());
        var altar=(BloodAltarBlockEntity)level.getBlockEntity(pos);
        ItemStack slate=altarCraft(altar,Items.STONE,1000,BloodMagicItems.SLATE.get());
        ItemStack orb=altarCraft(altar,Items.DIAMOND,2000,BloodMagicItems.WEAK_BLOOD_ORB.get());
        BloodOrbData.bind(orb,ArrayChecks.OWNER,"Survival chain");
        var runeInput=CraftingInput.of(3,3,List.of(stone,slate,stone,stone,orb,stone,stone,stone,stone));
        var runeRecipe=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,runeInput,level).orElseThrow().value();
        check(runeRecipe.assemble(runeInput).is(BloodMagicBlocks.BLANK_RUNE.get().asItem()),"blank rune recipe unlocks tier two");
        check(runeRecipe.getRemainingItems(runeInput).stream().anyMatch(s->ItemStack.matches(s,orb)),"crafting blank rune returns the bound orb");
        for(var component:AltarStructure.components(2)){
            BlockPos runePos=pos.offset(component.offset());level.getChunk(runePos);
            level.setBlockAndUpdate(runePos,BloodMagicBlocks.BLANK_RUNE.get().defaultBlockState());
        }
        check(altar.refreshStructure()&&altar.getAltarState().tier()==2,"craftable blank runes form a real tier two altar");
        ItemStack apprentice=altarCraft(altar,Items.REDSTONE_BLOCK,5000,BloodMagicItems.APPRENTICE_BLOOD_ORB.get());
        BloodOrbData.bind(apprentice,ArrayChecks.OWNER,"Survival chain");
        ItemStack tableItem=craft(level,List.of(stone,stone,stone,new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.GOLD_INGOT),slate,new ItemStack(Items.GOLD_INGOT)),BloodMagicItems.ALCHEMY_TABLE.get());
        var player=ArrayChecks.player;BlockPos tablePos=ArrayChecks.TABLE;
        ArrayChecks.clear(level,tablePos);ArrayChecks.clear(level,tablePos.north());ArrayChecks.clear(level,tablePos.south());
        level.setBlockAndUpdate(tablePos.below(),Blocks.STONE.defaultBlockState());
        player.setPos(tablePos.getX()+.5,tablePos.getY(),tablePos.getZ()+2);player.setYRot(180);
        player.setItemInHand(InteractionHand.MAIN_HAND,tableItem);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(tablePos.below()),Direction.UP,tablePos.below(),false));
        check(tableItem.useOn(context).consumesAction()&&player.getMainHandItem().isEmpty(),"crafted table places through survival BlockItem interaction");
        var table=(AlchemyTableBlockEntity)level.getBlockEntity(tablePos);
        BlockPos other=BlockAlchemyTable.other(tablePos,table.getBlockState());
        check(BlockAlchemyTable.complete(level,tablePos,table.getBlockState())&&level.getBlockEntity(other)==null,"two table halves have one inventory");
        var ingredients=List.of(List.of(Items.REDSTONE,Items.WHITE_DYE,Items.GUNPOWDER,Items.COAL),
                List.of(Items.SUGAR,Items.WATER_BUCKET,Items.WATER_BUCKET),List.of(Items.LAVA_BUCKET,Items.REDSTONE,Items.COBBLESTONE,Items.COAL_BLOCK),
                List.of(Items.BUCKET,Items.STRING,Items.STRING,Items.GUNPOWDER),List.of(Items.GHAST_TEAR,Items.FEATHER,Items.FEATHER),
                List.of(Items.GLOWSTONE_DUST,Items.GLASS,Items.GLASS,BloodMagicItems.DIVINATION_SIGIL.get()));
        var results=List.of(BloodMagicItems.ARCANE_ASHES.get(),BloodMagicItems.REAGENT_WATER.get(),BloodMagicItems.REAGENT_LAVA.get(),
                BloodMagicItems.REAGENT_VOID.get(),BloodMagicItems.REAGENT_AIR.get(),BloodMagicItems.REAGENT_SIGHT.get());
        int[] costs={500,300,1000,1000,2000,500};
        var network=SoulNetworks.get(level);network.consume(ArrayChecks.OWNER,network.get(ArrayChecks.OWNER).currentEssence());network.fill(ArrayChecks.OWNER,8000,25000);
        ItemStack ashes=ItemStack.EMPTY,water=ItemStack.EMPTY;
        for(int i=0;i<ingredients.size();i++){
            table.clearContent();table.setItem(6,apprentice);
            for(int j=0;j<ingredients.get(i).size();j++)table.setItem(j,new ItemStack(ingredients.get(i).get(j)));
            int before=network.get(ArrayChecks.OWNER).currentEssence();ArrayChecks.tickTable(table,200);
            check(table.getItem(7).is(results.get(i))&&network.get(ArrayChecks.OWNER).currentEssence()==before-costs[i],"precursor recipe produces result and charges exact LP "+i);
            if(i==0)ashes=table.removeItem(7,1);if(i==1)water=table.removeItem(7,1);
        }
        check(network.get(ArrayChecks.OWNER).currentEssence()==2700,"all six precursor recipes consume 5300 LP");
        ArrayChecks.clear(level,ArrayChecks.POS);ArrayChecks.clearDrops(level);
        level.setBlockAndUpdate(ArrayChecks.POS.below(),Blocks.STONE.defaultBlockState());
        player.setPos(ArrayChecks.POS.getX()+.5,ArrayChecks.POS.getY(),ArrayChecks.POS.getZ()+2);player.setItemInHand(InteractionHand.MAIN_HAND,ashes);
        check(ashes.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ArrayChecks.POS.below()),Direction.UP,ArrayChecks.POS.below(),false))).consumesAction(),"table-produced ashes draw an array");
        var array=(AlchemyArrayBlockEntity)level.getBlockEntity(ArrayChecks.POS);
        player.setItemInHand(InteractionHand.MAIN_HAND,water);array.interact(player,InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND,slate.copy());array.interact(player,InteractionHand.MAIN_HAND);ArrayChecks.tickArray(array,200);
        check(ArrayChecks.drops(level,BloodMagicItems.WATER_SIGIL.get())==1&&network.get(ArrayChecks.OWNER).currentEssence()==2700,"altar slate and table reagent become a sigil with upstream zero direct array LP");
        table.clearContent();table.setItem(6,apprentice);ArrayChecks.clearDrops(level);
        level.destroyBlock(other,true);table.getBlockState().tick(level,tablePos,level.getRandom());
        check(level.isEmptyBlock(tablePos)&&level.isEmptyBlock(other)&&ArrayChecks.drops(level,BloodMagicItems.ALCHEMY_TABLE.get())==1,
                "breaking secondary half removes both and drops exactly one table");
        check(ArrayChecks.drops(level,BloodMagicItems.APPRENTICE_BLOOD_ORB.get())==1,"table teardown drops its only inventory once");
        altar.setStoredItem(ItemStack.EMPTY);ArrayChecks.clear(level,pos);
        for(var component:AltarStructure.components(2))level.setBlockAndUpdate(pos.offset(component.offset()),Blocks.AIR.defaultBlockState());
        ArrayChecks.clearDrops(level);return assertions;
    }
    private static ItemStack craft(ServerLevel level,List<ItemStack> stacks,Item result){
        var input=CraftingInput.of(3,3,stacks);
        var recipe=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value();
        ItemStack output=recipe.assemble(input);check(output.is(result),"survival crafting result "+result);return output;
    }
    private static ItemStack altarCraft(BloodAltarBlockEntity altar,Item input,int cost,Item result){
        altar.setStoredItem(new ItemStack(input));altar.fillMainTank(cost);
        for(int i=0;i<cost/5+35;i++)BloodAltarBlockEntity.serverTick(altar.getLevel(),altar.getBlockPos(),altar.getBlockState(),altar);
        ItemStack output=altar.getStoredItem().copy();check(output.is(result)&&altar.getAltarState().blood()==0,"altar precursor with exact LP "+result);altar.setStoredItem(ItemStack.EMPTY);return output;
    }
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("STAGE6_SMOKE FAIL: "+message);assertions++;}
}
