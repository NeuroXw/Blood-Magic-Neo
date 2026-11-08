package wayoftime.bloodmagic.test;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.connection.ConnectionType;
import wayoftime.bloodmagic.altar.AltarUpgrades;
import wayoftime.bloodmagic.block.enums.BloodRuneType;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.BloodOrbData;
import wayoftime.bloodmagic.common.item.ItemBindable;
import wayoftime.bloodmagic.common.item.ItemBloodOrb;
import wayoftime.bloodmagic.common.item.sigil.ItemSigilBase;
import wayoftime.bloodmagic.common.registries.BloodMagicBloodOrbs;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.core.data.SoulNetworks;
import wayoftime.bloodmagic.core.data.SoulNetworkPayment;

/** Real dedicated server players/listeners, wire codecs and world mutations; no GUI/GPU claims. */
final class OrbSigilChecks {
    private static final BlockPos ALTAR = new BlockPos(256, 180, 256);
    private static final BlockPos FLUID = new BlockPos(260, 180, 256);
    private static final UUID SAVED = UUID.fromString("e98430df-96db-4a9f-85e1-883eeea00005");
    private static final UUID TICK_OWNER = UUID.fromString("e98430df-96db-4a9f-85e1-883eeea00006");
    private static final Path SAVE_MARKER = Path.of("stage5-network-expected.txt");
    private static int assertions;
    private static int sequence;

    static int begin(ServerLevel level) throws java.io.IOException {
        int start = assertions;
        SoulNetworks networks = SoulNetworks.get(level);
        if (Files.exists(SAVE_MARKER)) {
            check(networks.get(SAVED).currentEssence() == 123_456 && networks.get(SAVED).orbTier() == 5,
                    "world restart retains UUID network balance and highest orb tier");
        } else {
            check(networks.get(SAVED).currentEssence() == 0, "new network persistence fixture");
        }
        clear(networks, SAVED);
        networks.recordOrb(SAVED, 5);
        networks.fill(SAVED, 123_456, 10_000_000);
        check(SoulNetworks.get(level.getServer().getLevel(Level.NETHER)) == networks,
                "all dimensions share overworld-owned SavedData");
        var encoded = SoulNetworks.CODEC.encodeStart(JsonOps.INSTANCE, networks).getOrThrow();
        var restored = SoulNetworks.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        check(restored.get(SAVED).equals(networks.get(SAVED)), "network codec round trip");
        restored.consume(SAVED, 1);
        check(networks.get(SAVED).currentEssence() == 123_456, "decoded network is not aliased");
        var extreme = UUID.randomUUID();
        check(networks.fill(extreme, Integer.MAX_VALUE, Integer.MAX_VALUE) == Integer.MAX_VALUE,
                "network fill at maximum int");
        check(networks.fill(extreme, Integer.MAX_VALUE, Integer.MAX_VALUE) == 0
                && networks.fill(extreme, -1, Integer.MAX_VALUE) == 0, "full/negative fill cannot overflow");
        check(!networks.consume(extreme, -1) && networks.get(extreme).currentEssence() == Integer.MAX_VALUE,
                "negative debit cannot mint LP");
        networks.consume(extreme, Integer.MAX_VALUE);
        check(!networks.consume(extreme, 1), "failed debit leaves empty balance");
        testOrbs(level);
        testSigils(level);
        testFluids(level);
        testAltar(level);
        clear(networks, TICK_OWNER);
        var ticking = place(level);
        ticking.setStoredItem(boundOrb(1, TICK_OWNER));
        ticking.fillMainTank(100);
        level.getChunkSource().updateChunkForced(ChunkPos.containing(ALTAR), true);
        return assertions - start;
    }

