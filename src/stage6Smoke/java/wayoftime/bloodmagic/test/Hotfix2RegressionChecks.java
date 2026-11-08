package wayoftime.bloodmagic.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.DungeonDecorativeBlocks;
import wayoftime.bloodmagic.common.block.entity.MasterRitualBlockEntity;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemRitualDiviner;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.ritual.BasicRitual;

/** Hotfix.2 regressions for diviner dispatch, connected columns and one-block gate collisions. */
final class Hotfix2RegressionChecks {
    private static final BlockPos DIVINER = new BlockPos(686, 180, 686);
    private static final BlockPos PILLAR = new BlockPos(706, 180, 706);
    private static final BlockPos GATE = new BlockPos(726, 180, 726);
    private static int assertions;

    private Hotfix2RegressionChecks() {}

    static int run(ServerLevel level) {
        assertions = 0;
        level.getChunk(DIVINER);
        level.getChunk(PILLAR);
        level.getChunk(GATE);
        var player = MenuTestPlayer.create(level, "Hotfix2");
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        player.getInventory().clearContent();
        player.setPos(DIVINER.getX() + .5, DIVINER.getY(), DIVINER.getZ() + 2);
        clearArea(level, DIVINER, 6, 2, 6);
        level.setBlockAndUpdate(DIVINER, BloodMagicBlocks.MASTER_RITUAL_STONE.get().defaultBlockState());

        var diviner = new ItemStack(BloodMagicItems.RITUAL_DIVINER.get());
        diviner.set(BloodMagicDataComponents.RITUAL_SELECTION.get(), BasicRitual.WATER.ordinal());
        player.setItemInHand(InteractionHand.MAIN_HAND, diviner);
        player.getInventory().setItem(1, new ItemStack(BloodMagicBlocks.RITUAL_STONE.get().asItem(), 8));
        PlayerInteractionChecks.useOn(player, DIVINER);
        var master = (MasterRitualBlockEntity) level.getBlockEntity(DIVINER);
        check(master.ritual() == BasicRitual.WATER, "diviner selects the ritual through a real server use packet");
        check(player.getInventory().getItem(1).getCount() == 7, "one diviner right-click consumes exactly one blank ritual stone");
        check(BasicRitual.WATER.components().stream().filter(part -> level.getBlockState(DIVINER.offset(part.offset())).is(part.rune().block())).count() == 1,
                "one diviner right-click places exactly one missing rune");

        var normal = (ItemRitualDiviner) diviner.getItem();
        check(normal.canPerform(BasicRitual.WATER) && !normal.canPerform(BasicRitual.CRUSHING),
                "normal diviner supports elemental rituals but not dusk runes");
        var dusk = new ItemStack(BloodMagicItems.DUSK_RITUAL_DIVINER.get());
        check(((ItemRitualDiviner) dusk.getItem()).canPerform(BasicRitual.CRUSHING),
                "dusk diviner supports dusk runes");

        for (String id : new String[]{"dungeon_pillar_center", "dungeon_pillar_special", "dungeon_pillar_center_corrosive", "dungeon_pillar_special_vengeful"}) {
            RotatedPillarBlock pillar = (RotatedPillarBlock) DungeonDecorativeBlocks.block(id).get();
            clear(level, PILLAR);
            var first = pillar.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            level.setBlockAndUpdate(PILLAR, first);
            var hit = new BlockHitResult(Vec3.atCenterOf(PILLAR), Direction.EAST, PILLAR, false);
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(pillar.asItem()), hit);
            BlockState second = pillar.getStateForPlacement(context);
            check(second != null && second.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y,
                    id + " inherits the connected column axis when placed against its side: clicked="
                            + level.getBlockState(PILLAR) + ", target=" + context.getClickedPos()
                            + ", second=" + second + ", sneak=" + player.isShiftKeyDown());
            level.setBlockAndUpdate(PILLAR.east(), second);
            check(level.getBlockState(PILLAR).getValue(RotatedPillarBlock.AXIS)
                            == level.getBlockState(PILLAR.east()).getValue(RotatedPillarBlock.AXIS),
                    id + " connects two matching column blocks");
        }

        int gateCount = 0;
        for (var entry : DungeonDecorativeBlocks.BLOCK_BY_ID.entrySet()) {
            if (!entry.getKey().contains("_gate")) continue;
            FenceGateBlock gate = (FenceGateBlock) entry.getValue().get();
            BlockPos pos = GATE.offset(gateCount * 2, 0, 0);
            level.getChunk(pos);
            clear(level, pos);
            BlockState closed = gate.defaultBlockState();
            level.setBlockAndUpdate(pos, closed);
            double collisionTop = closed.getCollisionShape(level, pos, CollisionContext.empty()).bounds().maxY;
            check(Math.abs(collisionTop - 1.0D) < 1.0E-5D, entry.getKey() + " closed collision matches its one-block visual height");
            check(closed.setValue(FenceGateBlock.OPEN, true).getCollisionShape(level, pos, CollisionContext.empty()).isEmpty(),
                    entry.getKey() + " open gate has no collision");
            gateCount++;
        }
        check(gateCount == 10, "all ten dungeon gate variants are checked");

        clearArea(level, DIVINER, 6, 2, 6);
        clear(level, PILLAR);
        clear(level, PILLAR.east());
        player.getInventory().clearContent();
        return assertions;
    }

    private static void clearArea(ServerLevel level, BlockPos center, int horizontal, int vertical, int below) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-horizontal, -below, -horizontal),
                center.offset(horizontal, vertical, horizontal))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE6_SMOKE FAIL: " + message);
        assertions++;
    }
}
