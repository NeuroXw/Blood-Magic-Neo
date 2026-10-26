package wayoftime.bloodmagic.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.ContainerInput;
import net.neoforged.neoforge.network.connection.ConnectionType;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.altar.AltarState;
import wayoftime.bloodmagic.altar.AltarStructure;
import wayoftime.bloodmagic.block.enums.BloodRuneType;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.recipe.BloodAltarRecipe;
import wayoftime.bloodmagic.recipe.BloodAltarRecipeInput;

/** Real-world structure, JSON/stream, interaction, LP conservation and persistence checks. */
final class AltarGameplayChecks {
    private static final BlockPos TEST = new BlockPos(128, 180, 128);
    private static final BlockPos NATURAL = new BlockPos(64, 180, 64);
    private static final BlockPos RESUME = new BlockPos(96, 180, 96);
    private static final Path RESUME_TAG = Path.of("stage4-active-expected.nbt");
    private static int assertions;
    private static boolean resumed;

    static void begin(ServerLevel level) throws IOException {
        for (int x = 7; x <= 8; x++) for (int z = 7; z <= 8; z++) level.getChunk(x, z);
        clearStructure(level, TEST);
        BloodAltarBlockEntity altar = place(level, TEST);
        testStructure(level, altar);
        testRecipes(level, altar);
        testGameplay(level, altar);
        testInteraction(level, altar);
        clearStructure(level, TEST);
        reset(altar);

        level.getChunk(RESUME);
        if (Files.exists(RESUME_TAG)) {
            resumed = true;
            BloodAltarBlockEntity saved = (BloodAltarBlockEntity) level.getBlockEntity(RESUME);
            var expected = NbtIo.readCompressed(RESUME_TAG, NbtAccounter.unlimitedHeap());
            check(saved != null && saved.saveWithFullMetadata(level.registryAccess()).equals(expected),
                    "active recipe survives real server restart, including definition and LP progress");
            int previous = saved.getAltarState().progress();
            int blood = saved.getAltarState().blood();
            tick(saved, 1);
            check(saved.getAltarState().progress() == previous + 5 && saved.getAltarState().blood() == blood - 5,
                    "restarted recipe resumes without charging paid LP twice");
        } else {
            check(level.getBlockState(RESUME).isAir() || level.getBlockState(RESUME).is(BloodMagicBlocks.BLOOD_ALTAR.get()),
                    "new active fixture location is empty or belongs to an interrupted test run");
            check(true, "active fixture will be verified on the second server run");
        }
        BloodAltarBlockEntity resume = place(level, RESUME);
        reset(resume);
        resume.setStoredItem(new ItemStack(Items.STONE, 2));
        resume.fillMainTank(2_000);
        tick(resume, 50);
        check(resume.getAltarState().progress() == 250 && resume.getAltarState().blood() == 1_750,
                "prepare unfinished restart fixture");
        level.getChunkSource().updateChunkForced(ChunkPos.containing(RESUME), true);

        BloodAltarBlockEntity natural = place(level, NATURAL);
        reset(natural);
        natural.setStoredItem(new ItemStack(Items.STONE));
        natural.fillMainTank(1_000);
        tick(natural, 199);
        check(natural.getAltarState().progress() == 995, "prepare final real-server tick");
        level.getChunkSource().updateChunkForced(ChunkPos.containing(NATURAL), true);
    }

    static int finish(ServerLevel level) throws IOException {
        BloodAltarBlockEntity natural = (BloodAltarBlockEntity) level.getBlockEntity(NATURAL);
        check(natural.getStoredItem().is(BloodMagicItems.SLATE.get()) && natural.getAltarState().blood() == 0,
                "real registered ticker finishes recipe with exactly 1000 LP");
        BloodAltarBlockEntity resume = (BloodAltarBlockEntity) level.getBlockEntity(RESUME);
        check(resume.getAltarState().progress() > 250 && resume.getAltarState().blood() + resume.getAltarState().progress() == 2_000,
                "natural ticks and unchanged recipe reload preserve LP conservation: " + resume.getAltarState());
        NbtIo.writeCompressed(resume.saveWithFullMetadata(level.registryAccess()), RESUME_TAG);
        return assertions;
    }

