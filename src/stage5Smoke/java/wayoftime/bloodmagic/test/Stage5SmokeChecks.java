package wayoftime.bloodmagic.test;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.connection.ConnectionType;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.altar.AltarState;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;
import wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.BloodOrbData;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.network.AltarProgressPayload;
import wayoftime.bloodmagic.network.BloodMagicNetwork;

/** Opt-in, dedicated-server integration checks. Not packaged in the mod JAR. */
@EventBusSubscriber(modid = BloodMagic.MODID)
public final class Stage5SmokeChecks {
    private static final BlockPos POS = new BlockPos(0, 100, 0);
    private static final Binding OWNER = new Binding(UUID.fromString("12345678-1234-5678-9abc-123456789abc"), "Stage2Tester");
    private static final AltarState SAMPLE = new AltarState(4321, 10_000, 123, 456, 1_000, 1, 0, 0, false);
    private static int assertions;
    private static int ticks;
    private static boolean started;
    private static boolean restarted;
    private static boolean dropsVerified;

    private Stage5SmokeChecks() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) throws IOException {
        ServerLevel level = event.getServer().overworld();
        check(Path.of("").toAbsolutePath().normalize().endsWith(Path.of("run", "stage5-smoke")), "isolated game directory");
        Files.deleteIfExists(Path.of("stage5-smoke-result.txt"));
        // This historical persistence fixture now contains a functional bound orb. Saturate its
        // network so the same 174 baseline assertions still test idle storage, not charging.
        wayoftime.bloodmagic.core.data.SoulNetworks.get(level).fill(OWNER.ownerId(), Integer.MAX_VALUE, 150_000);
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BloodMagicBlockEntities.BLOOD_ALTAR.get())
                .equals(BloodMagic.rl("altar")), "registered BE type");
        check(NetworkRegistry.getCodec(AltarProgressPayload.TYPE.id(), ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND)
                != null, "registered clientbound play payload");
        check(!NetworkRegistry.getCommonPlayChannels(PacketFlow.SERVERBOUND).contains(AltarProgressPayload.TYPE.id()),
                "no serverbound altar mutation channel");

        var orbs = List.of(BloodMagicItems.WEAK_BLOOD_ORB, BloodMagicItems.APPRENTICE_BLOOD_ORB,
                BloodMagicItems.MAGICIAN_BLOOD_ORB, BloodMagicItems.MASTER_BLOOD_ORB, BloodMagicItems.ARCHMAGE_BLOOD_ORB);
        for (int index = 0; index < orbs.size(); index++) {
            ItemStack orb = new ItemStack(orbs.get(index).get());
            check(BloodOrbData.getTier(orb) == index + 1 && orb.getMaxStackSize() == 1, "orb defaults " + index);
            check(BloodOrbData.getBinding(orb).isEmpty(), "orb starts unbound " + index);
        }
        ItemStack stack = sampleOrb();
        ItemStack copy = stack.copy();
        BloodOrbData.unbind(copy);
        BloodOrbData.setTier(copy, 5);
        check(BloodOrbData.getBinding(stack).orElseThrow().equals(OWNER) && BloodOrbData.getTier(stack) == 3,
                "components copied without mutation alias");
        check(BloodOrbData.getBinding(copy).isEmpty() && BloodOrbData.getTier(copy) == 5, "component remove and replace");
        boolean rejected = false;
        try {
            BloodOrbData.setTier(stack, 6);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "invalid orb tier rejected");

        BloodAltarBlockEntity altar = newAltar();
        check(altar.getAltarState().equals(AltarState.EMPTY) && altar.getStoredItem().isEmpty(), "new altar defaults");
        check(altar.fillMainTank(Integer.MAX_VALUE) == 10_000, "fill clamps without integer overflow");
        check(altar.fillMainTank(-1) == 0 && altar.fillMainTank(1) == 0, "negative and full fill");
        check(altar.drainMainTank(-1) == 0 && altar.drainMainTank(Integer.MAX_VALUE) == 10_000, "drain clamps");
        altar.setAltarState(SAMPLE);
        altar.setStoredItem(stack);
        BloodOrbData.unbind(stack);
        BloodOrbData.unbind(altar.getStoredItem());
        check(BloodOrbData.getBinding(altar.getStoredItem()).orElseThrow().equals(OWNER), "inventory defensive copies");

        CompoundTag tag = altar.saveWithFullMetadata(level.registryAccess());
        Path fixture = Path.of("stage5-altar-roundtrip.nbt");
        NbtIo.writeCompressed(tag, fixture);
        BlockEntity restored = BlockEntity.loadStatic(POS, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState(),
                NbtIo.readCompressed(fixture, NbtAccounter.unlimitedHeap()), level.registryAccess());
        check(restored instanceof BloodAltarBlockEntity, "compressed NBT type dispatch");
        checkAltar((BloodAltarBlockEntity) restored, "compressed NBT round trip");

        var problems = new ProblemReporter.Collector();
        BloodAltarBlockEntity trackingCopy = newAltar();
        trackingCopy.handleUpdateTag(TagValueInput.create(problems, level.registryAccess(), altar.getUpdateTag(level.registryAccess())));
        checkAltar(trackingCopy, "initial tracking update tag");
        trackingCopy.loadCustomOnly(TagValueInput.create(problems, level.registryAccess(), new CompoundTag()));
        check(trackingCopy.getAltarState().equals(AltarState.EMPTY) && trackingCopy.getStoredItem().isEmpty(), "missing fields reset to defaults");
        CompoundTag invalidState = new CompoundTag();
        invalidState.putInt("capacity", -1);
        invalidState.putInt("blood", Integer.MAX_VALUE);
        invalidState.putInt("input_blood", -1);
        invalidState.putInt("output_blood", Integer.MAX_VALUE);
        invalidState.putInt("tier", 99);
        invalidState.putInt("progress", Integer.MAX_VALUE);
        invalidState.putInt("required_blood", 500);
        CompoundTag invalidTag = new CompoundTag();
        invalidTag.put("altar", invalidState);
        trackingCopy.loadCustomOnly(TagValueInput.create(problems, level.registryAccess(), invalidTag));
        check(trackingCopy.getAltarState().equals(new AltarState(10_000, 10_000, 0, 1_000, 1_000, 6, 500, 500, false)),
                "out of range save data normalized");
        check(problems.isEmpty(), "registry-aware serialization has no reported problems: " + problems.getReport());

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            var payload = new AltarProgressPayload(level.dimension(), POS, SAMPLE);
            AltarProgressPayload.STREAM_CODEC.encode(buffer, payload);
            check(payload.equals(AltarProgressPayload.STREAM_CODEC.decode(buffer)) && !buffer.isReadable(), "payload stream round trip");
            buffer.clear();
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, sampleOrb());
            ItemStack decoded = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            checkOrb(decoded, "item component network round trip");
            check(!buffer.isReadable(), "item stream consumed fully");
            buffer.clear();
            BloodMagicDataComponents.BLOOD_ORB_TIER.get().streamCodec().encode(buffer, 4);
            check(BloodMagicDataComponents.BLOOD_ORB_TIER.get().streamCodec().decode(buffer) == 4, "tier component stream");
        } finally {
            buffer.release();
        }

        level.getChunk(POS);
        if (level.getBlockState(POS).is(BloodMagicBlocks.BLOOD_ALTAR.get())) {
            checkAltar((BloodAltarBlockEntity) level.getBlockEntity(POS), "world restart persistence");
            restarted = true;
        } else {
            check(level.getBlockState(POS).isAir(), "fixture location is empty");
            level.setBlockAndUpdate(POS, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
            check(level.getBlockEntity(POS) instanceof BloodAltarBlockEntity, "placement creates registered block entity");
            BloodAltarBlockEntity placed = (BloodAltarBlockEntity) level.getBlockEntity(POS);
            placed.setAltarState(SAMPLE);
            placed.setStoredItem(sampleOrb());
        }
        BloodAltarBlockEntity placed = (BloodAltarBlockEntity) level.getBlockEntity(POS);
        check(placed.getUpdatePacket() != null, "vanilla update packet with attached registry context");
        BloodMagicNetwork.applySnapshot(level, new AltarProgressPayload(level.dimension(), POS, AltarState.EMPTY));
        checkAltar(placed, "client handler cannot mutate server world");
        check(BloodMagicBlocks.BLOOD_ALTAR.get().getTicker(level, placed.getBlockState(), placed.getType()) != null,
                "server ticker connected");
        level.getChunkSource().updateChunkForced(ChunkPos.containing(POS), true);
        AltarGameplayChecks.begin(level);
        assertions += AltarMenuChecks.run(level);
        assertions += OrbSigilChecks.begin(level);
        started = true;
    }

    private static void testItemDrop(ServerLevel level) {
        BlockPos dropPos = POS.offset(3, 0, 0);
        check(level.getBlockState(dropPos).isAir(), "drop fixture location is empty");
        var area = new AABB(2, 99, -1, 5, 102, 2);
        // A failed prior run may have saved old item entities in this isolated fixture area.
        var previousDrops = level.getEntitiesOfClass(ItemEntity.class, area);
        level.setBlockAndUpdate(dropPos, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
        ((BloodAltarBlockEntity) level.getBlockEntity(dropPos)).setStoredItem(sampleOrb());
        level.setBlockAndUpdate(dropPos, Blocks.AIR.defaultBlockState());
        var drops = level.getEntitiesOfClass(ItemEntity.class, area).stream()
                .filter(entity -> !previousDrops.contains(entity)).toList();
        check(drops.size() == 1, "altar removal drops inventory once: " + drops.size() + " new, " + previousDrops.size() + " old");
        checkOrb(drops.getFirst().getItem(), "dropped orb retains components");
        drops.forEach(ItemEntity::discard);
        previousDrops.forEach(ItemEntity::discard);
    }

    @SubscribeEvent
    public static void afterTick(ServerTickEvent.Post event) throws IOException {
        if (!started || ++ticks < 10) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        if (!dropsVerified) {
            testItemDrop(level); // Loaded item-entity sections become visible after the initial world ticks.
            dropsVerified = true;
        }
        if (!AltarReloadChecks.afterTick(level)) return;
        checkAltar((BloodAltarBlockEntity) level.getBlockEntity(POS), "idle altar retains storage after 10 ticks");
        assertions += AltarGameplayChecks.finish(level);
        assertions += AltarReloadChecks.assertions();
        assertions += OrbSigilChecks.finish(level);
        event.getServer().saveEverything(false, true, true);
        String result = "STAGE5_SMOKE PASS: " + assertions + " assertions; "
                + (restarted && AltarGameplayChecks.resumed() ? "RESTART persistence verified" : "WRITE persistence fixture created");
        Files.writeString(Path.of("stage5-smoke-result.txt"), result + System.lineSeparator());
        BloodMagic.LOGGER.info(result);
        started = false;
        event.getServer().halt(false);
    }

    private static BloodAltarBlockEntity newAltar() {
        return BloodMagicBlockEntities.BLOOD_ALTAR.get().create(POS, BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
    }

    private static ItemStack sampleOrb() {
        ItemStack orb = new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get());
        BloodOrbData.setTier(orb, 3);
        BloodOrbData.bind(orb, OWNER.ownerId(), OWNER.ownerName());
        return orb;
    }

    private static void checkAltar(BloodAltarBlockEntity altar, String message) {
        check(altar != null && altar.getAltarState().equals(SAMPLE), message + " state");
        checkOrb(altar.getStoredItem(), message + " inventory");
    }

    private static void checkOrb(ItemStack stack, String message) {
        check(stack.is(BloodMagicItems.WEAK_BLOOD_ORB.get()) && stack.getCount() == 1
                && BloodOrbData.getTier(stack) == 3 && BloodOrbData.getBinding(stack).orElseThrow().equals(OWNER), message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("STAGE5_SMOKE FAIL: " + message);
        }
        assertions++;
    }
}

