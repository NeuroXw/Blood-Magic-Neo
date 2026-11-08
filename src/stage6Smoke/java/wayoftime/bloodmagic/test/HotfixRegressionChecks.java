package wayoftime.bloodmagic.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.block.BlockDemonCrystal;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.common.block.entity.DemonCrucibleBlockEntity;
import wayoftime.bloodmagic.common.entity.SoulSnare;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemDemonWill;
import wayoftime.bloodmagic.common.menu.ArcMenu;
import wayoftime.bloodmagic.common.registries.BloodMagicWill;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;

/** Hotfix regressions driven through real server packets and entity/event paths. */
final class HotfixRegressionChecks {
    private static final BlockPos ALTAR = new BlockPos(640, 180, 640);
    private static final BlockPos ARC = new BlockPos(646, 180, 640);
    private static final BlockPos CRYSTAL = new BlockPos(652, 181, 640);
    private static final BlockPos CRUCIBLE = new BlockPos(658, 180, 640);
    private static final BlockPos MOB = new BlockPos(664, 180, 640);
    private static int assertions;

    static int run(ServerLevel level) {
        int start = assertions;
        ServerPlayer player = MenuTestPlayer.create(level, "HotfixPlayer");
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(20);
        player.clearFire();

        clear(level, ALTAR);
        level.setBlockAndUpdate(ALTAR, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
        BloodAltarBlockEntity altar = (BloodAltarBlockEntity) level.getBlockEntity(ALTAR);
        player.setPos(ALTAR.getX() + .5, ALTAR.getY(), ALTAR.getZ() + 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BloodMagicItems.SACRIFICIAL_DAGGER.get()));
        PlayerInteractionChecks.useOn(player, ALTAR);
        check(player.getHealth() == 18 && altar.getAltarState().blood() == 200,
                "held sacrificial dagger sacrifices through real right-click: health=" + player.getHealth() + ", LP=" + altar.getAltarState().blood());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        clear(level, ARC);
        level.setBlockAndUpdate(ARC, BloodMagicBlocks.ALCHEMICAL_REACTION_CHAMBER.get().defaultBlockState());
        player.setPos(ARC.getX() + .5, ARC.getY(), ARC.getZ() + 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BloodMagicItems.SIMPLE_CATALYST.get()));
        PlayerInteractionChecks.useOn(player, ARC);
        check(player.containerMenu instanceof ArcMenu, "held-item right-click opens ARC");
        player.closeContainer();

