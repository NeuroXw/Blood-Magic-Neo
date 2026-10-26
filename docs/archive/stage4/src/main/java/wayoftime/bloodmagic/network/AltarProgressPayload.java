package wayoftime.bloodmagic.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.altar.AltarState;

/** Server-authored numeric snapshot; inventory travels through vanilla BE update tags. */
public record AltarProgressPayload(ResourceKey<Level> dimension, BlockPos pos, AltarState state)
        implements CustomPacketPayload {
    public static final Type<AltarProgressPayload> TYPE = new Type<>(BloodMagic.rl("altar_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AltarProgressPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.DIMENSION), AltarProgressPayload::dimension,
                    BlockPos.STREAM_CODEC, AltarProgressPayload::pos,
                    AltarState.STREAM_CODEC, AltarProgressPayload::state,
                    AltarProgressPayload::new);

    public AltarProgressPayload {
        pos = pos.immutable();
    }

    @Override
    public Type<AltarProgressPayload> type() {
        return TYPE;
    }
}
