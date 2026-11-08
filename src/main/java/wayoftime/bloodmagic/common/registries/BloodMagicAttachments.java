package wayoftime.bloodmagic.common.registries;

import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.will.DemonWillHolder;

/** Replaces upstream WorldDemonWillHandler's ChunkDataEvent "BloodMagic" tag with a chunk attachment. */
public final class BloodMagicAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BloodMagic.MODID);
    public static final Supplier<AttachmentType<DemonWillHolder>> CHUNK_WILL = ATTACHMENTS.register("chunk_will",
            () -> AttachmentType.builder(DemonWillHolder::new).serialize(DemonWillHolder.CODEC.fieldOf("current"), h -> !h.isEmpty()).build());
    private BloodMagicAttachments() {}
}