    static int finish(ServerLevel level) throws java.io.IOException {
        int start = assertions;
        var altar = (BloodAltarBlockEntity) level.getBlockEntity(ALTAR);
        int balance = SoulNetworks.get(level).get(TICK_OWNER).currentEssence();
        check(balance > 0 && balance + altar.getAltarState().blood() == 100,
                "registered world ticker charges offline owner and conserves LP through reloads");
        check(altar.getStoredItem().is(BloodMagicItems.WEAK_BLOOD_ORB.get())
                && BloodOrbData.getBinding(altar.getStoredItem()).orElseThrow().ownerId().equals(TICK_OWNER),
                "natural charging does not consume orb or binding");
        altar.clearContent();
        level.setBlockAndUpdate(ALTAR, Blocks.AIR.defaultBlockState());
        level.getChunkSource().updateChunkForced(ChunkPos.containing(ALTAR), false);
        Files.writeString(SAVE_MARKER, "123456 LP / tier 5\n");
        return assertions - start;
    }

    private static void testOrbs(ServerLevel level) {
        var items = List.of(BloodMagicItems.WEAK_BLOOD_ORB, BloodMagicItems.APPRENTICE_BLOOD_ORB,
                BloodMagicItems.MAGICIAN_BLOOD_ORB, BloodMagicItems.MASTER_BLOOD_ORB, BloodMagicItems.ARCHMAGE_BLOOD_ORB);
        int[] capacities = {5_000, 25_000, 150_000, 1_000_000, 10_000_000};
        int[] rates = {2, 5, 15, 25, 50};
        SoulNetworks networks = SoulNetworks.get(level);
        check(BloodMagicBloodOrbs.REGISTRY.size() == 5, "five real registered blood orb definitions");
        for (int i = 0; i < 5; i++) {
            var definition = BloodMagicBloodOrbs.forTier(i + 1);
            check(definition.capacity() == capacities[i] && definition.fillRate() == rates[i], "upstream orb constants " + i);
            var player = player(level, "S5Orb" + i);
            clear(networks, player.getUUID());
            var stack = new ItemStack(items.get(i).get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            use(player, InteractionHand.MAIN_HAND);
            check(BloodOrbData.getBinding(stack).orElseThrow().ownerId().equals(player.getUUID()), "wire use binds orb " + i);
            check(networks.get(player.getUUID()).currentEssence() == 200 && player.getHealth() == 18,
                    "wire use sacrifices two health for 200 LP " + i + ": " + player.getHealth());
            check(networks.get(player.getUUID()).orbTier() == i + 1, "using own orb records tier " + i);
            var remainder = stack.getCraftingRemainder();
            check(remainder != null && ItemStack.isSameItemSameComponents(stack, remainder.create()), "craft remainder preserves all components " + i);
            var clientCopy = roundTripItem(level, stack);
            check(ItemStack.matches(stack, clientCopy), "bound orb wire component round trip " + i);
            var other = player(level, "S5Other" + i);
            clear(networks, other.getUUID());
            other.setItemInHand(InteractionHand.OFF_HAND, stack.copy());
            use(other, InteractionHand.OFF_HAND);
            check(networks.get(player.getUUID()).currentEssence() == 400 && networks.get(other.getUUID()).currentEssence() == 0,
                    "borrower credits bound owner, not user " + i);
            check(other.getHealth() == 18 && ItemBindable.binding(other.getOffhandItem()).ownerId().equals(player.getUUID()),
                    "offhand borrower pays health and cannot rebind " + i);
            networks.fill(player.getUUID(), Integer.MAX_VALUE, capacities[i]);
            player.setHealth(20);
            use(player, InteractionHand.MAIN_HAND);
            check(player.getHealth() == 20 && networks.get(player.getUUID()).currentEssence() == capacities[i], "full orb no damage " + i);
            clear(networks, player.getUUID());
            player.setHealth(1);
            use(player, InteractionHand.MAIN_HAND);
            check(player.getHealth() == 1 && networks.get(player.getUUID()).currentEssence() == 0, "orb cannot kill user for LP " + i);
            player.setHealth(20);
            player.getAbilities().invulnerable = true;
            use(player, InteractionHand.MAIN_HAND);
            check(networks.get(player.getUUID()).currentEssence() == 0 && player.getHealth() == 20,
                    "rejected orb damage mints no LP " + i);
        }
        var creative = player(level, "S5CreativeOrb");
        clear(networks, creative.getUUID());
        creative.setGameMode(GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get()));
        use(creative, InteractionHand.MAIN_HAND);
        check(creative.getHealth() == 20 && networks.get(creative.getUUID()).currentEssence() == 200,
                "creative orb use charges without health cost");
        var spectator = player(level, "S5Spectator");
        spectator.setGameMode(GameType.SPECTATOR);
        var unbound = new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get());
        spectator.setItemInHand(InteractionHand.MAIN_HAND, unbound);
        use(spectator, InteractionHand.MAIN_HAND);
        check(ItemBindable.binding(unbound) == null, "spectator cannot bind orb");
        var noTier = new ItemStack(BloodMagicItems.MASTER_BLOOD_ORB.get());
        noTier.remove(BloodMagicDataComponents.BLOOD_ORB_TIER.get());
        check(BloodOrbData.getTier(noTier) == 4, "removed tier component falls back to registered item tier");
        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get()));
        check(fake.getMainHandItem().getItem().use(level, fake, InteractionHand.MAIN_HAND) == InteractionResult.FAIL
                && ItemBindable.binding(fake.getMainHandItem()) == null, "fake players cannot bind or sacrifice with orbs");
    }

    private static void testSigils(ServerLevel level) {
        var player = player(level, "S5Sigil");
        var networks = SoulNetworks.get(level);
        clear(networks, player.getUUID());
        networks.fill(player.getUUID(), 500, 5000);
        var air = new ItemStack(BloodMagicItems.AIR_SIGIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, air);
        use(player, InteractionHand.MAIN_HAND);
        check(ItemBindable.binding(air).ownerId().equals(player.getUUID()), "air sigil binds on wire use");
        check(networks.get(player.getUUID()).currentEssence() == 450 && player.getHealth() == 20,
                "air sigil consumes exact LP without hurting user");
        check(Math.abs(player.getDeltaMovement().length() - 1.7) < 0.00001 && player.fallDistance == 0,
                "air impulse originates on server");
        var motion = MenuTestPlayer.connection(player).packets.stream().filter(ClientboundSetEntityMotionPacket.class::isInstance)
                .map(ClientboundSetEntityMotionPacket.class::cast).findFirst().orElseThrow();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientboundSetEntityMotionPacket.STREAM_CODEC.encode(buffer, motion);
            var decoded = ClientboundSetEntityMotionPacket.STREAM_CODEC.decode(buffer);
            check(decoded.id() == player.getId() && !buffer.isReadable(), "air movement packet wire round trip");
        } finally { buffer.release(); }
        clear(networks, player.getUUID());
        networks.fill(player.getUUID(), 25, 5000);
        use(player, InteractionHand.MAIN_HAND);
        check(networks.get(player.getUUID()).currentEssence() == 25 && player.getHealth() == 19,
                "insufficient LP charges full rounded health cost, preserves partial LP");
        use(player, InteractionHand.MAIN_HAND);
        check(player.getHealth() == 18, "repeated sacrifice bypasses hurt cooldown without free uses");
        player.setHealth(1);
        player.setDeltaMovement(Vec3.ZERO);
        use(player, InteractionHand.MAIN_HAND);
        check(player.getHealth() == 1 && player.getDeltaMovement().equals(Vec3.ZERO)
                && networks.get(player.getUUID()).currentEssence() == 25, "unpayable sigil has no movement or LP loss");
        player.setHealth(20);
        player.getAbilities().invulnerable = true;
        use(player, InteractionHand.MAIN_HAND);
        check(player.getDeltaMovement().equals(Vec3.ZERO), "rejected damage cannot activate sigil");
        player.getAbilities().invulnerable = false;
        player.setGameMode(GameType.CREATIVE);
        use(player, InteractionHand.MAIN_HAND);
        check(networks.get(player.getUUID()).currentEssence() == 25 && player.getHealth() == 20
                && player.getDeltaMovement().length() > 1, "creative sigil has effect without cost");
        player.setGameMode(GameType.SURVIVAL);
        var owner = player(level, "S5SigilOwner");
        clear(networks, owner.getUUID());
        networks.fill(owner.getUUID(), 1000, 5000);
        air.set(BloodMagicDataComponents.BINDING.get(), new Binding(owner.getUUID(), "offline owner"));
        use(player, InteractionHand.MAIN_HAND);
        check(networks.get(owner.getUUID()).currentEssence() == 950 && networks.get(player.getUUID()).currentEssence() == 25,
                "sigil charges bound UUID even when owner is offline");
        check(ItemStack.matches(air, roundTripItem(level, air)), "sigil binding component stream round trip");
        var binding = ItemBindable.binding(air);
        check(!SoulNetworkPayment.perform(level, player, binding, 50, () -> false)
                && networks.get(owner.getUUID()).currentEssence() == 950, "failed effect refunds LP debit");
        clear(networks, owner.getUUID());
        check(!SoulNetworkPayment.perform(level, player, binding, 50, () -> false) && player.getHealth() == 20,
                "failed effect refunds health payment");
        for (var reader : List.of(BloodMagicItems.DIVINATION_SIGIL.get(), BloodMagicItems.SEER_SIGIL.get())) {
            ItemStack stack = new ItemStack(reader);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setXRot(-90);
            MenuTestPlayer.connection(player).packets.clear();
            use(player, InteractionHand.MAIN_HAND);
            check(ItemBindable.binding(stack) != null && MenuTestPlayer.connection(player).packets.stream()
                    .anyMatch(ClientboundSystemChatPacket.class::isInstance), "reader binds and reports network");
            var altar = place(level);
            altar.fillMainTank(1234);
            player.setPos(ALTAR.getX() + 0.5, ALTAR.getY() + 1, ALTAR.getZ() + 0.5);
            var hit = new BlockHitResult(Vec3.atCenterOf(ALTAR), Direction.UP, ALTAR, false);
            int beforePackets = MenuTestPlayer.connection(player).packets.size();
            useOn(player, hit);
            check(altar.isEmpty() && altar.getAltarState().blood() == 1234
                    && MenuTestPlayer.connection(player).packets.subList(beforePackets, MenuTestPlayer.connection(player).packets.size())
                    .stream().anyMatch(ClientboundSystemChatPacket.class::isInstance),
                    "wire block use reads altar before insertion and is free");
        }
    }

    private static void testFluids(ServerLevel level) {
        var player = player(level, "S5Fluid");
        var networks = SoulNetworks.get(level);
        clear(networks, player.getUUID());
        networks.fill(player.getUUID(), 5000, 5000);
        level.getChunk(FLUID);
        level.setBlockAndUpdate(FLUID.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(FLUID, Blocks.AIR.defaultBlockState());
        player.setPos(FLUID.getX() + 0.5, FLUID.getY() + 1, FLUID.getZ() + 0.5);
        player.setXRot(90);
        var water = new ItemStack(BloodMagicItems.WATER_SIGIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, water);
        use(player, InteractionHand.MAIN_HAND);
        check(level.getFluidState(FLUID).isSource() && level.getFluidState(FLUID).getType() == Fluids.WATER
                && networks.get(player.getUUID()).currentEssence() == 4900, "water source costs 100 LP");
        use(player, InteractionHand.MAIN_HAND);
        check(networks.get(player.getUUID()).currentEssence() == 4900, "existing source does not double-charge");
        var empty = new ItemStack(BloodMagicItems.VOID_SIGIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        use(player, InteractionHand.MAIN_HAND);
        check(level.getFluidState(FLUID).isEmpty() && networks.get(player.getUUID()).currentEssence() == 4850,
                "void removes source for 50 LP");
        use(player, InteractionHand.MAIN_HAND);
        check(networks.get(player.getUUID()).currentEssence() == 4850, "void on dry block costs nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BloodMagicItems.LAVA_SIGIL.get()));
        use(player, InteractionHand.MAIN_HAND);
        check(level.getFluidState(FLUID).getType() == Fluids.LAVA && networks.get(player.getUUID()).currentEssence() == 3850,
                "lava source costs 1000 LP");
        level.setBlockAndUpdate(FLUID, Blocks.OAK_SLAB.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, water);
        use(player, InteractionHand.MAIN_HAND);
        check(level.getBlockState(FLUID).getValue(BlockStateProperties.WATERLOGGED)
                && networks.get(player.getUUID()).currentEssence() == 3750, "waterlogging costs once");
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        use(player, InteractionHand.MAIN_HAND);
        check(!level.getBlockState(FLUID).getValue(BlockStateProperties.WATERLOGGED)
                && networks.get(player.getUUID()).currentEssence() == 3700, "void drains waterlogged block without destroying it");
        player.setXRot(-90);
        player.setItemInHand(InteractionHand.MAIN_HAND, water);
        use(player, InteractionHand.MAIN_HAND);
        check(networks.get(player.getUUID()).currentEssence() == 3700, "missed fluid use costs nothing");
        player.setXRot(90);
        player.setGameMode(GameType.ADVENTURE);
        use(player, InteractionHand.MAIN_HAND);
        check(networks.get(player.getUUID()).currentEssence() == 3700
                && !level.getBlockState(FLUID).getValue(BlockStateProperties.WATERLOGGED), "adventure permissions respected");
        player.setGameMode(GameType.SURVIVAL);
        clear(networks, player.getUUID());
        player.setHealth(1);
        use(player, InteractionHand.MAIN_HAND);
        check(!level.getBlockState(FLUID).getValue(BlockStateProperties.WATERLOGGED) && player.getHealth() == 1,
                "unpaid fluid operation cannot place water");
        player.setHealth(20);
        networks.fill(player.getUUID(), 500, 5000);
        var hit = new BlockHitResult(Vec3.atCenterOf(FLUID), Direction.UP, FLUID, false);
        useOn(player, hit);
        check(level.getBlockState(FLUID).getValue(BlockStateProperties.WATERLOGGED)
                && networks.get(player.getUUID()).currentEssence() == 400, "wire block-use waterlogs once, without duplicate charge");
        level.setBlockAndUpdate(FLUID, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(FLUID.below(), Blocks.AIR.defaultBlockState());
    }

    private static void testAltar(ServerLevel level) {
        var networks = SoulNetworks.get(level);
        for (int tier = 1; tier <= 5; tier++) {
            var owner = UUID.nameUUIDFromBytes(("S5Altar" + tier).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            clear(networks, owner);
            var altar = place(level);
            var orb = boundOrb(tier, owner);
            altar.setStoredItem(orb);
            altar.fillMainTank(100);
            tick(altar);
            var definition = BloodMagicBloodOrbs.forTier(tier);
            check(networks.get(owner).currentEssence() == definition.fillRate()
                    && altar.getAltarState().blood() == 100 - definition.fillRate(), "altar rate and conservation " + tier);
            check(ItemStack.matches(orb, altar.getStoredItem()), "charging retains orb " + tier);
            networks.fill(owner, Integer.MAX_VALUE, definition.capacity());
            int before = altar.getAltarState().blood();
            tick(altar);
            check(altar.getAltarState().blood() == before, "full network stops tank drain " + tier);
            networks.consume(owner, 1);
            tick(altar);
            check(altar.getAltarState().blood() == before - 1 && networks.get(owner).currentEssence() == definition.capacity(),
                    "partial capacity accepts only one LP " + tier);
            clear(networks, owner);
            altar.drainMainTank(Integer.MAX_VALUE);
            tick(altar);
            check(networks.get(owner).currentEssence() == 0, "empty altar cannot create LP " + tier);
        }
        var altar = place(level);
        altar.fillMainTank(500);
        altar.setStoredItem(new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get()));
        tick(altar);
        check(altar.getAltarState().blood() == 500 && ItemBindable.binding(altar.getStoredItem()) == null,
                "altar does not bind or drain an unbound orb");
        var owner = UUID.randomUUID();
        var weak = boundOrb(1, owner);
        networks.fill(owner, 10_000, 25_000);
        altar.setStoredItem(weak);
        tick(altar);
        check(networks.get(owner).currentEssence() == 10_000 && altar.getAltarState().blood() == 500,
                "smaller orb cannot truncate a larger network");
        var upgrades = new AltarUpgrades(Map.of(BloodRuneType.ORB, 10, BloodRuneType.SPEED, 5));
        check(upgrades.orbCapacity(5000) == 6000 && upgrades.consumption(50) == 100,
                "orb-capacity and speed rune arithmetic");
        // Exercise the real structure scan -> upgrades -> charging path, not only formula helpers.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x != 0 || z != 0) level.setBlockAndUpdate(ALTAR.offset(x, -1, z), BloodMagicBlocks.BLANK_RUNE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(ALTAR.offset(0, -1, -1), BloodMagicBlocks.ORB_RUNE.get().defaultBlockState());
        level.setBlockAndUpdate(ALTAR.offset(0, -1, 1), BloodMagicBlocks.SPEED_RUNE_2.get().defaultBlockState());
        altar.refreshStructure();
        clear(networks, owner);
        networks.fill(owner, 5000, 5000);
        tick(altar);
        check(networks.get(owner).currentEssence() == 5002, "actual orb rune raises weak capacity above 5000");
        var magician = boundOrb(3, owner);
        altar.setStoredItem(magician);
        int before = altar.getAltarState().blood();
        tick(altar);
        check(before - altar.getAltarState().blood() == 21, "actual speed rune accelerates orb fill rate");
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x != 0 || z != 0) level.setBlockAndUpdate(ALTAR.offset(x, -1, z), Blocks.AIR.defaultBlockState());
        }
        altar.clearContent();
    }

    private static ServerPlayer player(ServerLevel level, String name) {
        level.getChunk(ALTAR);
        var player = MenuTestPlayer.create(level, name);
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(ALTAR.getX() + 0.5, ALTAR.getY() + 2, ALTAR.getZ() + 0.5);
        player.setHealth(20);
        return player;
    }

    private static void use(ServerPlayer player, InteractionHand hand) {
        var packet = new ServerboundUseItemPacket(hand, ++sequence, player.getYRot(), player.getXRot());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ServerboundUseItemPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = ServerboundUseItemPacket.STREAM_CODEC.decode(buffer);
            player.connection.handleUseItem(decoded);
            if (buffer.isReadable()) throw new IllegalStateException("use packet codec left unread data");
        } finally { buffer.release(); }
    }

    private static void useOn(ServerPlayer player, BlockHitResult hit) {
        var packet = new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, ++sequence);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ServerboundUseItemOnPacket.STREAM_CODEC.encode(buffer, packet);
            player.connection.handleUseItemOn(ServerboundUseItemOnPacket.STREAM_CODEC.decode(buffer));
            if (buffer.isReadable()) throw new IllegalStateException("block use packet codec left unread data");
        } finally { buffer.release(); }
    }

    private static ItemStack roundTripItem(ServerLevel level, ItemStack stack) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            return ItemStack.STREAM_CODEC.decode(buffer);
        } finally { buffer.release(); }
    }

    private static ItemStack boundOrb(int tier, UUID owner) {
        var items = List.of(BloodMagicItems.WEAK_BLOOD_ORB, BloodMagicItems.APPRENTICE_BLOOD_ORB,
                BloodMagicItems.MAGICIAN_BLOOD_ORB, BloodMagicItems.MASTER_BLOOD_ORB, BloodMagicItems.ARCHMAGE_BLOOD_ORB);
        var stack = new ItemStack(items.get(tier - 1).get());
        BloodOrbData.bind(stack, owner, "OfflineTester");
        return stack;
    }

    private static BloodAltarBlockEntity place(ServerLevel level) {
        level.getChunk(ALTAR);
        if (level.getBlockEntity(ALTAR) instanceof BloodAltarBlockEntity old) old.clearContent();
        level.setBlockAndUpdate(ALTAR, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(ALTAR, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
        return (BloodAltarBlockEntity) level.getBlockEntity(ALTAR);
    }

    private static void tick(BloodAltarBlockEntity altar) {
        BloodAltarBlockEntity.serverTick(altar.getLevel(), altar.getBlockPos(), altar.getBlockState(), altar);
    }

    private static void clear(SoulNetworks networks, UUID owner) { networks.consume(owner, networks.get(owner).currentEssence()); }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE5_SMOKE FAIL: " + message);
        assertions++;
    }
}

