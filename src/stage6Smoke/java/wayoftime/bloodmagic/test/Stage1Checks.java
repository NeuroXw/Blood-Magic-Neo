package wayoftime.bloodmagic.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;
import wayoftime.bloodmagic.common.block.entity.MasterRitualBlockEntity;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemActivationCrystal;
import wayoftime.bloodmagic.common.registries.BloodMagicWill;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.core.data.SoulNetworks;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;
import wayoftime.bloodmagic.ritual.BasicRitual;
import wayoftime.bloodmagic.ritual.EnumReaderBoundaries;

/** Stage 1 integration checks for Demon Will ritual configuration and effects. */
final class Stage1Checks {
    private static final BlockPos MASTER = new BlockPos(900, 180, 900);
    private static final Binding OWNER = new Binding(
            UUID.fromString("87654321-4321-8765-cba9-cba987654321"), "Stage1Tester");
    private static final Path MARKER = Path.of("stage1-ritual-fixture-committed.txt");
    private static int assertions;
    private static boolean restart;

    private Stage1Checks() {
    }

    static void begin(ServerLevel level) throws IOException {
        restart = Files.exists(MARKER) && Files.readString(MARKER).contains("v1");
        Files.deleteIfExists(MARKER);
        check(BloodMagicItems.RITUAL_TINKERER.get() != null, "ritual tinkerer registered");
        check(BloodMagicItems.ACTIVATION_CRYSTAL.get().crystalLevel() == 0, "weak crystal tier");
        check(BloodMagicItems.AWAKENED_ACTIVATION_CRYSTAL.get().crystalLevel() == 1, "awakened crystal tier");
        check(BloodMagicItems.CREATIVE_ACTIVATION_CRYSTAL.get().crystalLevel() == Integer.MAX_VALUE,
                "creative crystal tier");
        check(BloodMagicItems.ACTIVATION_CRYSTAL.get() instanceof ItemActivationCrystal,
                "activation crystal implementation registered");
        check(BloodMagicWill.PLANT_LEECH.get() != null && BloodMagicWill.SACRIFICIAL_LAMB.get() != null
                && BloodMagicWill.SOUL_FRAY.get() != null && BloodMagicWill.SUSPENDED.get() != null
                && BloodMagicWill.GROUNDED.get() != null && BloodMagicWill.GRAVITY.get() != null
                && BloodMagicWill.HEAVY_HEART.get() != null && BloodMagicWill.FIRE_FUSE.get() != null
                && BloodMagicWill.SOFT_FALL.get() != null, "Demon Will ritual effects registered");
        int willRituals = 0;
        for (BasicRitual ritual : BasicRitual.values()) {
            if (!ritual.usesDemonWillConfig()) continue;
            willRituals++;
            check(!ritual.rangeSpecs().isEmpty(), "Demon Will ritual exposes configurable ranges: " + ritual);
        }
        check(willRituals == 12, "all twelve upstream Demon Will rituals are wired");
        check(level.getServer().getRecipeManager().getRecipes().stream().anyMatch(holder ->
                        holder.id().identifier().equals(wayoftime.bloodmagic.BloodMagic.rl("ritual_reader"))),
                "ritual tinkerer recipe loads");
        TagKey<net.minecraft.world.level.block.Block> harvestable = TagKey.create(Registries.BLOCK,
                wayoftime.bloodmagic.BloodMagic.rl("geode_harvestable"));
        TagKey<net.minecraft.world.level.block.Block> acceleratable = TagKey.create(Registries.BLOCK,
                wayoftime.bloodmagic.BloodMagic.rl("geode_acceleratable"));
        check(Blocks.AMETHYST_CLUSTER.defaultBlockState().is(harvestable), "geode harvest tag");
        check(Blocks.BUDDING_AMETHYST.defaultBlockState().is(acceleratable), "geode acceleration tag");

    }

    static int run(ServerLevel level) {
        if (restart) {
            check(level.getBlockEntity(MASTER) instanceof MasterRitualBlockEntity,
                    "stage 1 fixture survives restart");
            MasterRitualBlockEntity restored = (MasterRitualBlockEntity) level.getBlockEntity(MASTER);
            check(restored.ritual() == BasicRitual.GREEN_GROVE, "ritual selection survives restart");
            check(restored.activeWillConfig().equals(List.of(EnumDemonWillType.DEFAULT)),
                    "will configuration survives restart");
            check(restored.range("growing") != null && restored.range("growing").minimum().equals(new BlockPos(0, 2, 0))
                    && restored.range("growing").maximum().equals(new BlockPos(1, 3, 1)),
                    "custom growing range survives restart");
            check(restored.active(), "active ritual state survives restart");
        } else {
            runFresh(level);
        }
        int result = assertions;
        assertions = 0;
        return result;
    }


