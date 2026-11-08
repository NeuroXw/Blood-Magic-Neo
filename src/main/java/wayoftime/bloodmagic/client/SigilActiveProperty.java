package wayoftime.bloodmagic.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.item.sigil.ItemSigilToggleableBase;

/** Item model property selected by the persistent sigil_activated component. */
public final class SigilActiveProperty implements SelectItemModelProperty<Boolean> {
    public static final SigilActiveProperty INSTANCE = new SigilActiveProperty();
    public static final Type<SigilActiveProperty, Boolean> TYPE = Type.create(MapCodec.unit(INSTANCE), Codec.BOOL);
    private SigilActiveProperty() {}
    @Override public Boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
        return ItemSigilToggleableBase.activated(stack);
    }
    @Override public Codec<Boolean> valueCodec() { return Codec.BOOL; }
    @Override public Type<? extends SelectItemModelProperty<Boolean>, Boolean> type() { return TYPE; }
}
