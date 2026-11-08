package wayoftime.bloodmagic.common.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;

/** Complete upstream sound-event registry transferred with its manifests and OGG assets. */
public final class BloodMagicSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, BloodMagic.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> BLEEDING_EDGE = SOUNDS.register("bleedingedge",
            () -> SoundEvent.createVariableRangeEvent(BloodMagic.rl("bleedingedge")));
    private BloodMagicSounds() {}
}