    private static void runFresh(ServerLevel level) {
        clearArea(level);
        ServerPlayer player = MenuTestPlayer.create(level, OWNER.ownerName());
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(MASTER.getX() + 1.5, MASTER.getY(), MASTER.getZ() + 1.5);
        level.setBlockAndUpdate(MASTER, BloodMagicBlocks.MASTER_RITUAL_STONE.get().defaultBlockState());
        MasterRitualBlockEntity master = (MasterRitualBlockEntity) level.getBlockEntity(MASTER);
        master.select(BasicRitual.GREEN_GROVE);
        master.setActiveWillConfig(player, List.of(EnumDemonWillType.DEFAULT));
        check(master.setBlockRangeByBounds(player, "growing", new BlockPos(-10, 2, -10), new BlockPos(10, 2, 10))
                == EnumReaderBoundaries.VOLUME_TOO_LARGE, "oversized growing range rejected");
        check(master.setBlockRangeByBounds(player, "growing", new BlockPos(50, 2, 50), new BlockPos(51, 2, 51))
                == EnumReaderBoundaries.NOT_WITHIN_BOUNDARIES, "out-of-range growing area rejected");
        check(master.setBlockRangeByBounds(player, "growing", new BlockPos(0, 2, 0), new BlockPos(0, 2, 0))
                == EnumReaderBoundaries.SUCCESS, "growing range resized");
        check(master.setBlockRangeByBounds(player, "hydrate", new BlockPos(0, 1, 0), new BlockPos(0, 2, 0))
                == EnumReaderBoundaries.SUCCESS, "hydration range resized");

        for (var part : BasicRitual.GREEN_GROVE.components())
            level.setBlockAndUpdate(MASTER.offset(part.offset()), part.rune().block().defaultBlockState());
        level.setBlockAndUpdate(MASTER.above(), Blocks.SAND.defaultBlockState());
        level.setBlockAndUpdate(MASTER.offset(1, 2, 0), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(MASTER.above(2), Blocks.SUGAR_CANE.defaultBlockState());


        check(BasicRitual.GREEN_GROVE.scan(level, MASTER) == BasicRitual.Scan.COMPLETE,
                "Green Grove rune scan is complete: " + BasicRitual.GREEN_GROVE.scan(level, MASTER));
        check(master.range("growing").positions(MASTER).contains(MASTER.above(2)),
                "growing range contains the crop: " + master.range("growing").minimum() + ".." + master.range("growing").maximum());
        clearAura(level);
        WorldDemonWillHandler.fillWill(level, MASTER, EnumDemonWillType.DEFAULT, 200, true);
        SoulNetworks networks = SoulNetworks.get(level);
        networks.consume(OWNER.ownerId(), networks.get(OWNER.ownerId()).currentEssence());
        networks.fill(OWNER.ownerId(), 100_000, 150_000);
        check(master.activate(player, OWNER, 0), "Demon Will ritual activates with a weak crystal");
        check(master.active(), "Demon Will ritual remains active after activation");


        check(!level.hasNeighborSignal(MASTER), "ritual fixture has no redstone signal");
        check(master.rangeWithinLimitsForEffects(), "all configured ritual ranges are valid");
        for (int tick = 0; tick < 10_000; tick++)
            MasterRitualBlockEntity.serverTick(level, MASTER, master.getBlockState(), master);
        BlockState sugarCane = level.getBlockState(MASTER.above(2));
        check(sugarCane.is(Blocks.SUGAR_CANE) && (sugarCane.getValue(net.minecraft.world.level.block.SugarCaneBlock.AGE) > 0 || level.getBlockState(MASTER.above(3)).is(Blocks.SUGAR_CANE)),
                "Green Grove growth effect executes: state=" + sugarCane + ", light=" + level.getRawBrightness(MASTER.above(2), 0) + ", raw=" + WorldDemonWillHandler.getCurrentWill(level, MASTER, EnumDemonWillType.DEFAULT) + ", lp=" + networks.get(OWNER.ownerId()).currentEssence() + ", above=" + level.getBlockState(MASTER.above(3)) + ", active=" + master.active() + ", ritual=" + master.ritual());
        double remainingRaw = WorldDemonWillHandler.getCurrentWill(level, MASTER, EnumDemonWillType.DEFAULT);
        check(remainingRaw < 200 && remainingRaw >= 190,
                "Green Grove drains configured Raw Will: " + remainingRaw);
        int remainingLp = networks.get(OWNER.ownerId()).currentEssence();
        check(remainingLp < 95_000 && remainingLp >= 90_000,
                "activation and ritual upkeep are charged from the bound network: " + remainingLp);
        CompoundTag tag = master.saveWithFullMetadata(level.registryAccess());
        check(tag.getBooleanOr("EnumWilldefault", false), "will config serialized");
        BlockEntity roundTrip = BlockEntity.loadStatic(MASTER,
                BloodMagicBlocks.MASTER_RITUAL_STONE.get().defaultBlockState(), tag, level.registryAccess());
        check(roundTrip instanceof MasterRitualBlockEntity copy && copy.ritual() == BasicRitual.GREEN_GROVE
                        && copy.activeWillConfig().equals(List.of(EnumDemonWillType.DEFAULT))
                        && copy.range("growing").minimum().equals(new BlockPos(0, 2, 0)),
                "master ritual state round-trips through NBT");
    }

    static void commit() throws IOException {
        Files.writeString(MARKER, "Stage 1 ritual configuration fixture v1\n");
    }

    private static void clearArea(ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(MASTER.offset(-8, -2, -8), MASTER.offset(8, 5, 8)))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static void clearAura(ServerLevel level) {
        for (EnumDemonWillType type : EnumDemonWillType.values()) {
            double amount = WorldDemonWillHandler.getCurrentWill(level, MASTER, type);
            if (amount > 0) WorldDemonWillHandler.drainWill(level, MASTER, type, amount, true);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE6_SMOKE FAIL: " + message);
        assertions++;
    }
}