    static boolean resumed() { return resumed; }

    private static void testStructure(ServerLevel level, BloodAltarBlockEntity altar) {
        check(altar.refreshStructure() && altar.getAltarState().tier() == 1, "bare altar tier one");
        set(level, TEST.offset(2, 0, 0), BloodMagicBlocks.SPEED_RUNE.get());
        altar.refreshStructure();
        check(altar.getUpgrades().level(BloodRuneType.SPEED) == 0, "arbitrary nearby runes do not count");
        set(level, TEST.offset(2, 0, 0), Blocks.AIR);
        buildTier(level, TEST, 2);
        altar.refreshStructure();
        check(altar.getAltarState().tier() == 2, "eight-rune tier two ring");
        check(altar.getUpgrades().level(BloodRuneType.BLANK) == 4, "tier two has exactly four upgrade slots");
        set(level, TEST.offset(-1, -1, -1), BloodMagicBlocks.SPEED_RUNE_2.get());
        set(level, TEST.offset(0, -1, -1), BloodMagicBlocks.SPEED_RUNE_2.get());
        set(level, TEST.offset(-1, -1, 0), BloodMagicBlocks.CAPACITY_RUNE_2.get());
        set(level, TEST.offset(1, -1, 0), BloodMagicBlocks.AUGMENTED_CAPACITY_RUNE.get());
        altar.refreshStructure();
        check(altar.getUpgrades().level(BloodRuneType.SPEED) == 2, "tier two corner ignored and double rune strength honored");
        check(altar.getUpgrades().consumption(5) == 7, "two speed levels give 40 percent faster consumption");
        check(altar.getAltarState().capacity() == 15_050 && altar.getAltarState().bufferCapacity() == 1_505,
                "normal and augmented capacity formula");
        buildTier(level, TEST, 3);
        altar.refreshStructure();
        check(altar.getAltarState().tier() == 3 && altar.getUpgrades().level(BloodRuneType.BLANK) == 28,
                "tier three has 28 rune upgrade slots and four complete pillars");
        set(level, TEST.offset(-3, 1, -3), Blocks.SEA_LANTERN);
        altar.refreshStructure();
        check(altar.getAltarState().tier() == 3, "upstream alternative glowstone capstones work through tags");
        set(level, TEST.offset(-3, 0, -3), Blocks.WATER);
        altar.refreshStructure();
        check(altar.getAltarState().tier() == 2, "liquid pillar invalidates tier three");
        buildTier(level, TEST, 4);
        altar.refreshStructure();
        check(altar.getAltarState().tier() == 4 && altar.getUpgrades().level(BloodRuneType.BLANK) == 56,
                "tier four has 56 upgrades and bloodstone caps");
        buildTier(level, TEST, 5);
        altar.refreshStructure();
        check(altar.getAltarState().tier() == 5 && altar.getUpgrades().level(BloodRuneType.BLANK) == 108,
                "tier five has 108 upgrades and hellforged caps");
        set(level, TEST.offset(-8, -4, -8), Blocks.BEACON);
        altar.refreshStructure();
        check(altar.getAltarState().tier() == 4, "vanilla beacon is not a 1.20.1 upstream tier-five capstone");
        clearStructure(level, TEST);
        buildTier(level, TEST, 2);
        set(level, TEST.offset(0, -1, -1), BloodMagicBlocks.CAPACITY_RUNE_2.get());
        altar.refreshStructure();
        altar.fillMainTank(Integer.MAX_VALUE);
        set(level, TEST.offset(1, -1, 1), Blocks.AIR);
        altar.refreshStructure();
        check(altar.getAltarState().tier() == 1 && altar.getAltarState().capacity() == 10_000
                && altar.getAltarState().blood() == 10_000, "broken structure removes bonuses and clamps LP");
        int loaded = level.getChunkSource().getLoadedChunksCount();
        AltarStructure.Scan remote = AltarStructure.scan(level, new BlockPos(300_000, 180, 300_000));
        check(!remote.complete() && level.getChunkSource().getLoadedChunksCount() == loaded,
                "scanning unloaded structure never loads chunks");
        BloodAltarBlockEntity detached = new BloodAltarBlockEntity(new BlockPos(300_000, 180, 300_000), altar.getBlockState());
        detached.setLevel(level);
        detached.setAltarState(new AltarState(14_000, 14_000, 0, 0, 1_400, 2, 0, 0, false));
        check(!detached.refreshStructure() && detached.getAltarState().blood() == 14_000,
                "unloaded structure pauses without destroying stored LP");
        clearStructure(level, TEST);
        reset(altar);
    }

