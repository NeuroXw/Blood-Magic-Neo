package wayoftime.bloodmagic.test;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.sigil.ItemSigilToggleableBase;
import wayoftime.bloodmagic.core.data.SoulNetworks;

/** Hotfix.3 regression for paid Frost Sigil cycles and solid-block protection. */
final class Hotfix3RegressionChecks {
    private static final BlockPos POS = new BlockPos(766, 180, 766);
    private static int assertions;

    private Hotfix3RegressionChecks() {}

    static int run(ServerLevel level) {
        assertions = 0;
        level.getChunk(POS);
        clearArea(level, POS);

        var noLP = MenuTestPlayer.create(level, "Hotfix3NoLP");
        noLP.setGameMode(GameType.SURVIVAL);
        noLP.setShiftKeyDown(false);
        noLP.setHealth(20);
        noLP.setPos(POS.getX() + .5, POS.getY(), POS.getZ() + .5);
        var networks = SoulNetworks.get(level);
        networks.consume(noLP.getUUID(), networks.get(noLP.getUUID()).currentEssence());
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            level.setBlockAndUpdate(POS.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        }
        var frost = new ItemStack(BloodMagicItems.ICE_SIGIL.get());
        noLP.setItemInHand(InteractionHand.MAIN_HAND, frost);
        noLP.setShiftKeyDown(true);
        check(frost.getItem().use(level, noLP, InteractionHand.MAIN_HAND).consumesAction()
                        && ItemSigilToggleableBase.activated(frost) && noLP.getHealth() == 19,
                "empty network activation immediately consumes one health");
        noLP.setShiftKeyDown(false);
        noLP.tickCount = 100;
        frost.getItem().inventoryTick(frost, level, noLP, EquipmentSlot.MAINHAND);
        check(noLP.getHealth() == 18 && networks.get(noLP.getUUID()).currentEssence() == 0,
                "next empty network upkeep consumes health instead of working for free");
        boolean solidReplaced = false;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (!level.getBlockState(POS.offset(x, -1, z)).is(Blocks.STONE)) solidReplaced = true;
        }
        check(!solidReplaced && level.getBlockState(POS).isAir(),
                "Frost Sigil never turns solid ground into ice");

        for (int x = -1; x <= 1; x++) level.setBlockAndUpdate(POS.offset(x, -1, 0), Blocks.WATER.defaultBlockState());
        noLP.tickCount = 101;
        frost.getItem().inventoryTick(frost, level, noLP, EquipmentSlot.MAINHAND);
        boolean waterFrozen = true;
        for (int x = -1; x <= 1; x++) waterFrozen &= level.getBlockState(POS.offset(x, -1, 0)).is(Blocks.FROSTED_ICE);
        check(waterFrozen && noLP.getHealth() == 18,
                "already charged interval freezes water without an extra health debit");

        var funded = MenuTestPlayer.create(level, "Hotfix3Funded");
        funded.setGameMode(GameType.SURVIVAL);
        funded.setHealth(20);
        funded.setPos(POS.getX() + .5, POS.getY(), POS.getZ() + 4.5);
        clearArea(level, POS.offset(0, 0, 4));
        for (int x = -1; x <= 1; x++) level.setBlockAndUpdate(POS.offset(x, -1, 4), Blocks.WATER.defaultBlockState());
        networks.consume(funded.getUUID(), networks.get(funded.getUUID()).currentEssence());
        networks.fill(funded.getUUID(), 1000, 5000);
        var paidFrost = new ItemStack(BloodMagicItems.ICE_SIGIL.get());
        funded.setItemInHand(InteractionHand.MAIN_HAND, paidFrost);
        funded.setShiftKeyDown(true);
        check(paidFrost.getItem().use(level, funded, InteractionHand.MAIN_HAND).consumesAction()
                        && funded.getHealth() == 20 && networks.get(funded.getUUID()).currentEssence() == 900,
                "funded activation spends LP and preserves health");
        funded.setShiftKeyDown(false);
        funded.tickCount = 101;
        paidFrost.getItem().inventoryTick(paidFrost, level, funded, EquipmentSlot.MAINHAND);
        check(level.getBlockState(POS.offset(0, -1, 4)).is(Blocks.FROSTED_ICE),
                "paid Frost Sigil freezes nearby source water");

        var lethal = MenuTestPlayer.create(level, "Hotfix3Unpayable");
        lethal.setGameMode(GameType.SURVIVAL);
        lethal.setHealth(1);
        lethal.setPos(POS.getX() + .5, POS.getY(), POS.getZ() - 4.5);
        networks.consume(lethal.getUUID(), networks.get(lethal.getUUID()).currentEssence());
        var unpayable = new ItemStack(BloodMagicItems.ICE_SIGIL.get());
        lethal.setItemInHand(InteractionHand.MAIN_HAND, unpayable);
        lethal.setShiftKeyDown(true);
        check(!unpayable.getItem().use(level, lethal, InteractionHand.MAIN_HAND).consumesAction()
                        && !ItemSigilToggleableBase.activated(unpayable) && lethal.getHealth() == 1,
                "Frost Sigil cannot activate when neither LP nor safe health can pay");

        clearArea(level, POS.offset(0, 0, 4));
        clearArea(level, POS);
        return assertions;
    }

    private static void clearArea(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE6_SMOKE FAIL: " + message);
        assertions++;
    }
}
