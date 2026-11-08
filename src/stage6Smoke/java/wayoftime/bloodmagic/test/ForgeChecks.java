package wayoftime.bloodmagic.test;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import wayoftime.bloodmagic.common.block.*;
import wayoftime.bloodmagic.common.block.entity.*;
import wayoftime.bloodmagic.common.item.*;
import wayoftime.bloodmagic.common.menu.MachineMenu;
import wayoftime.bloodmagic.common.registries.*;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;
import wayoftime.bloodmagic.common.loot.SetWillFraction;
import wayoftime.bloodmagic.common.loot.SetWillRange;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import wayoftime.bloodmagic.recipe.*;
import wayoftime.bloodmagic.will.*;

final class ForgeChecks {
    static final BlockPos POS = ArrayChecks.POS.offset(8, 0, 0);
    static final BlockPos SAVE = ArrayChecks.SAVE.offset(4, 0, 0);
    static final BlockPos WILL_SAVE = new BlockPos(32, 100, 32);
    static int assertions;
    static int begin(ServerLevel level) {
        int start = assertions; level.getChunk(SAVE);
        if (Stage6SmokeChecks.willRestartExpected) {
            level.getChunk(WILL_SAVE);
            check(WorldDemonWillHandler.getCurrentWill(level, WILL_SAVE, EnumDemonWillType.VENGEFUL) == 77.25,
                    "chunk attachment persists typed fractional will across server restart");
        }
        if (Stage6SmokeChecks.restartExpected) {
            var forge = (HellfireForgeBlockEntity) level.getBlockEntity(SAVE);
            check(forge != null && forge.progress() == 40 && forge.will() == 5.25, "world restart retains forge fractional will and progress");
        }
        ArrayChecks.clear(level, SAVE);
        return assertions - start;
    }
    static int run(ServerLevel level) {
        int start = assertions;
        checkWillSystem(level);
        var forge = place(level, POS);
        checkSurroundingFaces(level, forge);
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, 4097}) {
            boolean rejected = false; try { new DemonWill(invalid); } catch (IllegalArgumentException e) { rejected = true; }
            check(rejected, "invalid will rejected " + invalid);
        }
        var raw = will(BloodMagicItems.RAW_WILL.get(), 5.25);
        var tiny = will(BloodMagicItems.PETTY_GEM.get(), 63);
        check(ItemDemonWill.transfer(raw, tiny) == 1 && ItemDemonWill.amount(raw) == 4.25 && ItemDemonWill.amount(tiny) == 64, "partial will transfer conserves fractions at capacity");
        check(ItemDemonWill.transfer(raw, tiny) == 0 && ItemDemonWill.transfer(tiny, tiny) == 0, "full or same-stack transfer cannot mint will");
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try { ItemStack.STREAM_CODEC.encode(buf, raw); check(ItemStack.matches(raw, ItemStack.STREAM_CODEC.decode(buf)) && !buf.isReadable(), "will item components wire round trip"); } finally { buf.release(); }
        var encoded = DemonWill.CODEC.encodeStart(JsonOps.INSTANCE, new DemonWill(1.125)).getOrThrow();
        check(DemonWill.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().amount() == 1.125, "fractional will data codec");
        pettyInputs(forge); tick(forge, 120);
        check(forge.progress() == 0 && forge.getItem(5).isEmpty(), "forge requires will holder");
        forge.setItem(4, will(BloodMagicItems.RAW_WILL.get(), .75)); tick(forge, 120);
        check(forge.progress() == 0 && forge.will() == .75, "forge minimum threshold checked without debit");
        forge.setItem(4, will(BloodMagicItems.RAW_WILL.get(), 5.25));
        var recipe = level.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.FORGE_TYPE.get(), new MachineRecipeInput(List.of(forge.getItem(3),forge.getItem(0),forge.getItem(2),forge.getItem(1))), level).orElseThrow().value();
        check(recipe.minimumWill() == 1 && recipe.drain() == 1 && recipe.ticks() == 100, "upstream forge recipe, unordered matching and duration");
        buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try { HellfireForgeRecipe.STREAM_CODEC.encode(buf, recipe); check(recipe.sameDefinition(HellfireForgeRecipe.STREAM_CODEC.decode(buf)) && !buf.isReadable(), "forge recipe wire codec"); } finally { buf.release(); }
        forge.setItem(5, new ItemStack(Items.COBBLESTONE)); tick(forge, 120);
        check(forge.will() == 5.25 && forge.progress() == 0, "blocked forge output consumes nothing");
        forge.setItem(5, ItemStack.EMPTY); tick(forge, 99);
        check(forge.progress() == 99 && forge.will() == 5.25, "forge has no early will debit");
        var restored = ArrayChecks.roundTrip(level, forge, HellfireForgeBlockEntity.class);
        check(restored.progress() == 99 && restored.will() == 5.25, "forge save preserves energy and partial craft");
        tick(forge, 1);
        check(forge.getItem(5).is(BloodMagicItems.PETTY_GEM.get()) && forge.will() == 4.25, "forge completes for exactly one will");
        check(forge.getItem(0).isEmpty() && forge.getItem(1).isEmpty() && forge.getItem(2).isEmpty() && forge.getItem(3).isEmpty(), "forge consumes exactly four ingredients");
        forge.clearContent();
        forge.setItem(0, will(BloodMagicItems.PETTY_GEM.get(), 64)); forge.setItem(1, new ItemStack(Items.DIAMOND)); forge.setItem(2, new ItemStack(Items.REDSTONE_BLOCK)); forge.setItem(3, new ItemStack(Items.LAPIS_BLOCK));
        tick(forge, 100);
        check(forge.getItem(5).is(BloodMagicItems.LESSER_GEM.get()) && ItemDemonWill.amount(forge.getItem(5)) == 44 && forge.getItem(0).isEmpty(), "gem upgrade preserves 64 minus 20 will without separate catalyst");
        forge.clearContent(); pettyInputs(forge); forge.setItem(4, will(BloodMagicItems.RAW_WILL.get(), 1)); tick(forge,100);
        check(forge.getItem(4).isEmpty() && forge.getItem(5).is(BloodMagicItems.PETTY_GEM.get()), "exhausted raw will disappears once");
        var player = MenuTestPlayer.create(level, "S6Forge"); player.setPos(POS.getX(), POS.getY(), POS.getZ()+2);
        PlayerInteractionChecks.useOn(player, POS);
        check(player.containerMenu instanceof MachineMenu, "empty-hand right-click packet opens forge menu");
        var menu = (MachineMenu) player.containerMenu;
        check(menu.slots.size() == 42 && !menu.isTable() && !menu.slots.get(5).mayPlace(raw), "forge menu shape and output restrictions");
        check(!menu.quickMoveStack(player,5).isEmpty() && forge.getItem(5).isEmpty(), "forge result shift click"); player.closeContainer();
        check(Arrays.equals(forge.getSlotsForFace(Direction.DOWN), new int[]{5}) && !forge.canTakeItemThroughFace(4, raw, Direction.DOWN), "hopper extracts output only");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BloodMagicItems.SOUL_SNARE.get(),2));
        check(player.getOffhandItem().getItem().use(level,player,InteractionHand.OFF_HAND).consumesAction() && player.getOffhandItem().getCount()==1, "snare offhand use consumes exactly one");
        var mob = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        mob.addEffect(new MobEffectInstance(BloodMagicWill.SNARED,300));
        var dropList = new ArrayList<ItemEntity>();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new LivingDropsEvent(mob, mob.damageSources().generic(), dropList, true));
        check(dropList.size()==1 && ItemDemonWill.amount(dropList.getFirst().getItem())>=1 && ItemDemonWill.amount(dropList.getFirst().getItem())<5, "registered snared mob drop event creates fractional raw will");
        mob.removeEffect(BloodMagicWill.SNARED); dropList.clear();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new LivingDropsEvent(mob, mob.damageSources().generic(), dropList, true));
        check(dropList.isEmpty(), "unsnared mobs yield no free will");
        forge.clearContent(); forge.setItem(4, will(BloodMagicItems.PETTY_GEM.get(),12.5)); ArrayChecks.clearDrops(level);
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        var drops=level.getEntitiesOfClass(ItemEntity.class,ArrayChecks.area()).stream().filter(e->e.getItem().is(BloodMagicItems.PETTY_GEM.get())).toList();
        check(drops.size()==1 && ItemDemonWill.amount(drops.getFirst().getItem())==12.5, "forge removal drops stored will exactly once");
        ArrayChecks.clearDrops(level);
        return assertions-start;
    }
    private static void checkWillSystem(ServerLevel level) {
        check(EnumDemonWillType.DEFAULT.getSerializedName().equals("default")
                && EnumDemonWillType.CORROSIVE.getSerializedName().equals("corrosive")
                && EnumDemonWillType.values().length == 5, "upstream will types and serialized names");
        check(wayoftime.bloodmagic.common.block.BlockDemonCrystal.ATTACHED.getName().equals("attached"), "cluster blockstate uses upstream attached property id");
        var corrosiveSoul = will(BloodMagicItems.CORROSIVE_WILL.get(), 12.5);
        var typedGem = new ItemStack(BloodMagicItems.PETTY_GEM.get());
        check(ItemDemonWill.transfer(corrosiveSoul, typedGem) == 12.5
                && ItemDemonWill.type(typedGem) == EnumDemonWillType.CORROSIVE, "will gem adopts the source aspect");
        check(ItemDemonWill.fill(typedGem, EnumDemonWillType.DEFAULT, 1, true) == 0
                && ItemDemonWill.amount(typedGem) == 12.5, "a gem cannot mix will aspects");
        var typeBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            ItemStack.STREAM_CODEC.encode(typeBuf, typedGem);
            var decoded = ItemStack.STREAM_CODEC.decode(typeBuf);
            check(ItemDemonWill.type(decoded) == EnumDemonWillType.CORROSIVE && ItemDemonWill.amount(decoded) == 12.5,
                    "typed gem components survive item network encoding");
        } finally { typeBuf.release(); }
        var crystalStack = new ItemStack(BloodMagicItems.DESTRUCTIVE_CRYSTAL.get(), 3);
        check(ItemDemonCrystal.getWill(crystalStack) == 150 && ItemDemonCrystal.drainWill(crystalStack, 75) == 50
                && crystalStack.getCount() == 2, "discrete crystals drain whole 50-will units");
        var holder = new DemonWillHolder(); holder.addWill(EnumDemonWillType.STEADFAST, 17.25);
        var encodedHolder = DemonWillHolder.CODEC.encodeStart(JsonOps.INSTANCE, holder).getOrThrow();
        check(DemonWillHolder.CODEC.parse(JsonOps.INSTANCE, encodedHolder).getOrThrow().getWill(EnumDemonWillType.STEADFAST) == 17.25,
                "typed aura attachment codec preserves fractional values");
        var rangeCodec = SetWillRange.CODEC.codec();
        var rangeJson = rangeCodec.encodeStart(JsonOps.INSTANCE, new SetWillRange(List.of(), UniformGenerator.between(1, 5))).getOrThrow();
        check(rangeCodec.parse(JsonOps.INSTANCE, rangeJson).result().isPresent(), "set_will_range loot function codec round trip");
        var fractionCodec = SetWillFraction.CODEC.codec();
        var fractionJson = fractionCodec.encodeStart(JsonOps.INSTANCE, new SetWillFraction(List.of(), UniformGenerator.between(.1F, .5F))).getOrThrow();
        check(fractionCodec.parse(JsonOps.INSTANCE, fractionJson).result().isPresent(), "set_will_fraction loot function codec round trip");

        level.getChunk(WILL_SAVE);
        if (!Stage6SmokeChecks.willRestartExpected) {
            var persistenceAura = WorldDemonWillHandler.getWillHolder(level, WILL_SAVE); if (persistenceAura != null) persistenceAura.clearWill();
            WorldDemonWillHandler.fillWill(level, WILL_SAVE, EnumDemonWillType.VENGEFUL, 77.25, true);
        }

        BlockPos cruciblePos = new BlockPos(192, 100, 192);
        ArrayChecks.clear(level, cruciblePos);
        level.setBlockAndUpdate(cruciblePos, BloodMagicBlocks.DEMON_CRUCIBLE.get().defaultBlockState());
        var crucible = (DemonCrucibleBlockEntity) level.getBlockEntity(cruciblePos);
        var gem = new ItemStack(BloodMagicItems.PETTY_GEM.get()); crucible.setItem(0, gem);
        var crucibleAura = WorldDemonWillHandler.getWillHolder(level, cruciblePos); if (crucibleAura != null) crucibleAura.clearWill();
        WorldDemonWillHandler.fillWill(level, cruciblePos, EnumDemonWillType.CORROSIVE, 30, true);
        DemonCrucibleBlockEntity.serverTick(level, cruciblePos, crucible.getBlockState(), crucible);
        DemonCrucibleBlockEntity.serverTick(level, cruciblePos, crucible.getBlockState(), crucible);
        check(ItemDemonWill.amount(crucible.getItem(0)) == 10
                && ItemDemonWill.type(crucible.getItem(0)) == EnumDemonWillType.CORROSIVE
                && WorldDemonWillHandler.getCurrentWill(level, cruciblePos, EnumDemonWillType.CORROSIVE) == 20,
                "unpowered crucible transfers aura into its matching gem: power=" + level.hasNeighborSignal(cruciblePos) + ", amount=" + ItemDemonWill.amount(crucible.getItem(0)) + ", type=" + ItemDemonWill.type(crucible.getItem(0)) + ", aura=" + WorldDemonWillHandler.getCurrentWill(level, cruciblePos, EnumDemonWillType.CORROSIVE));

        BlockPos clusterPos = POS.offset(4, 0, 0);
        ArrayChecks.clear(level, clusterPos);
        level.setBlockAndUpdate(clusterPos, BloodMagicBlocks.CORROSIVE_DEMON_CRYSTAL.get().defaultBlockState());
        var cluster = (DemonCrystalBlockEntity) level.getBlockEntity(clusterPos);
        check(BloodMagicItems.CORROSIVE_CATALYST.get().applyCatalyst(cluster)
                && cluster.getInjectedWill() == 200, "matching crystal catalyst injects growth buffer");
        check(!BloodMagicItems.RAW_CATALYST.get().applyCatalyst(cluster),
                "crystal catalyst rejects the wrong aspect");

        BlockPos crystallizerPos = POS.offset(6, 0, 0);
        ArrayChecks.clear(level, crystallizerPos);
        var crystallizerAura = WorldDemonWillHandler.getWillHolder(level, crystallizerPos); if (crystallizerAura != null) crystallizerAura.clearWill();
        WorldDemonWillHandler.fillWill(level, crystallizerPos, EnumDemonWillType.DEFAULT, 100, true);
        level.setBlockAndUpdate(crystallizerPos, BloodMagicBlocks.DEMON_CRYSTALLIZER.get().defaultBlockState());
        var crystallizer = (DemonCrystallizerBlockEntity) level.getBlockEntity(crystallizerPos);
        for (int i = 0; i < 1000; i++) DemonCrystallizerBlockEntity.serverTick(level, crystallizerPos, crystallizer.getBlockState(), crystallizer);
        check(level.getBlockState(crystallizerPos.above()).is(BloodMagicBlocks.RAW_DEMON_CRYSTAL.get())
                && WorldDemonWillHandler.getCurrentWill(level, crystallizerPos, EnumDemonWillType.DEFAULT) == 1,
                "crystallizer forms a cluster after consuming 99 matching will");
        ArrayChecks.clear(level, cruciblePos); ArrayChecks.clear(level, clusterPos); ArrayChecks.clear(level, crystallizerPos);
    }
    static int finish(ServerLevel level) throws java.io.IOException {
        var forge=place(level,SAVE); pettyInputs(forge); forge.setItem(4,will(BloodMagicItems.RAW_WILL.get(),5.25)); tick(forge,40);
        check(forge.progress()==40 && forge.will()==5.25,"prepare forge restart fixture");
        check(WorldDemonWillHandler.getCurrentWill(level, WILL_SAVE, EnumDemonWillType.VENGEFUL) == 77.25,"prepare typed chunk aura persistence fixture: actual=" + WorldDemonWillHandler.getCurrentWill(level, WILL_SAVE, EnumDemonWillType.VENGEFUL));
        Files.writeString(Path.of("stage6-forge-expected.txt"),"40 / 5.25\n"); return 1;
    }
    private static void checkSurroundingFaces(ServerLevel level, HellfireForgeBlockEntity forge) {
        var state = forge.getBlockState();
        // Use the same face-culling decision as chunk rendering. Solid neighbors must
        // keep their inward faces: the smaller forge leaves those faces visible.
        for (var neighbor : List.of(Blocks.DIAMOND_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(),
                Blocks.GRASS_BLOCK.defaultBlockState(), BloodMagicBlocks.HELLFIRE_FORGE.get().defaultBlockState())) {
            for (Direction direction : Direction.values()) {
                check(net.minecraft.world.level.block.Block.shouldRenderFace(neighbor, state, direction),
                        "forge keeps neighboring face visible: " + neighbor + "/" + direction);
            }
        }
        check(!net.minecraft.world.level.block.Block.shouldRenderFace(Blocks.STONE.defaultBlockState(),
                Blocks.STONE.defaultBlockState(), Direction.NORTH), "solid neighbors still cull their shared face");
        // Read the visual bounds independently so future model edits cannot silently
        // reintroduce an invisible full-block collision or selection box.
        try (var stream = ForgeChecks.class.getResourceAsStream("/assets/bloodmagic/models/block/blockhellfireforge.obj");
                var reader = new java.io.BufferedReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(stream), java.nio.charset.StandardCharsets.UTF_8))) {
            double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            for (String line; (line = reader.readLine()) != null; ) {
                if (!line.startsWith("v ")) continue;
                String[] p = line.trim().split("\\s+");
                for (int i = 0; i < 3; i++) { double v = Double.parseDouble(p[i + 1]); min[i] = Math.min(min[i], v); max[i] = Math.max(max[i], v); }
            }
            var bounds = new net.minecraft.world.phys.AABB(Math.max(0, min[0]), Math.max(0, min[1]), Math.max(0, min[2]), max[0], max[1], max[2]);
            check(state.getShape(level, POS).bounds().equals(bounds), "forge selection bounds match visible OBJ model: " + bounds);
            check(state.getCollisionShape(level, POS).bounds().equals(bounds), "forge collision bounds match visible OBJ model");
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    static HellfireForgeBlockEntity place(ServerLevel level,BlockPos pos) { ArrayChecks.clear(level,pos); level.setBlockAndUpdate(pos,BloodMagicBlocks.HELLFIRE_FORGE.get().defaultBlockState()); return (HellfireForgeBlockEntity)level.getBlockEntity(pos); }
    static ItemStack will(Item item,double amount) { var stack=new ItemStack(item); ItemDemonWill.setAmount(stack,amount); return stack; }
    static void pettyInputs(HellfireForgeBlockEntity forge) { var ingredients=List.of(Items.REDSTONE,Items.GOLD_INGOT,Items.GLASS,Items.LAPIS_LAZULI); for(int i=0;i<4;i++)forge.setItem(i,new ItemStack(ingredients.get(i))); }
    static void tick(HellfireForgeBlockEntity forge,int count) { for(int i=0;i<count;i++)HellfireForgeBlockEntity.serverTick(forge.getLevel(),forge.getBlockPos(),forge.getBlockState(),forge); }
    static void check(boolean condition,String message) { if(!condition)throw new IllegalStateException("STAGE6_SMOKE FAIL: "+message); assertions++; }
}
