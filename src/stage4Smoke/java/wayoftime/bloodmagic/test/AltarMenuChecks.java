package wayoftime.bloodmagic.test;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.altar.AltarState;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.BloodOrbData;
import wayoftime.bloodmagic.common.menu.AltarMenu;
import wayoftime.bloodmagic.common.registries.BloodMagicMenus;

/** Real server opening/click handlers + codec round trips into a headless client-side menu model. */
final class AltarMenuChecks {
    private static final BlockPos POS = new BlockPos(192, 180, 192);
    private static int assertions;

    static int run(ServerLevel level) {
        level.getChunk(POS);
        level.setBlockAndUpdate(POS, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
        var altar = (BloodAltarBlockEntity) level.getBlockEntity(POS);
        altar.setStoredItem(new ItemStack(Items.STONE, 2));
        altar.setAltarState(new AltarState(100_000, 200_000, 0, 0, 1_000, 5, 70_000, 80_000, true));
        var player = MenuTestPlayer.create(level, "Stage4Menu");
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(POS.getX() + 0.5, POS.getY(), POS.getZ() + 2);
        player.getInventory().clearContent();
        var hit = new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false);
        var connection = MenuTestPlayer.connection(player);
        connection.packets.clear();

        check(BuiltInRegistries.MENU.getKey(BloodMagicMenus.ALTAR.get()).equals(BloodMagic.rl("altar")), "registered altar MenuType");
        check(altar.getBlockState().useItemOn(ItemStack.EMPTY, level, player, InteractionHand.MAIN_HAND, hit)
                == InteractionResult.PASS && player.containerMenu == player.inventoryMenu, "empty useItemOn delegates without double opening");
        check(altar.getBlockState().useWithoutItem(level, player, hit).consumesAction()
                && player.containerMenu instanceof AltarMenu, "real ServerPlayer opens menu from block empty-hand entry");
        AltarMenu menu = (AltarMenu) player.containerMenu;
        check(altar.getStoredItem().getCount() == 2 && player.getInventory().isEmpty(), "occupied altar opens GUI without ejecting item");
        check(menu.slots.size() == 37 && menu.getSlot(0).container == altar && menu.getSlot(0).getContainerSlot() == 0,
                "one shared altar slot and 36 player slots");
        check(menu.getSlot(1).getContainerSlot() == 9 && menu.getSlot(27).getContainerSlot() == 35
                && menu.getSlot(28).getContainerSlot() == 0 && menu.getSlot(36).getContainerSlot() == 8,
                "main inventory and hotbar boundaries");
        var open = connection.packets.stream().filter(ClientboundOpenScreenPacket.class::isInstance)
                .map(ClientboundOpenScreenPacket.class::cast).findFirst().orElseThrow();
        var decodedOpen = roundTrip(level, ClientboundOpenScreenPacket.STREAM_CODEC, open);
        check(decodedOpen.getType() == BloodMagicMenus.ALTAR.get() && decodedOpen.getContainerId() == menu.containerId
                && decodedOpen.getTitle().equals(altar.getDisplayName()), "real open packet preserves type, id and title");
        var mirrorPlayer = MenuTestPlayer.create(level, "Stage4Mirror");
        AltarMenu mirror = BloodMagicMenus.ALTAR.get().create(decodedOpen.getContainerId(), mirrorPlayer.getInventory());
        check(mirror.slots.size() == 37 && mirror.getSlot(0).container != altar, "client factory needs no client BE/world lookup");
        applyPackets(level, player, mirror);
        check(mirror.blood() == 100_000 && mirror.capacity() == 200_000 && mirror.progress() == 70_000
                && mirror.requiredBlood() == 80_000 && mirror.tier() == 5 && mirror.active(), "initial full 32-bit menu data sync");
        check(mirror.getSlot(0).getItem().getCount() == 2, "initial inventory snapshot reaches client menu");
        check(mirror.progressPixels(104) == 91 && mirror.bloodPixels(160) == 80, "GUI ratios use LP rather than ticks");

        altar.setAltarState(new AltarState(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 0, 0, 1_000, 6,
                1_500_000_000, 2_000_000_000, true));
        menu.broadcastChanges();
        applyPackets(level, player, mirror);
        check(mirror.blood() == Integer.MAX_VALUE - 1 && mirror.capacity() == Integer.MAX_VALUE
                && mirror.progress() == 1_500_000_000 && mirror.requiredBlood() == 2_000_000_000, "delta sync never truncates high LP/progress");
        check(mirror.bloodPixels(160) == 159 && mirror.progressPixels(104) == 78, "bar arithmetic avoids integer overflow");
        var authoritative = altar.getAltarState();
        menu.setData(0, 0);
        menu.setData(3, Integer.MAX_VALUE);
        check(altar.getAltarState().equals(authoritative), "menu data cannot write server LP or progress");
        menu.broadcastChanges();
        check(connection.packets.isEmpty(), "unchanged menu sends no redundant deltas");
        mirror.getSlot(0).setByPlayer(ItemStack.EMPTY);
        mirror.setData(0, 7);
        check(altar.getStoredItem().getCount() == 2 && altar.getAltarState().equals(authoritative), "client dummy storage is isolated from authoritative BE");

        altar.clearContent();
        altar.setAltarState(AltarState.EMPTY);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 5));
        click(level, player, 1, 0, ContainerInput.QUICK_MOVE);
        check(altar.getStoredItem().getCount() == 5 && player.getInventory().getItem(9).isEmpty(), "shift-click main inventory inserts full stack");
        applyPackets(level, player, mirror);
        check(mirror.getSlot(0).getItem().getCount() == 5 && mirror.getSlot(1).getItem().isEmpty(), "click result synchronizes both inventories");
        click(level, player, 0, 0, ContainerInput.QUICK_MOVE);
        check(altar.isEmpty() && player.getInventory().countItem(Items.STONE) == 5, "shift-click altar extracts stack");
        click(level, player, 0, 0, ContainerInput.QUICK_MOVE);
        check(player.getInventory().countItem(Items.STONE) == 5, "repeated shift extraction cannot duplicate");
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 3));
        click(level, player, 28, 0, ContainerInput.QUICK_MOVE);
        check(altar.getStoredItem().getCount() == 3 && player.getMainHandItem().isEmpty(), "shift-click hotbar inserts at correct boundary");

        altar.setStoredItem(new ItemStack(Items.STONE, 60));
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 10));
        click(level, player, 1, 0, ContainerInput.QUICK_MOVE);
        check(altar.getStoredItem().getCount() == 64 && player.getInventory().getItem(9).getCount() == 6,
                "partial insertion merges only available capacity");
        player.getInventory().setItem(10, new ItemStack(Items.DIRT, 4));
        click(level, player, 2, 0, ContainerInput.QUICK_MOVE);
        check(altar.getStoredItem().getCount() == 64 && player.getInventory().getItem(10).getCount() == 4,
                "occupied incompatible altar cannot be overwritten");
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        altar.setStoredItem(new ItemStack(Items.STONE, 5));
        click(level, player, 0, 0, ContainerInput.QUICK_MOVE);
        check(altar.getStoredItem().getCount() == 5 && player.getInventory().countItem(Items.STONE) == 0, "full inventory leaves altar stack unchanged");
        player.getInventory().setItem(8, new ItemStack(Items.STONE, 63));
        altar.setAltarState(new AltarState(1234, 10_000, 0, 0, 1_000, 1, 300, 5_000, true));
        click(level, player, 0, 0, ContainerInput.QUICK_MOVE);
        check(altar.getStoredItem().getCount() == 4 && player.getInventory().getItem(8).getCount() == 64,
                "partial extraction commits live Container stack mutation");
        check(altar.getAltarState().progress() == 0 && altar.getAltarState().blood() == 1234,
                "partial extraction resets paid recipe without refunding LP");

        altar.setAltarState(new AltarState(1234, 10_000, 0, 0, 1_000, 1, 50, 4000, true));
        altar.setChanged();
        check(altar.getAltarState().progress() == 50, "ordinary dirty flag does not erase progress");
        ItemStack removed = altar.removeItem(0, 1);
        check(removed.getCount() == 1 && altar.getStoredItem().getCount() == 3 && altar.getAltarState().progress() == 0,
                "Container.removeItem splits and invalidates recipe");
        check(altar.removeItem(1, 1).isEmpty() && altar.removeItem(0, -1).isEmpty() && altar.getStoredItem().getCount() == 3,
                "invalid slot and negative extraction are inert");
        check(altar.removeItemNoUpdate(0).getCount() == 3 && altar.isEmpty(), "Container.removeItemNoUpdate clears storage safely");

        player.getInventory().clearContent();
        altar.setStoredItem(new ItemStack(Items.STONE, 5));
        click(level, player, 0, 1, ContainerInput.PICKUP);
        check(menu.getCarried().getCount() == 3 && altar.getStoredItem().getCount() == 2, "ordinary right click picks up half stack");
        click(level, player, 0, 0, ContainerInput.PICKUP);
        check(menu.getCarried().isEmpty() && altar.getStoredItem().getCount() == 5, "ordinary left click merges carried stack");
        ItemStack orb = new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get());
        BloodOrbData.bind(orb, UUID.fromString("12345678-1234-5678-9abc-123456789abc"), "MenuOwner");
        player.getInventory().setItem(0, orb);
        click(level, player, 0, 0, ContainerInput.SWAP);
        check(altar.getStoredItem().is(orb.getItem()) && altar.getStoredItem().getCount() == 1
                && BloodOrbData.getBinding(altar.getStoredItem()).equals(BloodOrbData.getBinding(orb))
                && player.getInventory().getItem(0).getCount() == 5, "number-key swap retains item components and both stacks");
        applyPackets(level, player, mirror);
        check(BloodOrbData.getBinding(mirror.getSlot(0).getItem()).equals(BloodOrbData.getBinding(orb)), "slot network codec preserves bound orb components");

        var second = MenuTestPlayer.create(level, "Stage4Second");
        second.setPos(player.getX(), player.getY(), player.getZ());
        check(second.openMenu(altar).isPresent(), "second viewer opens same altar");
        AltarMenu secondMenu = (AltarMenu) second.containerMenu;
        check(secondMenu.getSlot(0).container == menu.getSlot(0).container, "multiple viewers share authoritative inventory");
        click(level, second, 0, 0, ContainerInput.QUICK_MOVE);
        menu.broadcastChanges();
        applyPackets(level, player, mirror);
        check(altar.isEmpty() && mirror.getSlot(0).getItem().isEmpty()
                && second.getInventory().countItem(orb.getItem()) == 1, "one viewer extraction updates other viewer without duplication");
        second.closeContainer();
        altar.setStoredItem(new ItemStack(Items.STONE));
        altar.fillMainTank(2000);
        BloodAltarBlockEntity.serverTick(level, POS, altar.getBlockState(), altar);
        menu.broadcastChanges();
        applyPackets(level, player, mirror);
        check(mirror.progress() == 5 && mirror.requiredBlood() == 1000 && mirror.blood() == 3229,
                "actual altar tick broadcasts LP debit and crafting progress to open menu");
        for (int tick = 1; tick < 200; tick++) BloodAltarBlockEntity.serverTick(level, POS, altar.getBlockState(), altar);
        menu.broadcastChanges();
        applyPackets(level, player, mirror);
        check(mirror.getSlot(0).getItem().is(BloodMagicItems.SLATE.get()) && mirror.progress() == 0 && !mirror.active(),
                "craft completion synchronizes output and clears displayed progress");
        check(menu.quickMoveStack(player, -1).isEmpty() && menu.quickMoveStack(player, 37).isEmpty(), "invalid shift indices are safe");

        var before = altar.getStoredItem();
        player.setPos(0, 180, 0);
        click(level, player, 0, 0, ContainerInput.QUICK_MOVE);
        check(!menu.stillValid(player) && ItemStack.matches(altar.getStoredItem(), before)
                && altar.createMenu(7, player.getInventory(), player) == null, "distant player cannot open or mutate menu");
        player.setPos(POS.getX() + 0.5, POS.getY(), POS.getZ() + 2);
        player.setGameMode(GameType.SPECTATOR);
        check(!menu.stillValid(player) && altar.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "spectators cannot modify or open altar menu");
        player.setGameMode(GameType.SURVIVAL);
        player.closeContainer();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check(ItemStack.matches(before, altar.getStoredItem()), "closing menu preserves block inventory");
        check(altar.getBlockState().useWithoutItem(level, player, hit).consumesAction(), "reopening occupied altar works");
        menu = (AltarMenu) player.containerMenu;
        altar.clearContent();
        player.closeContainer();
        check(altar.getBlockState().useWithoutItem(level, player, hit).consumesAction(), "empty altar also opens GUI");
        menu = (AltarMenu) player.containerMenu;
        player.closeContainer();
        player.setShiftKeyDown(true);
        player.setHealth(20);
        player.invulnerableTime = 0;
        check(altar.getBlockState().useWithoutItem(level, player, hit).consumesAction()
                && player.getHealth() == 18 && player.containerMenu == player.inventoryMenu,
                "sneaking empty use sacrifices without opening menu");
        player.setShiftKeyDown(false);
        player.openMenu(altar);
        menu = (AltarMenu) player.containerMenu;
        altar.setStoredItem(new ItemStack(Items.STONE));
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        check(!menu.stillValid(player) && menu.quickMoveStack(player, 0).isEmpty(), "removed altar invalidates existing menu");
        level.setBlockAndUpdate(POS, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
        check(!menu.stillValid(player), "same-position replacement cannot revive old menu");
        player.closeContainer();
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        return assertions;
    }

    private static void click(ServerLevel level, ServerPlayer player, int slot, int button, ContainerInput input) {
        // Predictions are intentionally empty: server corrects from its own authoritative inventory.
        var packet = new ServerboundContainerClickPacket(player.containerMenu.containerId, player.containerMenu.getStateId(),
                (short) slot, (byte) button, input, new Int2ObjectOpenHashMap<HashedStack>(), HashedStack.EMPTY);
        player.connection.handleContainerClick(roundTrip(level, ServerboundContainerClickPacket.STREAM_CODEC, packet));
    }

    private static void applyPackets(ServerLevel level, ServerPlayer player, AltarMenu mirror) {
        var packets = MenuTestPlayer.connection(player).packets;
        for (Packet<?> packet : List.copyOf(packets)) {
            if (packet instanceof ClientboundContainerSetContentPacket content && content.containerId() == mirror.containerId) {
                var decoded = roundTrip(level, ClientboundContainerSetContentPacket.STREAM_CODEC, content);
                mirror.initializeContents(decoded.stateId(), decoded.items(), decoded.carriedItem());
            } else if (packet instanceof ClientboundContainerSetSlotPacket slot && slot.getContainerId() == mirror.containerId) {
                var decoded = roundTrip(level, ClientboundContainerSetSlotPacket.STREAM_CODEC, slot);
                mirror.setItem(decoded.getSlot(), decoded.getStateId(), decoded.getItem());
            } else if (packet instanceof ClientboundCustomPayloadPacket custom
                    && custom.payload() instanceof AdvancedContainerSetDataPayload data
                    && Byte.toUnsignedInt(data.containerId()) == mirror.containerId) {
                var decoded = roundTrip(level, AdvancedContainerSetDataPayload.STREAM_CODEC, data);
                mirror.setData(decoded.dataId(), decoded.value());
            }
        }
        packets.clear();
    }

    private static <T> T roundTrip(ServerLevel level, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            codec.encode(buffer, value);
            T decoded = codec.decode(buffer);
            if (buffer.isReadable()) throw new IllegalStateException("Unconsumed menu packet bytes");
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE4_SMOKE FAIL: " + message);
        assertions++;
    }
}
