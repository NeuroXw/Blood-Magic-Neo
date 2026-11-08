package wayoftime.bloodmagic.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.block.BlockDemonCrystal;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.DungeonDecorativeBlocks;
import wayoftime.bloodmagic.common.block.entity.DemonCrucibleBlockEntity;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemDemonWill;
import wayoftime.bloodmagic.common.registries.BloodMagicCreativeTabs;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;

/** Real-packet regressions for the reported Demon Will, catalyst, crystal, scribe and dungeon connections. */
final class Hotfix4RegressionChecks {
    private static final BlockPos WILL = new BlockPos(786, 180, 786);
    private static final BlockPos CATALYST = new BlockPos(806, 181, 786);
    private static final BlockPos CRUCIBLE = new BlockPos(826, 180, 786);
    private static final BlockPos SCRIBE = new BlockPos(846, 180, 786);
    private static final BlockPos WALL = new BlockPos(866, 180, 786);
    private static final BlockPos PILLAR = new BlockPos(886, 180, 786);
    private static int assertions;

    private Hotfix4RegressionChecks() {}

    static int run(ServerLevel level) {
        assertions = 0;
        for (BlockPos pos : new BlockPos[]{WILL, CATALYST, CRUCIBLE, SCRIBE, WALL, PILLAR}) level.getChunk(pos);

        var player = MenuTestPlayer.create(level, "Hotfix4");
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(20);
        player.setShiftKeyDown(false);
        player.getInventory().clearContent();

        CreativeModeTab tab = BloodMagicCreativeTabs.BLOODMAGIC.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
        var displayedSoul = tab.getDisplayItems().stream()
                .filter(stack -> stack.is(BloodMagicItems.RAW_WILL.get()))
                .findFirst();
        check(displayedSoul.isPresent() && ItemDemonWill.amount(displayedSoul.get()) == 5,
                "creative Demon Will is populated instead of arriving empty");

        clearArea(level, WILL, 3, 2, 2);
        level.setBlockAndUpdate(WILL, Blocks.STONE.defaultBlockState());
        player.setPos(WILL.getX() + .5, WILL.getY(), WILL.getZ() + 2.5);
        var gem = new ItemStack(BloodMagicItems.PETTY_GEM.get());
        player.getInventory().setItem(9, gem);
        var soul = new ItemStack(BloodMagicItems.RAW_WILL.get());
        ItemDemonWill.setAmount(soul, 7.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, soul);
        PlayerInteractionChecks.useOn(player, WILL);
        check(ItemDemonWill.amount(player.getInventory().getItem(9)) == 7.5D && player.getMainHandItem().isEmpty(),
                "held Demon Will transfers through a real block right-click");

        clearArea(level, CATALYST, 3, 3, 2);
        level.setBlockAndUpdate(CATALYST.below(), Blocks.STONE.defaultBlockState());
        var crystalState = BloodMagicBlocks.VENGEFUL_DEMON_CRYSTAL.get().defaultBlockState()
                .setValue(BlockDemonCrystal.ATTACHED, Direction.UP)
                .setValue(BlockDemonCrystal.AGE, 2);
        level.setBlockAndUpdate(CATALYST, crystalState);
        player.getInventory().clearContent();
        var fundedGem = new ItemStack(BloodMagicItems.GREATER_GEM.get());
        ItemDemonWill.setWill(fundedGem, EnumDemonWillType.VENGEFUL, 600);
        player.getInventory().setItem(9, fundedGem);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BloodMagicItems.VENGEFUL_CATALYST.get()));
        player.setPos(CATALYST.getX() + .5, CATALYST.getY(), CATALYST.getZ() + 2.5);
        PlayerInteractionChecks.useOn(player, CATALYST);
        var crystal = (wayoftime.bloodmagic.common.block.entity.DemonCrystalBlockEntity) level.getBlockEntity(CATALYST);
        check(crystal.getInjectedWill() == 200 && crystal.speedModifier == 10 && crystal.getCrystalCount() == 3
                        && player.getMainHandItem().isEmpty(),
                "catalyst applies before automatic crystal harvesting");

        clearArea(level, CRUCIBLE, 3, 2, 2);
        clearAura(level, CRUCIBLE);
        level.setBlockAndUpdate(CRUCIBLE, BloodMagicBlocks.DEMON_CRUCIBLE.get().defaultBlockState());
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BloodMagicItems.CORROSIVE_CRYSTAL.get(), 2));
        player.setPos(CRUCIBLE.getX() + .5, CRUCIBLE.getY(), CRUCIBLE.getZ() + 2.5);
        PlayerInteractionChecks.useOn(player, CRUCIBLE);
        var crucible = (DemonCrucibleBlockEntity) level.getBlockEntity(CRUCIBLE);
        check(crucible.getItem(0).is(BloodMagicItems.CORROSIVE_CRYSTAL.get()) && player.getMainHandItem().getCount() == 1,
                "corrosive will crystal inserts into the Demon Crucible through a real right-click");
        for (int i = 0; i < 2; i++) DemonCrucibleBlockEntity.serverTick(level, CRUCIBLE, crucible.getBlockState(), crucible);
        check(crucible.getItem(0).isEmpty()
                        && WorldDemonWillHandler.getCurrentWill(level, CRUCIBLE, EnumDemonWillType.CORROSIVE) == 50,
                "corrosive will crystal feeds its typed chunk aura");

        clearArea(level, SCRIBE, 3, 2, 2);
        player.getInventory().clearContent();
        player.setGameMode(GameType.SURVIVAL);
        level.setBlockAndUpdate(SCRIBE, BloodMagicBlocks.RITUAL_STONE.get().defaultBlockState());
        var earthScribe = new ItemStack(BloodMagicItems.EARTH_SCRIBE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, earthScribe);
        player.setPos(SCRIBE.getX() + .5, SCRIBE.getY(), SCRIBE.getZ() + 2.5);
        PlayerInteractionChecks.useOn(player, SCRIBE);
        check(level.getBlockState(SCRIBE).is(BloodMagicBlocks.EARTH_RITUAL_STONE.get()) && earthScribe.getDamageValue() == 1,
                "earth inscription tool retunes a blank rune through a real right-click");
        PlayerInteractionChecks.useOn(player, SCRIBE);
        check(earthScribe.getDamageValue() == 1, "identical earth rune consumes no extra durability");
        level.setBlockAndUpdate(SCRIBE, BloodMagicBlocks.WATER_RITUAL_STONE.get().defaultBlockState());
        PlayerInteractionChecks.useOn(player, SCRIBE);
        check(level.getBlockState(SCRIBE).is(BloodMagicBlocks.EARTH_RITUAL_STONE.get()) && earthScribe.getDamageValue() == 2,
                "earth inscription tool converts another elemental rune");
        player.setGameMode(GameType.CREATIVE);
        level.setBlockAndUpdate(SCRIBE, BloodMagicBlocks.RITUAL_STONE.get().defaultBlockState());
        var creativeScribe = new ItemStack(BloodMagicItems.EARTH_SCRIBE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, creativeScribe);
        PlayerInteractionChecks.useOn(player, SCRIBE);
        check(level.getBlockState(SCRIBE).is(BloodMagicBlocks.EARTH_RITUAL_STONE.get()) && creativeScribe.getDamageValue() == 0,
                "creative earth inscription tool places without durability loss");
        player.setGameMode(GameType.SURVIVAL);

        clearArea(level, WALL, 4, 2, 2);
        int wallCount = 0;
        for (var entry : DungeonDecorativeBlocks.BLOCK_BY_ID.entrySet()) {
            if (!entry.getKey().contains("_wall")) continue;
            check(entry.getValue().get().defaultBlockState().is(BlockTags.WALLS), entry.getKey() + " belongs to minecraft:walls");
            wallCount++;
        }
        check(wallCount == 20, "all twenty dungeon wall variants are tagged");
        Block wallA = DungeonDecorativeBlocks.block("dungeon_brick1_wall").get();
        Block wallB = DungeonDecorativeBlocks.block("dungeon_stone_wall_corrosive").get();
        level.setBlockAndUpdate(WALL, wallA.defaultBlockState());
        level.setBlockAndUpdate(WALL.east(), wallB.defaultBlockState());
        BlockState westWall = Block.updateFromNeighbourShapes(level.getBlockState(WALL), level, WALL);
        BlockState eastWall = Block.updateFromNeighbourShapes(level.getBlockState(WALL.east()), level, WALL.east());
        level.setBlockAndUpdate(WALL, westWall);
        level.setBlockAndUpdate(WALL.east(), eastWall);
        check(westWall.getValue(WallBlock.EAST) != WallSide.NONE && eastWall.getValue(WallBlock.WEST) != WallSide.NONE,
                "different dungeon wall variants connect to each other");

        clearArea(level, PILLAR, 4, 2, 2);
        RotatedPillarBlock pillarA = (RotatedPillarBlock) DungeonDecorativeBlocks.block("dungeon_pillar_center").get();
        RotatedPillarBlock pillarB = (RotatedPillarBlock) DungeonDecorativeBlocks.block("dungeon_pillar_center_corrosive").get();
        level.setBlockAndUpdate(PILLAR, pillarA.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        var hit = new BlockHitResult(Vec3.atCenterOf(PILLAR), Direction.EAST, PILLAR, false);
        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(pillarB.asItem()), hit);
        BlockState secondPillar = pillarB.getStateForPlacement(context);
        check(secondPillar != null && secondPillar.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y,
                "different dungeon pillar variants inherit the connected axis");
        level.setBlockAndUpdate(PILLAR.east(), secondPillar);
        check(level.getBlockState(PILLAR.east()).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y,
                "different dungeon pillar variants align on the same axis");

        clearArea(level, WILL, 3, 2, 2);
        clearArea(level, CATALYST, 3, 3, 2);
        clearArea(level, CRUCIBLE, 3, 2, 2);
        clearArea(level, SCRIBE, 3, 2, 2);
        clearArea(level, WALL, 4, 2, 2);
        clearArea(level, PILLAR, 4, 2, 2);
        clearAura(level, CRUCIBLE);
        player.getInventory().clearContent();
        return assertions;
    }

    private static void clearAura(ServerLevel level, BlockPos pos) {
        for (EnumDemonWillType type : EnumDemonWillType.values()) {
            double amount = WorldDemonWillHandler.getCurrentWill(level, pos, type);
            if (amount > 0) WorldDemonWillHandler.drainWill(level, pos, type, amount, true);
        }
    }

    private static void clearArea(ServerLevel level, BlockPos center, int radius, int above, int below) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -below, -radius),
                center.offset(radius, above, radius))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE6_SMOKE FAIL: " + message);
        assertions++;
    }
}
