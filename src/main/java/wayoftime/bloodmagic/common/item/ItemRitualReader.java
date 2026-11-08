package wayoftime.bloodmagic.common.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.block.entity.MasterRitualBlockEntity;
import wayoftime.bloodmagic.common.registries.BloodMagicDataComponents;
import wayoftime.bloodmagic.ritual.EnumReaderBoundaries;
import wayoftime.bloodmagic.ritual.RitualRangeSpec;
import wayoftime.bloodmagic.will.DemonWillHolder;

/** Upstream Ritual Tinkerer: ritual information, area editing and Demon Will configuration. */
public final class ItemRitualReader extends Item {
    private static final String BASE = "tooltip.bloodmagic.ritualReader.";

    public ItemRitualReader(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isAlive() || player.isSpectator()) return InteractionResult.FAIL;
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()) cycleMode(stack, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || !player.isAlive() || player.isSpectator() || !player.mayBuild()
                || !player.isWithinBlockInteractionRange(pos, 0) || !level.mayInteract(player, pos))
            return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        if (level.isClientSide()) return mode(stack) == Mode.SET_AREA ? InteractionResult.SUCCESS : InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof MasterRitualBlockEntity master) {
            stack.set(BloodMagicDataComponents.RITUAL_READER_MASTER.get(), pos);
            stack.remove(BloodMagicDataComponents.RITUAL_READER_FIRST.get());
            switch (mode(stack)) {
                case INFORMATION -> master.provideRitualInformation(player);
                case SET_AREA -> {
                    if (player.isShiftKeyDown() && player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ItemBloodOrb) {
                        master.resetRitualRanges();
                        player.sendSystemMessage(Component.translatable("ritual.bloodmagic.blockRange.success"));
                    } else {
                        String range = nextRange(master, stack);
                        master.provideRangeInformation(player, range);
                    }
                }
                case SET_WILL_TYPES -> {
                    List<EnumDemonWillType> types = new ArrayList<>();
                    for (int slot = 0; slot < 9; slot++) {
                        ItemStack hotbar = player.getInventory().getItem(slot);
                        if (hotbar.getItem() instanceof ItemDemonCrystal crystal && !types.contains(crystal.type()))
                            types.add(crystal.type());
                    }
                    master.setActiveWillConfig(player, types);
                    master.provideWillInformation(player);
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        if (mode(stack) != Mode.SET_AREA) return InteractionResult.PASS;
        BlockPos masterPos = stack.get(BloodMagicDataComponents.RITUAL_READER_MASTER.get());
        if (masterPos == null || !(level.getBlockEntity(masterPos) instanceof MasterRitualBlockEntity master))
            return InteractionResult.FAIL;
        String range = nextRange(master, stack);
        BlockPos first = stack.get(BloodMagicDataComponents.RITUAL_READER_FIRST.get());
        if (first == null) {
            stack.set(BloodMagicDataComponents.RITUAL_READER_FIRST.get(), pos.subtract(masterPos));
            player.sendSystemMessage(Component.translatable("ritual.bloodmagic.blockRange.firstBlock"));
            return InteractionResult.SUCCESS_SERVER;
        }

        EnumReaderBoundaries result = master.setBlockRangeByBounds(player, range, first, pos.subtract(masterPos));
        switch (result) {
            case SUCCESS -> player.sendSystemMessage(Component.translatable("ritual.bloodmagic.blockRange.success"));
            case NOT_WITHIN_BOUNDARIES -> player.sendSystemMessage(Component.translatable(
                    "ritual.bloodmagic.blockRange.tooFar", master.maxVerticalForRange(range), master.maxHorizontalForRange(range)));
            case VOLUME_TOO_LARGE -> player.sendSystemMessage(Component.translatable(
                    "ritual.bloodmagic.blockRange.tooBig", master.maxVolumeForRange(range)));
            case UNKNOWN_RANGE -> player.sendSystemMessage(Component.translatable("ritual.bloodmagic.blockRange.noRange"));
        }
        stack.remove(BloodMagicDataComponents.RITUAL_READER_FIRST.get());
        return InteractionResult.SUCCESS_SERVER;
    }

    private static String nextRange(MasterRitualBlockEntity master, ItemStack stack) {
        String current = stack.get(BloodMagicDataComponents.RITUAL_READER_RANGE.get());
        List<RitualRangeSpec> specs = master.ritual().rangeSpecs();
        if (specs.isEmpty()) return "";
        int index = 0;
        for (int i = 0; i < specs.size(); i++) if (specs.get(i).name().equals(current)) { index = (i + 1) % specs.size(); break; }
        String next = specs.get(index).name();
        stack.set(BloodMagicDataComponents.RITUAL_READER_RANGE.get(), next);
        return next;
    }

    private static Mode mode(ItemStack stack) {
        return Mode.values()[Math.floorMod(stack.getOrDefault(BloodMagicDataComponents.RITUAL_READER_STATE.get(), 0), Mode.values().length)];
    }

    private static void cycleMode(ItemStack stack, Player player) {
        Mode next = Mode.values()[(mode(stack).ordinal() + 1) % Mode.values().length];
        stack.set(BloodMagicDataComponents.RITUAL_READER_STATE.get(), next.ordinal());
        player.sendSystemMessage(Component.translatable(BASE + "currentState",
                Component.translatable(BASE + next.name().toLowerCase(java.util.Locale.ROOT))));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Mode mode = mode(stack);
        tooltip.accept(Component.translatable(BASE + "currentState",
                Component.translatable(BASE + mode.name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(BASE + "desc." + mode.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
    }

    private enum Mode {
        INFORMATION,
        SET_AREA,
        SET_WILL_TYPES
    }
}

