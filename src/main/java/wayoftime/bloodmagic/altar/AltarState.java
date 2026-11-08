package wayoftime.bloodmagic.altar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Immutable storage/sync snapshot. Progress and required blood are LP, not ticks. */
public record AltarState(int blood, int capacity, int inputBlood, int outputBlood,
                         int bufferCapacity, int tier, int progress, int requiredBlood, boolean active) {
    public static final int BASE_CAPACITY = 10_000;
    public static final int BASE_BUFFER_CAPACITY = 1_000;
    public static final AltarState EMPTY = new AltarState(0, BASE_CAPACITY, 0, 0,
            BASE_BUFFER_CAPACITY, 1, 0, 0, false);

    public static final StreamCodec<FriendlyByteBuf, AltarState> STREAM_CODEC = StreamCodec.of(
            (buffer, state) -> {
                buffer.writeVarInt(state.blood);
                buffer.writeVarInt(state.capacity);
                buffer.writeVarInt(state.inputBlood);
                buffer.writeVarInt(state.outputBlood);
                buffer.writeVarInt(state.bufferCapacity);
                buffer.writeVarInt(state.tier);
                buffer.writeVarInt(state.progress);
                buffer.writeVarInt(state.requiredBlood);
                buffer.writeBoolean(state.active);
            },
            buffer -> new AltarState(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readBoolean()));

    public AltarState {
        capacity = capacity > 0 ? capacity : BASE_CAPACITY;
        bufferCapacity = bufferCapacity > 0 ? bufferCapacity : BASE_BUFFER_CAPACITY;
        blood = Math.clamp(blood, 0, capacity);
        inputBlood = Math.clamp(inputBlood, 0, bufferCapacity);
        outputBlood = Math.clamp(outputBlood, 0, bufferCapacity);
        tier = Math.clamp(tier, 1, 6);
        requiredBlood = Math.max(0, requiredBlood);
        progress = Math.clamp(progress, 0, requiredBlood);
        active = active && requiredBlood > 0;
    }

    public AltarState withBlood(int amount) {
        return new AltarState(amount, capacity, inputBlood, outputBlood, bufferCapacity,
                tier, progress, requiredBlood, active);
    }

    public static AltarState load(ValueInput input) {
        return new AltarState(input.getIntOr("blood", 0), input.getIntOr("capacity", BASE_CAPACITY),
                input.getIntOr("input_blood", 0), input.getIntOr("output_blood", 0),
                input.getIntOr("buffer_capacity", BASE_BUFFER_CAPACITY), input.getIntOr("tier", 1),
                input.getIntOr("progress", 0), input.getIntOr("required_blood", 0),
                input.getBooleanOr("active", false));
    }

    public void save(ValueOutput output) {
        output.putInt("blood", blood);
        output.putInt("capacity", capacity);
        output.putInt("input_blood", inputBlood);
        output.putInt("output_blood", outputBlood);
        output.putInt("buffer_capacity", bufferCapacity);
        output.putInt("tier", tier);
        output.putInt("progress", progress);
        output.putInt("required_blood", requiredBlood);
        output.putBoolean("active", active);
    }
}
