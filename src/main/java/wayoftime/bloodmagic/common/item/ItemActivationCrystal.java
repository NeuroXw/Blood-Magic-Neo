package wayoftime.bloodmagic.common.item;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import wayoftime.bloodmagic.common.block.entity.MasterRitualBlockEntity;

/** Upstream activation crystal tiers. All upstream rituals currently require tier 0. */
public final class ItemActivationCrystal extends ItemBindable {
    private final CrystalType type;

    public ItemActivationCrystal(Properties properties) {
        this(properties, CrystalType.WEAK);
    }

    public ItemActivationCrystal(Properties properties, CrystalType type) {
        super(properties, "activationcrystal");
        this.type = type;
    }

    public int crystalLevel() {
        return type == CrystalType.CREATIVE ? Integer.MAX_VALUE : type.ordinal();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        bindIfAbsent(player, player.getItemInHand(hand));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!canUse(player)) return InteractionResult.FAIL;
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MasterRitualBlockEntity master))
            return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        var binding = bindIfAbsent(player, context.getItemInHand());
        boolean success = player.isShiftKeyDown()
                ? master.stop(player, binding)
                : master.activate(player, binding, crystalLevel());
        player.sendOverlayMessage(Component.translatable(success
                ? "message.bloodmagic.ritual.changed" : "message.bloodmagic.ritual.failed"));
        return success ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    public enum CrystalType {
        WEAK,
        AWAKENED,
        CREATIVE
    }
}
