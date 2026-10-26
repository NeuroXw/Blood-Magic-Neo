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
import wayoftime.bloodmagic.recipe.*;
import wayoftime.bloodmagic.will.*;

final class ForgeChecks {
    static final BlockPos POS = ArrayChecks.POS.offset(8, 0, 0);
    static final BlockPos SAVE = ArrayChecks.SAVE.offset(4, 0, 0);
    static int assertions;
    static int begin(ServerLevel level) {
        int start = assertions; level.getChunk(SAVE);
        if (Stage6SmokeChecks.restartExpected) {
            var forge = (HellfireForgeBlockEntity) level.getBlockEntity(SAVE);
            check(forge != null && forge.progress() == 40 && forge.will() == 5.25, "world restart retains forge fractional will and progress");
        }
        ArrayChecks.clear(level, SAVE);
        return assertions - start;
    }
    static int run(ServerLevel level) {
        int start = assertions;
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
    static int finish(ServerLevel level) throws java.io.IOException {
        var forge=place(level,SAVE); pettyInputs(forge); forge.setItem(4,will(BloodMagicItems.RAW_WILL.get(),5.25)); tick(forge,40);
        check(forge.progress()==40 && forge.will()==5.25,"prepare forge restart fixture");
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
        try (var stream = ForgeChecks.class.getResourceAsStream("/assets/bloodmagic/models/block/soulforge.json");
                var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(stream), java.nio.charset.StandardCharsets.UTF_8)) {
            var model = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var element = model.getAsJsonArray("elements").get(0).getAsJsonObject();
            var from = element.getAsJsonArray("from"); var to = element.getAsJsonArray("to");
            var bounds = new net.minecraft.world.phys.AABB(from.get(0).getAsDouble() / 16, from.get(1).getAsDouble() / 16,
                    from.get(2).getAsDouble() / 16, to.get(0).getAsDouble() / 16, to.get(1).getAsDouble() / 16, to.get(2).getAsDouble() / 16);
            check(state.getShape(level, POS).bounds().equals(bounds), "forge selection bounds match visible model");
            check(state.getCollisionShape(level, POS).bounds().equals(bounds), "forge collision bounds match visible model");
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    static HellfireForgeBlockEntity place(ServerLevel level,BlockPos pos) { ArrayChecks.clear(level,pos); level.setBlockAndUpdate(pos,BloodMagicBlocks.HELLFIRE_FORGE.get().defaultBlockState()); return (HellfireForgeBlockEntity)level.getBlockEntity(pos); }
    static ItemStack will(Item item,double amount) { var stack=new ItemStack(item); ItemDemonWill.setAmount(stack,amount); return stack; }
    static void pettyInputs(HellfireForgeBlockEntity forge) { var ingredients=List.of(Items.REDSTONE,Items.GOLD_INGOT,Items.GLASS,Items.LAPIS_LAZULI); for(int i=0;i<4;i++)forge.setItem(i,new ItemStack(ingredients.get(i))); }
    static void tick(HellfireForgeBlockEntity forge,int count) { for(int i=0;i<count;i++)HellfireForgeBlockEntity.serverTick(forge.getLevel(),forge.getBlockPos(),forge.getBlockState(),forge); }
    static void check(boolean condition,String message) { if(!condition)throw new IllegalStateException("STAGE6_SMOKE FAIL: "+message); assertions++; }
}
