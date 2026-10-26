package wayoftime.bloodmagic.common.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import wayoftime.bloodmagic.common.item.ItemDemonWill;
import wayoftime.bloodmagic.common.menu.MachineMenu;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.recipe.*;

/** Upstream Hellfire Forge is a single block; four inputs, a will slot and an output. */
public final class HellfireForgeBlockEntity extends MachineInventory implements MenuProvider {
    private ResourceKey<Recipe<?>> recipeId;
    private HellfireForgeRecipe definition;
    private int status;
    public HellfireForgeBlockEntity(BlockPos pos, BlockState state) { super(BloodMagicBlockEntities.HELLFIRE_FORGE.get(), pos, state, 6); }
    @Override public int catalystSlot() { return 4; }
    @Override public int outputSlot() { return 5; }
    @Override public int duration() { return definition == null ? 100 : definition.ticks(); }
    @Override public int status() { return status; }
    public double will() { return items.subList(0, 5).stream().mapToDouble(ItemDemonWill::amount).sum(); }
    @Override public int resourceAmount() { return (int) Math.round(will() * 100); }
    @Override protected void inventoryChanged() { super.inventoryChanged(); recipeId = null; definition = null; status = 0; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < 4 || slot == 4 && stack.getItem() instanceof ItemDemonWill; }
    public static void serverTick(Level level, BlockPos pos, BlockState state, HellfireForgeBlockEntity forge) {
        if (!(level instanceof ServerLevel server) || forge.isRemoved()) return;
        forge.setChanged();
        var input = new MachineRecipeInput(forge.items.subList(0, 4));
        var found = server.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.FORGE_TYPE.get(), input, server, forge.recipeId);
        if (found.isEmpty()) { forge.inventoryChanged(); return; }
        var recipe = found.get().value();
        if (!found.get().id().equals(forge.recipeId) || !recipe.sameDefinition(forge.definition)) {
            forge.progress = 0; forge.recipeId = found.get().id(); forge.definition = recipe;
        }
        if (forge.items.subList(0, 5).stream().noneMatch(s -> s.getItem() instanceof ItemDemonWill)) { forge.status = 1; forge.progress = 0; return; }
        if (forge.will() < Math.max(recipe.minimumWill(), recipe.drain())) { forge.status = 2; forge.progress = 0; return; }
        // Simulate on copies. Cost, gem upgrade remainder and output capacity form one commit.
        var next = forge.copies();
        double remainingCost = recipe.drain();
        for (int i = 0; i < 5 && remainingCost > 0; i++) {
            ItemStack stack = next.get(i); double amount = ItemDemonWill.amount(stack);
            if (amount <= 0) continue;
            double paid = Math.min(amount, remainingCost); ItemDemonWill.setAmount(stack, amount - paid); remainingCost -= paid;
            if (stack.getItem() instanceof ItemDemonWill item && !item.isGem() && ItemDemonWill.amount(stack) == 0) stack.shrink(1);
        }
        if (remainingCost > 1e-9) { forge.status = 2; return; }
        ItemStack result = recipe.assemble(input);
        double inputWill = next.subList(0, 4).stream().mapToDouble(ItemDemonWill::amount).sum();
        if (inputWill > 0) {
            if (!(result.getItem() instanceof ItemDemonWill output) || !output.isGem() || inputWill + ItemDemonWill.amount(result) > output.capacity()) { forge.status = 3; return; }
            ItemDemonWill.setAmount(result, ItemDemonWill.amount(result) + inputWill);
        }
        if (!forge.canAcceptOutput(result)) { forge.status = 3; forge.progress = 0; return; }
        forge.status = 0;
        if (++forge.progress < recipe.ticks()) return;
        for (int i = 0; i < 5; i++) forge.items.set(i, next.get(i));
        forge.consumeInputs(4); forge.insertOutput(result); forge.setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.bloodmagic.soulforge"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return level instanceof ServerLevel && stillValid(player) ? new MachineMenu(id, inventory, this, false) : null; }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); if (recipeId != null && definition != null) { out.store("recipe_id", Recipe.KEY_CODEC, recipeId); out.store("recipe_definition", HellfireForgeRecipe.CODEC.codec(), definition); } }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); recipeId = in.read("recipe_id", Recipe.KEY_CODEC).orElse(null); definition = in.read("recipe_definition", HellfireForgeRecipe.CODEC.codec()).orElse(null); progress = Math.min(progress, duration()); status = 0; }
}