        clear(level, CRYSTAL.below());
        level.setBlockAndUpdate(CRYSTAL.below(), Blocks.STONE.defaultBlockState());
        clear(level, CRYSTAL);
        var crystalState = BloodMagicBlocks.RAW_DEMON_CRYSTAL.get().defaultBlockState()
                .setValue(BlockDemonCrystal.ATTACHED, Direction.UP).setValue(BlockDemonCrystal.AGE, 2);
        level.setBlockAndUpdate(CRYSTAL, crystalState);
        var greaterGem = new ItemStack(BloodMagicItems.GREATER_GEM.get());
        ItemDemonWill.setWill(greaterGem, EnumDemonWillType.DEFAULT, 600);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, greaterGem);
        player.setPos(CRYSTAL.getX() + .5, CRYSTAL.getY(), CRYSTAL.getZ() + 2);
        PlayerInteractionChecks.useOn(player, CRYSTAL);
        check(level.getBlockState(CRYSTAL).getValue(BlockDemonCrystal.AGE) == 1,
                "charged gem harvests one will crystal through real right-click");

        clear(level, CRUCIBLE);
        clearAura(level, CRUCIBLE);
        level.setBlockAndUpdate(CRUCIBLE, BloodMagicBlocks.DEMON_CRUCIBLE.get().defaultBlockState());
        DemonCrucibleBlockEntity crucible = (DemonCrucibleBlockEntity) level.getBlockEntity(CRUCIBLE);
        var crystalItem = new ItemStack(BloodMagicItems.RAW_CRYSTAL.get(), 2);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, crystalItem);
        player.setPos(CRUCIBLE.getX() + .5, CRUCIBLE.getY(), CRUCIBLE.getZ() + 2);
        PlayerInteractionChecks.useOn(player, CRUCIBLE);
        check(crucible.getItem(0).is(BloodMagicItems.RAW_CRYSTAL.get()) && player.getMainHandItem().getCount() == 1,
                "will crystal inserts into crucible through real right-click");
        for (int i = 0; i < 4; i++) DemonCrucibleBlockEntity.serverTick(level, CRUCIBLE, crucible.getBlockState(), crucible);
        check(WorldDemonWillHandler.getCurrentWill(level, CRUCIBLE, EnumDemonWillType.DEFAULT) == 50,
                "will crystal feeds chunk aura");

        player.getInventory().clearContent();
        var pickupGem = new ItemStack(BloodMagicItems.PETTY_GEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pickupGem);
        var rawDrop = new ItemStack(BloodMagicItems.RAW_WILL.get());
        ItemDemonWill.setAmount(rawDrop, 2.5);
        ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), rawDrop);
        level.addFreshEntity(itemEntity);
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Pre(player, itemEntity));
        check(ItemDemonWill.amount(player.getMainHandItem()) == 2.5 && itemEntity.isRemoved(),
                "raw will pickup fills a gem without losing will");

        player.getInventory().clearContent();
        var transferGem = new ItemStack(BloodMagicItems.PETTY_GEM.get());
        player.getInventory().setItem(9, transferGem);
        var transferSoul = new ItemStack(BloodMagicItems.RAW_WILL.get());
        ItemDemonWill.setAmount(transferSoul, 3.25);
        player.setItemInHand(InteractionHand.MAIN_HAND, transferSoul);
        check(transferSoul.getItem().use(level, player, InteractionHand.MAIN_HAND).consumesAction()
                && ItemDemonWill.amount(player.getInventory().getItem(9)) == 3.25,
                "will gem absorbs raw will through direct item use");

        player.getInventory().clearContent();
        player.setPos(MOB.getX() + 10.5, MOB.getY(), MOB.getZ() + .5);
        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        check(zombie != null, "zombie fixture created");
        zombie.setPos(MOB.getX() + .5, MOB.getY(), MOB.getZ() + .5);
        level.addFreshEntity(zombie);
        var snare = new SoulSnare(level, player, new ItemStack(BloodMagicItems.SOUL_SNARE.get()));
        snare.setPos(zombie.getX(), zombie.getY() + .5, zombie.getZ());
        level.addFreshEntity(snare);
        try {
            var hitMethod = SoulSnare.class.getDeclaredMethod("onHitEntity", net.minecraft.world.phys.EntityHitResult.class);
            hitMethod.setAccessible(true);
            hitMethod.invoke(snare, new net.minecraft.world.phys.EntityHitResult(zombie));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke soul snare hit handler", exception);
        }
        check(zombie.hasEffect(BloodMagicWill.SNARED), "soul snare projectile marks a mob on hit");

        player.getInventory().clearContent();
        var directGem = new ItemStack(BloodMagicItems.PETTY_GEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, directGem);
        var snaredDrops = new java.util.ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(
                zombie, zombie.damageSources().playerAttack(player), snaredDrops, true));
        check(ItemDemonWill.amount(player.getMainHandItem()) > 0 && snaredDrops.isEmpty(),
                "snared mob kill auto-fills a carried will gem");
        zombie.discard();
        snare.discard();

        clear(level, CRYSTAL); clear(level, CRYSTAL.below()); clear(level, CRUCIBLE); clear(level, ARC); clear(level, ALTAR);
        player.getInventory().clearContent();
        return assertions - start;
    }

    private static void clearAura(ServerLevel level, BlockPos pos) {
        for (EnumDemonWillType type : EnumDemonWillType.values()) {
            double amount = WorldDemonWillHandler.getCurrentWill(level, pos, type);
            if (amount > 0) WorldDemonWillHandler.drainWill(level, pos, type, amount, true);
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







