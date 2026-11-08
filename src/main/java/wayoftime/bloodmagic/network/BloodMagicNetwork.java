package wayoftime.bloodmagic.network;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;

public final class BloodMagicNetwork {
    private BloodMagicNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").executesOn(HandlerThread.MAIN);
        registrar.playToClient(
                AltarProgressPayload.TYPE, AltarProgressPayload.STREAM_CODEC, BloodMagicNetwork::handleAltarProgress);
        registrar.playToClient(
                DemonAuraPayload.TYPE, DemonAuraPayload.STREAM_CODEC,
                (payload, context) -> wayoftime.bloodmagic.will.ClientAura.current = payload.toHolder());
    }

    private static void handleAltarProgress(AltarProgressPayload payload, IPayloadContext context) {
        // MAIN dispatch is explicit above. This handler references no client-only classes.
        applySnapshot(context.player().level(), payload);
    }

    public static void applySnapshot(Level level, AltarProgressPayload payload) {
        if (!level.isClientSide() || !level.dimension().equals(payload.dimension())
                || level.getChunkSource().getChunkNow(payload.pos().getX() >> 4, payload.pos().getZ() >> 4) == null) {
            return;
        }
        if (level.getBlockEntity(payload.pos()) instanceof BloodAltarBlockEntity altar && !altar.isRemoved()) {
            altar.applyClientSnapshot(payload.state());
        }
    }
}
