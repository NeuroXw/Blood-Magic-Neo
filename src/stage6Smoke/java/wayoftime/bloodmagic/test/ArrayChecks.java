package wayoftime.bloodmagic.test;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.*;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.network.connection.ConnectionType;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.block.*;
import wayoftime.bloodmagic.common.block.entity.*;
import wayoftime.bloodmagic.common.item.*;
import wayoftime.bloodmagic.common.menu.MachineMenu;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.core.data.SoulNetworks;
import wayoftime.bloodmagic.recipe.*;

final class ArrayChecks {
    static final BlockPos POS = new BlockPos(512, 180, 512);
    static final BlockPos TABLE = POS.offset(5, 0, 0);
    static final BlockPos SAVE = new BlockPos(600, 180, 600);
    static final BlockPos TABLE_SAVE = SAVE.offset(8,0,0);
    static final UUID TABLE_OWNER = UUID.fromString("e98430df-96db-4a9f-85e1-883eeea00062");
    static final UUID OWNER = UUID.fromString("e98430df-96db-4a9f-85e1-883eeea00060");
    static int assertions, phase, beginAssertions;
    static Path recipePath;
    static CompletableFuture<Void> reload;
    static long deadline;
    static AlchemyArrayBlockEntity reloadArray;
    static ServerPlayer player;
    static int begin(ServerLevel level) {
        int start = assertions;
        level.getChunk(POS); level.getChunk(TABLE); level.getChunk(SAVE);level.getChunk(TABLE_SAVE);
        level.getChunkSource().updateChunkForced(ChunkPos.containing(POS), true);
        level.getChunkSource().updateChunkForced(ChunkPos.containing(TABLE), true);
        if (Stage6SmokeChecks.restartExpected) {
            var saved = (AlchemyArrayBlockEntity) level.getBlockEntity(SAVE);
            check(saved != null && saved.progress() == 70 && OWNER.equals(saved.owner()), "world restart retains active array and owner");
            check(saved.getItem(0).is(Items.REDSTONE) && saved.getItem(1).is(BloodMagicItems.SLATE.get()), "world restart retains both array ingredients");
        }
        if(Stage6SmokeChecks.restartExpected&&Files.exists(Path.of("stage6-table-expected.txt"))){
            var saved=(AlchemyTableBlockEntity)level.getBlockEntity(TABLE_SAVE);
            check(saved!=null&&saved.progress()==80&&BlockAlchemyTable.complete(level,TABLE_SAVE,saved.getBlockState()),"world restart retains both table halves and partial progress");
            check(BloodOrbData.getBinding(saved.getItem(6)).orElseThrow().ownerId().equals(TABLE_OWNER),"world restart retains table orb owner");
            tickTable(saved,120);
            check(saved.getItem(7).is(BloodMagicItems.ARCANE_ASHES.get())&&SoulNetworks.get(level).get(TABLE_OWNER).currentEssence()==500,"restarted table completes with exactly one 500 LP payment");
        }
        clear(level, SAVE); clear(level, POS); clear(level, TABLE);
        player = MenuTestPlayer.create(level, "S6Array");
        player.setGameMode(GameType.SURVIVAL); player.setPos(POS.getX() + .5, POS.getY(), POS.getZ() + 2);
        level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());
        beginAssertions = assertions;
        return assertions - start;
    }
    static boolean afterTick(ServerLevel level) throws java.io.IOException {
        if (phase == 0) {
            testArray(level); testTable(level);
            Path pack = level.getServer().getWorldPath(LevelResource.DATAPACK_DIR).resolve("stage6_array_probe");
            recipePath = pack.resolve("data/bloodmagic/recipe/array/divinationsigil.json");
            Files.createDirectories(recipePath.getParent());
            var f = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
            String version = "[" + f.major() + "," + f.minor() + "]";
            Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Array integration probe\",\"min_format\":" + version + ",\"max_format\":" + version + "}}");
            writeRecipe("minecraft:emerald", 4, 60);
            var server = level.getServer(); server.getPackRepository().reload();
            var packs = new ArrayList<>(server.getPackRepository().getSelectedIds());
            if (!packs.contains("file/stage6_array_probe")) packs.add("file/stage6_array_probe");
            reload = server.reloadResources(packs); deadline = System.nanoTime() + 120_000_000_000L; phase = 1; return false;
        }
        if (System.nanoTime() > deadline && phase < 4) throw new IllegalStateException("Stage 6 array reload timeout");
        if (!reload.isDone()) return false;
        reload.join();
        if (phase == 1) {
            reloadArray = placeArray(level, POS); reloadArray.initialize(OWNER, Direction.NORTH);
            reloadArray.setItem(0, new ItemStack(Items.REDSTONE)); reloadArray.setItem(1, new ItemStack(BloodMagicItems.SLATE.get()));
            var networks = SoulNetworks.get(level); networks.consume(OWNER, networks.get(OWNER).currentEssence()); networks.fill(OWNER, 59, 1000);
            tickArray(reloadArray, 10);
            check(reloadArray.progress() == 0 && networks.get(OWNER).currentEssence() == 59 && !reloadArray.isEmpty(), "insufficient array LP retains ingredients and balance");
            networks.fill(OWNER, 101, 1000); tickArray(reloadArray, 2);
            check(reloadArray.progress() == 2 && networks.get(OWNER).currentEssence() == 160, "array payment only at commit");
            // Detached fixture cannot advance during the asynchronous reload.
            reloadArray = roundTrip(level, reloadArray, AlchemyArrayBlockEntity.class); reloadArray.setLevel(level);
            clear(level, POS);
            writeRecipe("minecraft:diamond", 5, 70);
            reload = level.getServer().reloadResources(level.getServer().getPackRepository().getSelectedIds()); phase = 2; return false;
        }
        if (phase == 2) {
            tickArray(reloadArray, 1);
            check(reloadArray.progress() == 1 && reloadArray.duration() == 5 && SoulNetworks.get(level).get(OWNER).currentEssence() == 160,
                    "same ID changed recipe resets progress after real reload");
            // Reattach to the real world to verify one committed drop and one LP debit.
            level.setBlockAndUpdate(POS, BloodMagicBlocks.ALCHEMY_ARRAY.get().defaultBlockState());
            level.setBlockEntity(reloadArray);
            clearDrops(level); RitualChecks.denyArrayDrop=true;
            try { tickArray(reloadArray, 4); }
            finally { RitualChecks.denyArrayDrop=false; }
            check(SoulNetworks.get(level).get(OWNER).currentEssence()==160&&!reloadArray.isEmpty()&&drops(level,Items.DIAMOND)==0,
                    "cancelled output spawn refunds LP and retains both inputs");
            tickArray(reloadArray, 1);
            check(level.getBlockState(POS).isAir() && SoulNetworks.get(level).get(OWNER).currentEssence() == 90, "array completion consumes exactly 70 LP");
            check(drops(level, Items.DIAMOND) == 1 && drops(level, Items.EMERALD) == 0 && drops(level, Items.REDSTONE) == 0, "reload produces only current result, no duplicated inputs");
            tickArray(reloadArray, 10);
            check(SoulNetworks.get(level).get(OWNER).currentEssence() == 90 && drops(level, Items.DIAMOND) == 1, "removed array cannot charge or produce twice");
            clearDrops(level);
            Files.delete(recipePath); reload = level.getServer().reloadResources(level.getServer().getPackRepository().getSelectedIds()); phase = 3; return false;
        }
        if (phase == 3) {
            var array = placeArray(level, POS); array.setItem(0, new ItemStack(Items.REDSTONE)); array.setItem(1, new ItemStack(BloodMagicItems.SLATE.get()));
            check(array.findRecipe().orElseThrow().value().lpCost() == 0, "removing override restores original zero-LP array recipe");
            clear(level, POS); clearDrops(level); phase = 4;
        }
        return true;
    }
    private static void testArray(ServerLevel level) {
        var ashes = new ItemStack(BloodMagicItems.ARCANE_ASHES.get());
        player.setItemInHand(InteractionHand.OFF_HAND, ashes);
        var hit = new BlockHitResult(Vec3.atCenterOf(POS.below()), Direction.UP, POS.below(), false);
        var context = new UseOnContext(player, InteractionHand.OFF_HAND, hit);
        check(ashes.getItem().useOn(context).consumesAction() && ashes.getDamageValue() == 1, "chalk offhand places array with one durability");
        check(level.getBlockEntity(POS) instanceof AlchemyArrayBlockEntity && player.getUUID().equals(((AlchemyArrayBlockEntity)level.getBlockEntity(POS)).owner()), "chalk sets placer UUID");
        check(!ashes.getItem().useOn(context).consumesAction() && ashes.getDamageValue() == 1, "occupied placement costs no durability");
        clear(level, POS); player.setGameMode(GameType.ADVENTURE);
        check(!ashes.getItem().useOn(context).consumesAction() && level.isEmptyBlock(POS), "chalk rejects adventure building");
        player.setGameMode(GameType.CREATIVE); ashes.getItem().useOn(context);
        check(ashes.getDamageValue() == 1, "creative chalk retains durability");
        clear(level, POS); player.setGameMode(GameType.SURVIVAL);
        var inputs = List.of(Items.REDSTONE, BloodMagicItems.REAGENT_WATER.get(), BloodMagicItems.REAGENT_LAVA.get(), BloodMagicItems.REAGENT_VOID.get(), BloodMagicItems.REAGENT_AIR.get(), BloodMagicItems.REAGENT_SIGHT.get());
        var outputs = List.of(BloodMagicItems.DIVINATION_SIGIL.get(), BloodMagicItems.WATER_SIGIL.get(), BloodMagicItems.LAVA_SIGIL.get(), BloodMagicItems.VOID_SIGIL.get(), BloodMagicItems.AIR_SIGIL.get(), BloodMagicItems.SEER_SIGIL.get());
        for (int i = 0; i < 6; i++) {
            clearDrops(level); var array = placeArray(level, POS);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(inputs.get(i), 3));
            check(array.interact(player, InteractionHand.MAIN_HAND).consumesAction() && player.getMainHandItem().getCount() == 2, "one base ingredient inserted " + i);
            var slate = i < 3 ? BloodMagicItems.SLATE.get() : BloodMagicItems.REINFORCED_SLATE.get();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(slate, 2)); array.interact(player, InteractionHand.MAIN_HAND);
            check(player.getMainHandItem().getCount() == 1 && array.findRecipe().isPresent(), "real datapack sigil recipe " + i);
            var recipe = array.findRecipe().orElseThrow().value();
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
            try { AlchemyArrayRecipe.STREAM_CODEC.encode(buf, recipe); check(recipe.sameDefinition(AlchemyArrayRecipe.STREAM_CODEC.decode(buf)) && !buf.isReadable(), "array recipe wire round trip " + i); } finally { buf.release(); }
            tickArray(array, 199);
            check(level.getBlockEntity(POS) == array && array.progress() == 199, "no early completion " + i);
            tickArray(array, 1);
            check(level.getBlockState(POS).isAir() && drops(level, outputs.get(i)) == 1, "exactly one sigil result " + i);
            check(drops(level, inputs.get(i)) == 0 && drops(level, slate) == 0, "completion consumes both inputs " + i);
        }
        clearDrops(level); var array = placeArray(level, POS);
        array.setItem(0, new ItemStack(BloodMagicItems.SLATE.get())); array.setItem(1, new ItemStack(Items.REDSTONE)); tickArray(array, 220);
        check(array.findRecipe().isEmpty() && array.progress() == 0 && !array.isEmpty(), "ordered recipe rejects reversed ingredients");
        array.setItem(0, new ItemStack(Items.REDSTONE)); array.setItem(1, new ItemStack(BloodMagicItems.SLATE.get())); tickArray(array, 20);
        var restored = roundTrip(level, array, AlchemyArrayBlockEntity.class);
        check(restored.progress() == 20 && restored.getItem(0).is(Items.REDSTONE), "array partial progress NBT round trip");
        array.removeItem(1, 1); check(array.progress() == 0, "extraction invalidates array progress");
        array.setItem(1, new ItemStack(BloodMagicItems.SLATE.get()));
        level.setBlockAndUpdate(POS.below(), Blocks.AIR.defaultBlockState());
        check(level.getBlockState(POS).isAir(), "support removal destroys array");
        check(drops(level, Items.REDSTONE) == 1 && drops(level, BloodMagicItems.SLATE.get()) == 1, "support removal refunds each ingredient once");
        clearDrops(level); level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());
    }
    private static void testTable(ServerLevel level) {
        var table = placeTable(level, TABLE);
        var orb = new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get()); BloodOrbData.bind(orb, OWNER, "Offline crafter");
        var inputs = List.of(Items.SUGAR, Items.WATER_BUCKET, Items.WATER_BUCKET);
        for (int i = 0; i < inputs.size(); i++) table.setItem(i, new ItemStack(inputs.get(i)));
        var match = level.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.TABLE_TYPE.get(), new MachineRecipeInput(List.of(table.getItem(2), table.getItem(0), table.getItem(1))), level);
        check(match.isPresent() && match.get().value().syphon() == 300, "unordered table recipe and upstream cost");
        check(level.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.TABLE_TYPE.get(), new MachineRecipeInput(List.of(new ItemStack(Items.SUGAR), new ItemStack(Items.WATER_BUCKET, 2))), level).isEmpty(), "two bucket ingredients require two slots");
        tickTable(table, 220); check(table.progress() == 0 && table.getItem(7).isEmpty(), "table requires bound orb");
        table.setItem(6, orb);
        var networks = SoulNetworks.get(level); networks.consume(OWNER, networks.get(OWNER).currentEssence()); networks.fill(OWNER, 299, 1000);
        tickTable(table, 220); check(table.progress() == 0 && networks.get(OWNER).currentEssence() == 299, "table cannot spend insufficient LP");
        networks.fill(OWNER, 201, 1000);
        table.setItem(7, new ItemStack(Items.COBBLESTONE)); tickTable(table, 10);
        check(table.progress() == 0 && networks.get(OWNER).currentEssence() == 500, "blocked output cannot spend LP");
        table.setItem(7, ItemStack.EMPTY); tickTable(table, 199);
        check(table.progress() == 199 && networks.get(OWNER).currentEssence() == 500, "table progress charges on completion only");
        var saved = roundTrip(level, table, AlchemyTableBlockEntity.class);
        check(saved.progress() == 199 && ItemStack.matches(saved.getItem(6), orb), "table stores progress and bound orb");
        tickTable(table, 1);
        check(table.getItem(7).is(BloodMagicItems.REAGENT_WATER.get()) && networks.get(OWNER).currentEssence() == 200, "table crafts water reagent for exactly 300 LP");
        check(table.getItem(1).is(Items.BUCKET) && table.getItem(2).is(Items.BUCKET) && table.getItem(0).isEmpty(), "26.1 crafting remainders return both buckets");
        check(ItemStack.matches(table.getItem(6), orb), "table retains orb and binding");
        player.setPos(TABLE.getX() + .5, TABLE.getY(), TABLE.getZ() + 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        PlayerInteractionChecks.useOn(player, TABLE);
        check(player.containerMenu instanceof MachineMenu, "empty-hand right-click packet opens table menu");
        var menu = (MachineMenu) player.containerMenu;
        check(menu.slots.size() == 44 && !menu.slots.get(7).mayPlace(new ItemStack(Items.STONE)), "table menu inventory and protected output");
        check(!menu.quickMoveStack(player, 7).isEmpty() && table.getItem(7).isEmpty(), "table output shift click");
        player.closeContainer();
        table.clearContent(); level.setBlockAndUpdate(BlockAlchemyTable.other(TABLE, table.getBlockState()), Blocks.AIR.defaultBlockState());
        table.setItem(0, new ItemStack(Items.REDSTONE)); table.setItem(1, new ItemStack(Items.WHITE_DYE)); table.setItem(2, new ItemStack(Items.GUNPOWDER)); table.setItem(3, new ItemStack(Items.COAL)); table.setItem(6, orb);
        networks.fill(OWNER, 1000, 2000); int before = networks.get(OWNER).currentEssence(); tickTable(table, 250);
        check(!BlockAlchemyTable.complete(level, TABLE, table.getBlockState()) && table.progress() == 0 && networks.get(OWNER).currentEssence() == before, "broken two-block table pauses without spending LP");
        clear(level, TABLE); clearDrops(level);
        player.setPos(POS.getX() + .5, POS.getY(), POS.getZ() + 2);
    }
    static int finish(ServerLevel level) throws java.io.IOException {
        var saved = placeArray(level, SAVE); saved.initialize(OWNER, Direction.SOUTH);
        saved.setItem(0, new ItemStack(Items.REDSTONE)); saved.setItem(1, new ItemStack(BloodMagicItems.SLATE.get())); tickArray(saved, 70);
        check(saved.progress() == 70, "prepare real restart fixture at tick 70");
        Files.writeString(Path.of("stage6-array-expected.txt"), "70\n");
        var table=placeTable(level,TABLE_SAVE);
        var orb=new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get());BloodOrbData.bind(orb,TABLE_OWNER,"Table restart");
        table.setItem(6,orb);
        var inputs=List.of(Items.REDSTONE,Items.WHITE_DYE,Items.GUNPOWDER,Items.COAL);
        for(int i=0;i<inputs.size();i++)table.setItem(i,new ItemStack(inputs.get(i)));
        var network=SoulNetworks.get(level);network.consume(TABLE_OWNER,network.get(TABLE_OWNER).currentEssence());network.fill(TABLE_OWNER,1000,5000);
        tickTable(table,80);check(table.progress()==80,"prepare real two-block table restart fixture");
        Files.writeString(Path.of("stage6-table-expected.txt"),"80 / 1000 LP\n");
        level.getChunkSource().updateChunkForced(ChunkPos.containing(POS), false);
        level.getChunkSource().updateChunkForced(ChunkPos.containing(TABLE), false);
        return assertions - beginAssertions;
    }
    static AlchemyArrayBlockEntity placeArray(ServerLevel level, BlockPos pos) {
        clear(level, pos); level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, BloodMagicBlocks.ALCHEMY_ARRAY.get().defaultBlockState());
        return (AlchemyArrayBlockEntity) level.getBlockEntity(pos);
    }
    static AlchemyTableBlockEntity placeTable(ServerLevel level, BlockPos pos) {
        clear(level, pos);
        var state = BloodMagicBlocks.ALCHEMY_TABLE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state); level.setBlockAndUpdate(BlockAlchemyTable.other(pos, state), state.setValue(BlockAlchemyTable.PART, BedPart.HEAD));
        return (AlchemyTableBlockEntity) level.getBlockEntity(pos);
    }
    static void clear(ServerLevel level, BlockPos pos) { if (level.getBlockEntity(pos) instanceof MachineInventory m) m.clearContent(); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
    static void tickArray(AlchemyArrayBlockEntity array, int n) { for (int i = 0; i < n; i++) AlchemyArrayBlockEntity.serverTick(array.getLevel(), array.getBlockPos(), array.getBlockState(), array); }
    static void tickTable(AlchemyTableBlockEntity table, int n) { for (int i = 0; i < n; i++) AlchemyTableBlockEntity.serverTick(table.getLevel(), table.getBlockPos(), table.getBlockState(), table); }
    static <T extends BlockEntity> T roundTrip(ServerLevel level, T entity, Class<T> type) {
        return type.cast(BlockEntity.loadStatic(entity.getBlockPos(), entity.getBlockState(), entity.saveWithFullMetadata(level.registryAccess()), level.registryAccess()));
    }
    static AABB area() { return new AABB(POS).inflate(12); }
    static long drops(ServerLevel level, Item item) { return level.getEntitiesOfClass(ItemEntity.class, area()).stream().filter(e -> e.getItem().is(item)).mapToLong(e -> e.getItem().getCount()).sum(); }
    static void clearDrops(ServerLevel level) { level.getEntitiesOfClass(ItemEntity.class, area()).forEach(ItemEntity::discard); }
    static void writeRecipe(String output, int ticks, int lp) throws java.io.IOException {
        Files.writeString(recipePath, "{\"type\":\"bloodmagic:array\",\"baseinput\":\"minecraft:redstone\",\"addedinput\":\"bloodmagic:blankslate\",\"output\":{\"id\":\"" + output + "\"},\"ticks\":" + ticks + ",\"lpCost\":" + lp + "}");
    }
    static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException("STAGE6_SMOKE FAIL: " + message); assertions++; }
}
