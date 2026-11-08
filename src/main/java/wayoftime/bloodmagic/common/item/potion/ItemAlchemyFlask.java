package wayoftime.bloodmagic.common.item.potion;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

/** Eight-use alchemy flask using the vanilla POTION_CONTENTS data component. */
public class ItemAlchemyFlask extends Item {
    public ItemAlchemyFlask(Properties properties) { super(properties.stacksTo(1).durability(8)); }
    public int remainingUses(ItemStack stack) { return Math.max(0, stack.getMaxDamage() - stack.getDamageValue()); }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.DRINK; }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return remainingUses(stack) > 0 ? ItemUtils.startUsingInstantly(level, player, hand) : InteractionResult.PASS;
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).applyToLivingEntity(entity, 1.0F);
        if (entity instanceof Player player && !player.hasInfiniteMaterials()) consumeUse(stack);
        return stack;
    }
    protected static void consumeUse(ItemStack stack) {
        stack.set(DataComponents.DAMAGE, Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.bloodmagic.arctool.uses", remainingUses(stack)).withStyle(ChatFormatting.GOLD));
        var contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.hasEffects()) PotionContents.addPotionTooltip(contents.getAllEffects(), tooltip, 1.0F, 20.0F);
    }

    public static final class Throwable extends ItemAlchemyFlask {
        private final boolean lingering;
        public Throwable(Properties properties, boolean lingering) { super(properties); this.lingering = lingering; }
        @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.NONE; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            var contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (remainingUses(stack) <= 0 || !contents.hasEffects()) return InteractionResult.PASS;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                    0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            if (level instanceof ServerLevel server) {
                var projectile = lingering ? new ThrownLingeringPotion(server, player, stack.copyWithCount(1))
                        : new ThrownSplashPotion(server, player, stack.copyWithCount(1));
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
                server.addFreshEntity(projectile);
            }
            if (!player.hasInfiniteMaterials()) consumeUse(stack);
            return InteractionResult.SUCCESS;
        }
    }
}
