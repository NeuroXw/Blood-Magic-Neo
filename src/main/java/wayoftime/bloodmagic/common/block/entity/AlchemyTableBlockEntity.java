package wayoftime.bloodmagic.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wayoftime.bloodmagic.common.block.BlockAlchemyTable;
import wayoftime.bloodmagic.common.item.BloodOrbData;
import wayoftime.bloodmagic.common.item.ItemBloodOrb;
import wayoftime.bloodmagic.common.menu.MachineMenu;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.core.data.SoulNetworks;
import wayoftime.bloodmagic.recipe.*;

public final class AlchemyTableBlockEntity extends MachineInventory implements MenuProvider {
    private ResourceKey<Recipe<?>> recipeId;
    private AlchemyTableRecipe definition;
    private ResourceKey<Recipe<?>> potionRecipeId;
    private PotionRecipe potionDefinition;
    private int status;
    public AlchemyTableBlockEntity(BlockPos pos, BlockState state) { super(BloodMagicBlockEntities.ALCHEMY_TABLE.get(), pos, state, 8); }
    @Override public int outputSlot() { return 7; }
    @Override public int catalystSlot() { return 6; }
    @Override public int duration() { return potionDefinition != null ? potionDefinition.ticks() : definition == null ? 200 : definition.ticks(); }
    @Override public int status() { return status; }
    @Override public int resourceAmount() {
        var binding = BloodOrbData.getBinding(getItem(6));
        return level instanceof ServerLevel server && binding.isPresent() ? SoulNetworks.get(server).get(binding.get().ownerId()).currentEssence() : 0;
    }
    @Override protected void inventoryChanged() { super.inventoryChanged(); recipeId = null; definition = null; potionRecipeId = null; potionDefinition = null; status = 0; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < 6 || slot == 6 && stack.getItem() instanceof ItemBloodOrb; }
    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyTableBlockEntity table) {
        if (!(level instanceof ServerLevel server) || table.isRemoved()) return;
        table.setChanged();
        if (!BlockAlchemyTable.complete(level, pos, state)) { table.status = 4; return; }
        var input = new MachineRecipeInput(table.items.subList(0, 6));
        var potionFound = server.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.POTION_TYPE.get(), input, server, table.potionRecipeId);
        if (potionFound.isPresent()) {
            PotionRecipe potion = potionFound.get().value();
            if (!potionFound.get().id().equals(table.potionRecipeId) || !potion.sameDefinition(table.potionDefinition)) {
                table.progress = 0; table.potionRecipeId = potionFound.get().id(); table.potionDefinition = potion;
            }
            ItemStack orbPotion = table.getItem(6);
            var potionBinding = BloodOrbData.getBinding(orbPotion);
            if (!(orbPotion.getItem() instanceof ItemBloodOrb) || potionBinding.isEmpty() || BloodOrbData.getTier(orbPotion) < potion.minimumTier()) {
                table.status = 1; table.progress = 0; return;
            }
            if (table.resourceAmount() < potion.syphon()) { table.status = 2; return; }
            ItemStack potionResult = potion.assemble(input);
            if (!table.canAcceptOutput(potionResult)) { table.status = 3; table.progress = 0; return; }
            table.status = 0;
            if (++table.progress < potion.ticks()) { table.setChanged(); return; }
            if (!SoulNetworks.get(server).consume(potionBinding.get().ownerId(), potion.syphon())) return;
            potion.consumeIngredients(table.items.subList(0, 6));
            for (int i = 0; i < 6; i++) if (PotionRecipe.isFlask(table.items.get(i))) { table.items.set(i, ItemStack.EMPTY); break; }
            table.insertOutput(potionResult); table.progress = 0; table.potionRecipeId = null; table.potionDefinition = null; table.setChanged(); return;
        }        var found = server.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.TABLE_TYPE.get(), input, server, table.recipeId);
        if (found.isEmpty()) { table.inventoryChanged(); return; }
        var recipe = found.get().value();
        if (!found.get().id().equals(table.recipeId) || !recipe.sameDefinition(table.definition)) {
            table.progress = 0; table.recipeId = found.get().id(); table.definition = recipe;
        }
        ItemStack orb = table.getItem(6);
        var binding = BloodOrbData.getBinding(orb);
        if (!(orb.getItem() instanceof ItemBloodOrb) || binding.isEmpty() || BloodOrbData.getTier(orb) < recipe.minimumTier()) {
            table.status = 1; table.progress = 0; return;
        }
        if (table.resourceAmount() < recipe.syphon()) { table.status = 2; return; }
        ItemStack result = recipe.assemble(input);
        if (!table.canAcceptOutput(result)) { table.status = 3; return; }
        table.status = 0;
        if (++table.progress < recipe.ticks()) return;
        if (!SoulNetworks.get(server).consume(binding.get().ownerId(), recipe.syphon())) return;
        table.consumeInputs(6);
        table.insertOutput(result);
        table.setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.bloodmagic.alchemytable"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return level instanceof ServerLevel && stillValid(player) ? new MachineMenu(id, inventory, this, true) : null;
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (recipeId != null && definition != null) { out.store("recipe_id", Recipe.KEY_CODEC, recipeId); out.store("recipe_definition", AlchemyTableRecipe.CODEC.codec(), definition); }
        if (potionRecipeId != null && potionDefinition != null) { out.store("potion_recipe_id", Recipe.KEY_CODEC, potionRecipeId); out.store("potion_recipe_definition", PotionRecipe.CODEC.codec(), potionDefinition); }
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        recipeId = in.read("recipe_id", Recipe.KEY_CODEC).orElse(null);
        definition = in.read("recipe_definition", AlchemyTableRecipe.CODEC.codec()).orElse(null);
        potionRecipeId = in.read("potion_recipe_id", Recipe.KEY_CODEC).orElse(null);
        potionDefinition = in.read("potion_recipe_definition", PotionRecipe.CODEC.codec()).orElse(null);
        progress = Math.min(progress, duration()); status = 0;
    }
}
