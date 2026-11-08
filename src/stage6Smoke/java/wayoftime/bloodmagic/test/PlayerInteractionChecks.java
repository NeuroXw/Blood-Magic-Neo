package wayoftime.bloodmagic.test;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wayoftime.bloodmagic.common.block.*;
import wayoftime.bloodmagic.common.block.entity.*;
import wayoftime.bloodmagic.common.item.*;
import wayoftime.bloodmagic.common.menu.*;
import wayoftime.bloodmagic.core.data.*;

/** Regression for the 26.1 TRY_WITH_EMPTY_HAND dispatch and actual player orb insertion. */
final class PlayerInteractionChecks {
    private static final BlockPos POS = new BlockPos(416, 180, 416);
    private static int sequence;
    private static int assertions;

    static void useOn(ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ServerboundUseItemOnPacket.STREAM_CODEC.encode(buffer,
                    new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, ++sequence));
            player.connection.handleUseItemOn(ServerboundUseItemOnPacket.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    static int run(ServerLevel level) {
        assertions = 0;
        level.getChunk(POS);
        var player = MenuTestPlayer.create(level, "InteractionHotfix");
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(20);
        player.setPos(POS.getX() + .5, POS.getY(), POS.getZ() + 2);
        var networks = SoulNetworks.get(level);
        int previousTier = networks.get(player.getUUID()).orbTier();
        networks.consume(player.getUUID(), networks.get(player.getUUID()).currentEssence());
        if (level.getBlockEntity(POS) instanceof net.minecraft.world.Container inventory) inventory.clearContent();
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
        var altar = (BloodAltarBlockEntity) level.getBlockEntity(POS);
        useOn(player, POS);
        check(player.containerMenu instanceof AltarMenu, "empty altar right-click opens GUI");
        check(MenuTestPlayer.connection(player).packets.stream().filter(ClientboundOpenScreenPacket.class::isInstance).count() == 1,
                "one right-click sends exactly one open-screen packet");
        player.closeContainer();
        player.setShiftKeyDown(true);
        useOn(player, POS);
        check(altar.getAltarState().blood() == 200 && player.getHealth() == 18
                && player.containerMenu == player.inventoryMenu, "sneak right-click supplies altar blood");
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get()));
        useOn(player, POS);
        check(player.getMainHandItem().isEmpty() && ItemBindable.binding(altar.getItem(0)).ownerId().equals(player.getUUID()),
                "direct insertion binds a new orb to its player and consumes one item");
        BloodAltarBlockEntity.serverTick(level, POS, altar.getBlockState(), altar);
        check(networks.get(player.getUUID()).currentEssence() == 2 && altar.getAltarState().blood() == 198,
                "newly inserted orb charges and conserves altar/network LP");
        check(player.getHealth() == 18 && networks.get(player.getUUID()).orbTier() == Math.max(1, previousTier),
                "insertion binding records orb tier without health sacrifice");
        altar.clearContent();
        useOn(player, POS);
        var menu = (AltarMenu) player.containerMenu;
        menu.setCarried(new ItemStack(BloodMagicItems.APPRENTICE_BLOOD_ORB.get()));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        check(ItemBindable.binding(altar.getItem(0)).ownerId().equals(player.getUUID()), "GUI cursor insertion binds orb");
        BloodAltarBlockEntity.serverTick(level, POS, altar.getBlockState(), altar);
        check(networks.get(player.getUUID()).currentEssence() == 7 && altar.getAltarState().blood() == 193,
                "GUI inserted orb charges at its correct rate");
        altar.clearContent();
        player.getInventory().setItem(9, new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get()));
        menu.quickMoveStack(player, 1);
        check(ItemBindable.binding(altar.getItem(0)).ownerId().equals(player.getUUID()), "shift insertion binds orb");
        altar.clearContent();
        UUID owner = UUID.randomUUID();
        ItemStack borrowed = new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get());
        BloodOrbData.bind(borrowed, owner, "OriginalOwner");
        menu.setCarried(borrowed);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        BloodAltarBlockEntity.serverTick(level, POS, altar.getBlockState(), altar);
        check(ItemBindable.binding(altar.getItem(0)).ownerId().equals(owner)
                && networks.get(owner).currentEssence() == 2, "borrowed orb keeps and charges original owner");
        altar.clearContent(); player.closeContainer();
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());

        // Both table halves must lead through the block-use handler to the same inventory.
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var state = BloodMagicBlocks.ALCHEMY_TABLE.get().defaultBlockState().setValue(BlockAlchemyTable.FACING, facing);
            BlockPos second = BlockAlchemyTable.other(POS, state);
            level.getChunk(second);
            level.setBlockAndUpdate(POS, state);
            level.setBlockAndUpdate(second, state.setValue(BlockAlchemyTable.PART, BedPart.HEAD));
            var table = (AlchemyTableBlockEntity) level.getBlockEntity(POS);
            for (BlockPos half : new BlockPos[]{POS, second}) {
                player.setPos(half.getX() + .5, half.getY() + 1, half.getZ() + .5);
                useOn(player, half);
                check(player.containerMenu instanceof MachineMenu m && m.isTable()
                        && m.slots.get(0).container == table, "table right-click opens shared menu: " + facing + "/" + half);
                player.closeContainer();
            }
            useOn(player, POS);
            var tableMenu = (MachineMenu) player.containerMenu;
            tableMenu.setCarried(new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get()));
            tableMenu.clicked(6, 0, ContainerInput.PICKUP, player);
            check(ItemBindable.binding(table.getItem(6)).ownerId().equals(player.getUUID()), "table catalyst insertion binds orb");
            table.clearContent(); player.closeContainer();
            level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(POS, BloodMagicBlocks.HELLFIRE_FORGE.get().defaultBlockState());
        player.setPos(POS.getX() + .5, POS.getY(), POS.getZ() + 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        useOn(player, POS);
        check(player.containerMenu instanceof MachineMenu m && !m.isTable(), "held-item right-click also opens forge");
        player.closeContainer(); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        useOn(player, POS);
        check(player.containerMenu instanceof MachineMenu m && !m.isTable(), "empty-hand right-click opens forge");
        player.closeContainer();
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        return assertions;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE6_SMOKE FAIL: " + message);
        assertions++;
    }
}
