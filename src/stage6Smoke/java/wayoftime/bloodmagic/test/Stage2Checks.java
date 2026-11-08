package wayoftime.bloodmagic.test;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemRitualDiviner;
import wayoftime.bloodmagic.common.item.sigil.ItemSigilHolding;
import wayoftime.bloodmagic.common.item.sigil.ItemSigilToggleableBase;
import wayoftime.bloodmagic.core.data.SoulNetworks;
import wayoftime.bloodmagic.ritual.BasicRitual;

/** Stage 2: all upstream ritual geometries, remaining sigils, toggle upkeep and holding carrier. */
final class Stage2Checks {
    private static int assertions;
    private Stage2Checks() {}
    static int run(ServerLevel level) {
        int start = assertions;
        check(BasicRitual.values().length == 32, "stage 2 registers all 32 upstream rituals");
        int largest = 0;
        for (BasicRitual ritual : BasicRitual.values()) {
            var parts = ritual.components();
            check(!parts.isEmpty() && parts.size() == new HashSet<>(parts).size(),
                    "ritual geometry is nonempty and duplicate-free: " + ritual.getSerializedName());
            check(ritual.activationCost >= 0 && ritual.cost >= 0 && ritual.interval >= 1,
                    "ritual costs are nonnegative and interval positive: " + ritual.getSerializedName());
            largest = Math.max(largest, parts.size());
        }
        check(largest >= 60, "large rituals retain their original rune complexity: largest=" + largest);

        var player = MenuTestPlayer.create(level, "Stage2Tester");
        player.setPos(64.5, 100, 64.5);
        var network = SoulNetworks.get(level);
        network.consume(player.getUUID(), network.get(player.getUUID()).currentEssence());
        network.fill(player.getUUID(), 1000, 20000);

        ItemStack grove = new ItemStack(BloodMagicItems.GROWTH_SIGIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, grove);
        player.setShiftKeyDown(true);
        check(grove.getItem().use(level, player, InteractionHand.MAIN_HAND).consumesAction()
                && ItemSigilToggleableBase.activated(grove), "sneak-use toggles the Green Grove sigil on");
        player.setShiftKeyDown(false);
        player.tickCount = 100;
        grove.getItem().inventoryTick(grove, level, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        check(network.get(player.getUUID()).currentEssence() == 700,
                "toggle activation and the next 100-tick upkeep each pay 150 LP");

        ItemStack holding = new ItemStack(BloodMagicItems.HOLDING_SIGIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, holding);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BloodMagicItems.MINING_SIGIL.get()));
        player.setShiftKeyDown(true);
        check(holding.getItem().use(level, player, InteractionHand.MAIN_HAND).consumesAction()
                && ItemSigilHolding.contents(holding).get(0).is(BloodMagicItems.MINING_SIGIL.get()),
                "holding sigil inserts the offhand sigil into its first slot");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        int before = ItemSigilHolding.selected(holding);
        holding.getItem().use(level, player, InteractionHand.MAIN_HAND);
        check(ItemSigilHolding.selected(holding) == (before + 1) % ItemSigilHolding.SLOTS,
                "holding sigil cycles its selected slot");
        player.setShiftKeyDown(false);

        check(BloodMagicItems.MINING_SIGIL.get() != null, "mining sigil registration");
        BlockPos anchor = new BlockPos(70, 100, 70);
        level.setBlockAndUpdate(anchor, Blocks.STONE.defaultBlockState());
        var teleport = new ItemStack(BloodMagicItems.TELEPOSITION_SIGIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, teleport);
        player.setShiftKeyDown(true);
        var context = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(anchor),
                        net.minecraft.core.Direction.UP, anchor, false));
        check(teleport.getItem().useOn(context).consumesAction()
                && anchor.equals(teleport.get(wayoftime.bloodmagic.common.registries.BloodMagicDataComponents.TELEPOS_POS.get())),
                "teleposition sigil binds a block anchor");
        player.setShiftKeyDown(false);
        level.setBlockAndUpdate(anchor, Blocks.AIR.defaultBlockState());

        BlockPos craftPos = new BlockPos(80, 100, 80);
        player.setPos(craftPos.getX()+.5, craftPos.getY(), craftPos.getZ()+2);
        for (BlockPos p : BlockPos.betweenClosed(craftPos.offset(-4,-4,-4), craftPos.offset(4,5,4))) {
            level.getChunkAt(p); level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(craftPos, wayoftime.bloodmagic.common.block.BloodMagicBlocks.MASTER_RITUAL_STONE.get().defaultBlockState());
        var master = (wayoftime.bloodmagic.common.block.entity.MasterRitualBlockEntity) level.getBlockEntity(craftPos);
        master.select(BasicRitual.CRAFTING);
        for (var part : BasicRitual.CRAFTING.components()) level.setBlockAndUpdate(craftPos.offset(part.offset()), part.rune().block().defaultBlockState());
        BlockPos chestPos = craftPos.above(); level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestPos);
        for (int slot : new int[]{0,1,3,4}) chest.setItem(slot, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS));
        network.consume(player.getUUID(), network.get(player.getUUID()).currentEssence());
        network.fill(player.getUUID(), 20000, 20000);
        var owner = new wayoftime.bloodmagic.core.data.Binding(player.getUUID(), player.getGameProfile().name());
        check(master.activate(player, owner) && network.get(player.getUUID()).currentEssence() == 5000,
                "crafting ritual activates for its upstream fee");
        wayoftime.bloodmagic.common.block.entity.MasterRitualBlockEntity.serverTick(level, craftPos, master.getBlockState(), master);
        check(chest.countItem(net.minecraft.world.item.Items.CRAFTING_TABLE) == 1,
                "crafting ritual consumes a 3x3 crafting grid and routes the result to the chest above");
        return assertions - start;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE6_SMOKE FAIL: " + message);
        assertions++;
    }
}