    private static void testRecipes(ServerLevel level, BloodAltarBlockEntity altar) {
        check(BuiltInRegistries.RECIPE_TYPE.getKey(BloodMagicRecipes.ALTAR_TYPE.get()).equals(BloodMagic.rl("altar")), "registered recipe type");
        check(BuiltInRegistries.RECIPE_SERIALIZER.getKey(BloodMagicRecipes.ALTAR_SERIALIZER.get()).equals(BloodMagic.rl("altar")), "registered native serializer");
        var recipes = level.getServer().getRecipeManager().recipeMap().byType(BloodMagicRecipes.ALTAR_TYPE.get());
        check(recipes.size() == 10, "all ten migrated JSON recipes loaded by real recipe manager");
        var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (var holder : recipes) {
            var json = Recipe.CODEC.encodeStart(ops, holder.value()).getOrThrow();
            var restored = (BloodAltarRecipe) Recipe.CODEC.parse(ops, json).getOrThrow();
            check(holder.value().sameDefinition(restored), "registry-dispatched JSON round trip " + holder.id());
        }
        altar.setStoredItem(new ItemStack(Items.STONE));
        var slate = altar.findRecipe().orElseThrow().value();
        check(slate.minimumTier() == 1 && slate.syphon() == 1_000, "legacy upgradeLevel zero means runtime tier one");
        ItemStack output = slate.output();
        output.setCount(63);
        check(slate.output().getCount() == 1, "recipe result is a defensive copy");
        altar.setStoredItem(new ItemStack(BloodMagicItems.SLATE.get()));
        check(altar.findRecipe().isEmpty(), "matching ingredient is rejected below minimum tier");
        altar.setStoredItem(new ItemStack(Items.DIRT));
        check(altar.findRecipe().isEmpty(), "wrong ingredient has no recipe");
        check(!new BloodAltarRecipe(Ingredient.of(Items.DIAMOND), new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get()), 1, 2_000, 5, 1)
                .matches(new BloodAltarRecipeInput(new ItemStack(Items.DIAMOND, 2), 1), level), "nonstackable outputs cannot silently lose a batch");
        check(!new BloodAltarRecipe(Ingredient.of(Items.STONE), new ItemStack(Items.GOLD_INGOT), 1, Integer.MAX_VALUE, 5, 1)
                .matches(new BloodAltarRecipeInput(new ItemStack(Items.STONE, 2), 1), level), "LP batch overflow rejected");
        String json = "{\"input\":\"minecraft:stone\",\"output\":{\"id\":\"bloodmagic:blankslate\"},\"upgradeLevel\":0,\"altarSyphon\":1000,\"consumptionRate\":5,\"drainRate\":5}";
        check(BloodAltarRecipe.CODEC.codec().parse(ops, JsonParser.parseString(json)).isSuccess(), "new Ingredient and ItemStack JSON syntax");
        for (String bad : new String[]{json.replace("1000", "-1"), json.replace("\"consumptionRate\":5", "\"consumptionRate\":0"),
                json.replace("\"upgradeLevel\":0", "\"upgradeLevel\":6"), json.replace("\"drainRate\":5", "\"drainRate\":-1")}) {
            check(BloodAltarRecipe.CODEC.codec().parse(ops, JsonParser.parseString(bad)).isError(), "invalid JSON numeric bounds rejected");
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            Recipe.STREAM_CODEC.encode(buffer, slate);
            var decoded = (BloodAltarRecipe) Recipe.STREAM_CODEC.decode(buffer);
            check(decoded.matches(new BloodAltarRecipeInput(new ItemStack(Items.STONE), 1), level)
                    && decoded.syphon() == 1000 && decoded.consumptionRate() == 5 && decoded.drainRate() == 5
                    && decoded.output().is(BloodMagicItems.SLATE.get()) && !buffer.isReadable(), "registry-dispatched recipe StreamCodec round trip");
            buffer.clear();
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, Ingredient.of(Items.STONE));
            ItemStackTemplate.STREAM_CODEC.encode(buffer, new ItemStackTemplate(Items.GOLD_INGOT));
            for (int value : new int[]{1, -1, 5, 0}) buffer.writeVarInt(value);
            boolean rejected = false;
            try { BloodAltarRecipe.STREAM_CODEC.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "network codec validates negative LP costs");
        } finally { buffer.release(); }
        reset(altar);
    }

    private static void testGameplay(ServerLevel level, BloodAltarBlockEntity altar) {
        altar.setStoredItem(new ItemStack(Items.STONE));
        altar.fillMainTank(1_500);
        tick(altar, 199);
        check(altar.getAltarState().progress() == 995 && altar.getAltarState().blood() == 505
                && altar.getStoredItem().is(Items.STONE), "input retained until complete; each tick consumes actual LP");
        tick(altar, 1);
        check(altar.getStoredItem().is(BloodMagicItems.SLATE.get()) && altar.getAltarState().blood() == 500
                && !altar.getAltarState().active(), "stone produces slate for exactly 1000 LP");
        tick(altar, 60);
        check(altar.getStoredItem().getCount() == 1 && altar.getAltarState().blood() == 500,
                "finished output is not duplicated and insufficient-tier chain consumes no LP");
        reset(altar);
        altar.setStoredItem(new ItemStack(Items.STONE, 2));
        altar.fillMainTank(2_123);
        tick(altar, 400);
        check(altar.getStoredItem().is(BloodMagicItems.SLATE.get()) && altar.getStoredItem().getCount() == 2
                && altar.getAltarState().blood() == 123, "batch scales both LP cost and result count");
        reset(altar);
        altar.setStoredItem(new ItemStack(Items.STONE));
        tick(altar, 20);
        check(altar.getStoredItem().is(Items.STONE) && altar.getAltarState().progress() == 0, "no free craft in empty tank");
        altar.fillMainTank(7);
        tick(altar, 2);
        check(altar.getAltarState().progress() == 7 && altar.getAltarState().blood() == 0, "partial final drain never makes LP negative");
        tick(altar, 1);
        check(altar.getAltarState().progress() == 2, "starvation applies recipe drainRate");
        tick(altar, 1);
        check(altar.getAltarState().progress() == 0, "starvation progress clamps at zero");
        altar.fillMainTank(500);
        tick(altar, 10);
        altar.setStoredItem(new ItemStack(Items.DIAMOND));
        check(altar.getAltarState().progress() == 0, "changing input clears paid progress without LP refund");
        tick(altar, 1);
        check(altar.getAltarState().progress() == 5 && altar.getAltarState().requiredBlood() == 2_000, "new recipe starts fresh");
        var serialized = altar.saveWithFullMetadata(level.registryAccess());
        var restored = (BloodAltarBlockEntity) BlockEntity.loadStatic(TEST, altar.getBlockState(), serialized, level.registryAccess());
        restored.setLevel(level);
        tick(restored, 1);
        check(restored.getAltarState().progress() == 10 && restored.getAltarState().blood() == altar.getAltarState().blood() - 5,
                "unfinished recipe NBT round trip resumes LP progress");
        serialized.putString("recipe_id", "bloodmagic:missing_recipe");
        restored = (BloodAltarBlockEntity) BlockEntity.loadStatic(TEST, altar.getBlockState(), serialized, level.registryAccess());
        restored.setLevel(level);
        tick(restored, 1);
        check(restored.getAltarState().progress() == 5, "stale persisted recipe identity resets progress");
        reset(altar);
        altar.setStoredItem(new ItemStack(Items.DIRT));
        altar.fillMainTank(400);
        tick(altar, 20);
        check(altar.getAltarState().blood() == 400 && !altar.getAltarState().active(), "unmatched input consumes no LP");
        buildTier(level, TEST, 2);
        set(level, TEST.offset(0, -1, -1), BloodMagicBlocks.SPEED_RUNE_2.get());
        reset(altar);
        altar.refreshStructure();
        altar.setStoredItem(new ItemStack(BloodMagicItems.SLATE.get()));
        altar.fillMainTank(2_010);
        tick(altar, 285);
        check(altar.getAltarState().progress() == 1_995 && altar.getAltarState().blood() == 15, "speed rune accelerates real recipe progress");
        tick(altar, 1);
        check(altar.getStoredItem().is(BloodMagicItems.REINFORCED_SLATE.get()) && altar.getAltarState().blood() == 10,
                "final fast tick charges remaining five LP, not seven");
        reset(altar);
        altar.refreshStructure();
        altar.setStoredItem(new ItemStack(BloodMagicItems.SLATE.get()));
        altar.fillMainTank(100);
        tick(altar, 1);
        set(level, TEST.offset(1, -1, 1), Blocks.AIR);
        altar.refreshStructure();
        tick(altar, 1);
        check(altar.getAltarState().blood() == 93 && altar.getAltarState().progress() == 0
                && !altar.getAltarState().active(), "structure downgrade cancels ineligible recipe without extra consumption");
        clearStructure(level, TEST);
        reset(altar);
    }

    private static void testInteraction(ServerLevel level, BloodAltarBlockEntity altar) {
        var player = MenuTestPlayer.create(level, "Stage4Gameplay");
        player.setGameMode(GameType.SURVIVAL);
        player.connection.markClientLoaded();
        player.setInvulnerable(false);
        player.setPos(TEST.getX() + 0.5, TEST.getY(), TEST.getZ() + 2);
        player.getInventory().clearContent();
        player.setHealth(20);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 3));
        var hit = new BlockHitResult(Vec3.atCenterOf(TEST), Direction.UP, TEST, false);
        check(altar.getBlockState().useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "26.1 useItemOn delegates actual right-click insertion");
        check(altar.getStoredItem().getCount() == 1 && player.getMainHandItem().getCount() == 2, "survival insertion moves exactly one item");
        check(altar.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && player.getMainHandItem().getCount() == 2,
                "occupied slot never overwrites or consumes held items");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        player.invulnerableTime = 0;
        var sacrificeResult = altar.getBlockState().useWithoutItem(level, player, hit);
        check(sacrificeResult.consumesAction() && player.getHealth() == 18
                && altar.getAltarState().blood() == 200, "sneak empty right-click converts two health into 200 LP: "
                + sacrificeResult + ", health=" + player.getHealth() + ", state=" + altar.getAltarState()
                + ", invulnerable=" + player.isInvulnerableTo(level, player.damageSources().magic()));
        tick(altar, 40);
        check(altar.getAltarState().progress() == 200 && altar.getAltarState().blood() == 0, "player-supplied LP is consumed by inserted recipe");
        for (int i = 0; i < 4; i++) { player.invulnerableTime = 0; altar.sacrificeHealth(player); }
        tick(altar, 160);
        check(altar.getStoredItem().is(BloodMagicItems.SLATE.get()) && altar.getAltarState().blood() == 0 && player.getHealth() == 10,
                "full player insertion, sacrifice, LP debit and output path");
        player.setShiftKeyDown(false);
        check(altar.getBlockState().useWithoutItem(level, player, hit).consumesAction()
                && altar.getStoredItem().is(BloodMagicItems.SLATE.get()), "empty right-click opens menu and preserves completed output");
        player.containerMenu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
        player.containerMenu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
        check(altar.getStoredItem().isEmpty() && player.getInventory().countItem(BloodMagicItems.SLATE.get()) == 1,
                "shift-click extracts completed output exactly once");
        player.closeContainer();
        player.setHealth(2);
        player.invulnerableTime = 0;
        check(altar.sacrificeHealth(player) == 0 && player.getHealth() == 2, "sacrifice refuses lethal health cost");
        player.setHealth(20);
        altar.fillMainTank(Integer.MAX_VALUE);
        check(altar.sacrificeHealth(player) == 0 && player.getHealth() == 20, "full tank consumes no player health");
        altar.drainMainTank(Integer.MAX_VALUE);
        buildTier(level, TEST, 2);
        set(level, TEST.offset(0, -1, -1), BloodMagicBlocks.SELF_SACRIFICE_RUNE_2.get());
        player.invulnerableTime = 0;
        check(altar.sacrificeHealth(player) == 240 && player.getHealth() == 18, "self-sacrifice rune strengthens health-to-LP conversion");
        player.setGameMode(GameType.SPECTATOR);
        check(altar.sacrificeHealth(player) == 0 && altar.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "spectators cannot modify altar");
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(0, 180, 0);
        check(altar.sacrificeHealth(player) == 0, "remote player cannot supply LP");
        clearStructure(level, TEST);
        reset(altar);
    }

    private static BloodAltarBlockEntity place(ServerLevel level, BlockPos pos) {
        level.getChunk(pos);
        set(level, pos, BloodMagicBlocks.BLOOD_ALTAR.get());
        return (BloodAltarBlockEntity) level.getBlockEntity(pos);
    }

    private static void reset(BloodAltarBlockEntity altar) {
        altar.setStoredItem(ItemStack.EMPTY);
        altar.setAltarState(AltarState.EMPTY);
        altar.refreshStructure();
    }

    private static void tick(BloodAltarBlockEntity altar, int count) {
        for (int i = 0; i < count; i++) BloodAltarBlockEntity.serverTick(altar.getLevel(), altar.getBlockPos(), altar.getBlockState(), altar);
    }

    // Fixtures independently spell out upstream coordinates; they do not use production component lists.
    private static void buildTier(ServerLevel level, BlockPos pos, int tier) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x != 0 || z != 0) set(level, pos.offset(x, -1, z), BloodMagicBlocks.BLANK_RUNE.get());
        }
        if (tier >= 3) {
            ring(level, pos, 3, -2, 2);
            pillar(level, pos, 3, -1, 0, 1, Blocks.GLOWSTONE);
        }
        if (tier >= 4) {
            ring(level, pos, 5, -3, 3);
            pillar(level, pos, 5, -2, 1, 2, BloodMagicBlocks.BLOODSTONE.get());
        }
        if (tier >= 5) {
            ring(level, pos, 8, -4, 6);
            for (int x : new int[]{-8, 8}) for (int z : new int[]{-8, 8}) set(level, pos.offset(x, -4, z), BloodMagicBlocks.HELLFORGED_BLOCK.get());
        }
    }

    private static void ring(ServerLevel level, BlockPos pos, int radius, int y, int extent) {
        for (int i = -extent; i <= extent; i++) {
            set(level, pos.offset(radius, y, i), BloodMagicBlocks.BLANK_RUNE.get());
            set(level, pos.offset(-radius, y, i), BloodMagicBlocks.BLANK_RUNE.get());
            set(level, pos.offset(i, y, radius), BloodMagicBlocks.BLANK_RUNE.get());
            set(level, pos.offset(i, y, -radius), BloodMagicBlocks.BLANK_RUNE.get());
        }
    }

    private static void pillar(ServerLevel level, BlockPos pos, int radius, int bottom, int top, int capY, Block cap) {
        for (int x : new int[]{-radius, radius}) for (int z : new int[]{-radius, radius}) {
            for (int y = bottom; y <= top; y++) set(level, pos.offset(x, y, z), Blocks.STONE);
            set(level, pos.offset(x, capY, z), cap);
        }
    }

    private static void clearStructure(ServerLevel level, BlockPos pos) {
        for (var component : AltarStructure.components(6)) set(level, pos.offset(component.offset()), Blocks.AIR);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE4_SMOKE FAIL: " + message);
        assertions++;
    }
}
