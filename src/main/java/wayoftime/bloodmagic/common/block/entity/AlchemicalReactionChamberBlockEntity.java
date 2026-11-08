package wayoftime.bloodmagic.common.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wayoftime.bloodmagic.common.menu.ArcMenu;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.recipe.ArcRecipe;
import wayoftime.bloodmagic.recipe.MachineRecipeInput;

/** Server-authoritative ARC inventory, two 20-bucket tanks and atomic recipe commit. */
public final class AlchemicalReactionChamberBlockEntity extends MachineInventory implements MenuProvider {
    public static final int TOOL_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int NUM_OUTPUTS = 5;
    public static final int INPUT_SLOT = 6;
    public static final int INPUT_BUCKET_SLOT = 7;
    public static final int OUTPUT_BUCKET_SLOT = 8;
    public static final int TANK_CAPACITY = 20_000;
    public static final int WORK_TICKS = 200;
    private Identifier inputFluid = BuiltInRegistries.FLUID.getKey(Fluids.EMPTY);
    private int inputAmount;
    private Identifier outputFluid = BuiltInRegistries.FLUID.getKey(Fluids.EMPTY);
    private int outputAmount;
    private ResourceKey<Recipe<?>> recipeId;
    private ArcRecipe definition;
    private int status;

    public AlchemicalReactionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(BloodMagicBlockEntities.ALCHEMICAL_REACTION_CHAMBER.get(), pos, state, 9);
    }

    @Override public int duration() { return WORK_TICKS; }
    @Override public int outputSlot() { return 1; }
    @Override public int catalystSlot() { return 0; }
    @Override public int status() { return status; }
    @Override public int resourceAmount() { return inputAmount; }
    public Identifier inputFluid() { return inputFluid; }
    public int inputFluidAmount() { return inputAmount; }
    public Identifier outputFluid() { return outputFluid; }
    public int outputFluidAmount() { return outputAmount; }
    public int tankCapacity() { return TANK_CAPACITY; }
    @Override protected void inventoryChanged() { super.inventoryChanged(); recipeId = null; definition = null; status = 0; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= OUTPUT_SLOT && slot < OUTPUT_SLOT + NUM_OUTPUTS) return false;
        if (slot == INPUT_BUCKET_SLOT) return fluidOf(stack) != null;
        if (slot == OUTPUT_BUCKET_SLOT) return stack.is(Items.BUCKET);
        return slot >= 0 && slot <= INPUT_SLOT;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalReactionChamberBlockEntity arc) {
        if (!(level instanceof ServerLevel server) || arc.isRemoved()) return;
        arc.processBuckets();
        var input = new MachineRecipeInput(List.of(arc.items.get(INPUT_SLOT), arc.items.get(TOOL_SLOT)));
        var found = server.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.ARC_TYPE.get(), input, server, arc.recipeId);
        if (found.isEmpty()) { arc.status = 1; arc.progress = 0; arc.setLit(false); arc.setChanged(); return; }
        ArcRecipe recipe = found.get().value();
        if (!found.get().id().equals(arc.recipeId) || !recipe.sameDefinition(arc.definition)) {
            arc.recipeId = found.get().id(); arc.definition = recipe; arc.progress = 0;
        }
        if (recipe.inputFluid().isPresent()) {
            ArcRecipe.FluidSpec required = recipe.inputFluid().get();
            if (arc.inputAmount < required.amount() || !arc.inputFluid.equals(required.fluid())) { arc.status = 2; arc.progress = 0; arc.setLit(false); arc.setChanged(); return; }
        }
        List<ItemStack> results = recipe.rollOutputs(server.getRandom());
        if (!arc.outputsFit(results) || (recipe.outputFluid().isPresent() && !arc.outputFluidFits(recipe.outputFluid().get()))) {
            arc.status = 3; arc.progress = 0; arc.setLit(false); arc.setChanged(); return;
        }
        if (recipe.outputFluid().isPresent()) {
            ArcRecipe.FluidSpec fluid = recipe.outputFluid().get();
            if (arc.outputAmount > 0 && !arc.outputFluid.equals(fluid.fluid())) { arc.status = 3; arc.progress = 0; return; }
        }
        arc.status = 0;
        arc.setLit(true);
        if (++arc.progress < WORK_TICKS) { arc.setChanged(); return; }
        if (recipe.inputFluid().isPresent()) arc.inputAmount -= recipe.inputFluid().get().amount();
        if (recipe.outputFluid().isPresent()) { arc.outputFluid = recipe.outputFluid().get().fluid(); arc.outputAmount += recipe.outputFluid().get().amount(); }
        if (recipe.consumeIngredient()) arc.items.get(INPUT_SLOT).shrink(recipe.inputSize());
        for (ItemStack result : results) arc.insertResult(result);
        arc.damageTool();
        arc.progress = 0; arc.recipeId = null; arc.definition = null; arc.setLit(false); arc.setChanged();
    }

    private void processBuckets() {
        ItemStack sourceBucket = items.get(INPUT_BUCKET_SLOT);
        Identifier sourceFluid = fluidOf(sourceBucket);
        if (sourceFluid != null && sourceBucket.getCount() == 1) {
            int canMove = sourceFluid.equals(inputFluid) || inputAmount == 0 ? Math.min(1000, TANK_CAPACITY - inputAmount) : 0;
            if (canMove == 1000 && placeEmptyBucket()) { inputFluid = sourceFluid; inputAmount += canMove; addEmptyBucket(); items.set(INPUT_BUCKET_SLOT, ItemStack.EMPTY); setChanged(); }
        }
        ItemStack targetBucket = items.get(OUTPUT_BUCKET_SLOT);
        if (targetBucket.is(Items.BUCKET) && outputAmount >= 1000 && targetBucket.getCount() == 1) {
            ItemStack filled = filledBucket(outputFluid);
            if (!filled.isEmpty()) { outputAmount -= 1000; items.set(OUTPUT_BUCKET_SLOT, filled); setChanged(); }
        }
    }

    private boolean placeEmptyBucket() {
        ItemStack target = items.get(OUTPUT_BUCKET_SLOT);
        return target.isEmpty() || target.is(Items.BUCKET) && target.getCount() < target.getMaxStackSize();
    }
    private void addEmptyBucket() {
        ItemStack target = items.get(OUTPUT_BUCKET_SLOT);
        if (target.isEmpty()) items.set(OUTPUT_BUCKET_SLOT, new ItemStack(Items.BUCKET));
        else target.grow(1);
    }
    private static Identifier fluidOf(ItemStack stack) {
        if (stack.getItem() instanceof BucketItem bucket && bucket.getContent() != Fluids.EMPTY) return BuiltInRegistries.FLUID.getKey(bucket.getContent());
        return null;
    }
    private static ItemStack filledBucket(Identifier fluid) {
        var fluidValue = BuiltInRegistries.FLUID.getOptional(fluid).orElse(Fluids.EMPTY);
        return fluidValue == Fluids.EMPTY ? ItemStack.EMPTY : fluidValue.getBucket().getDefaultInstance();
    }

    private boolean outputsFit(List<ItemStack> results) {
        var simulated = new java.util.ArrayList<ItemStack>();
        for (int i = OUTPUT_SLOT; i < OUTPUT_SLOT + NUM_OUTPUTS; i++) simulated.add(items.get(i).copy());
        for (ItemStack result : results) {
            boolean placed = false;
            for (int i = 0; i < simulated.size(); i++) {
                ItemStack slot = simulated.get(i);
                if (slot.isEmpty()) { simulated.set(i, result.copy()); placed = true; break; }
                if (ItemStack.isSameItemSameComponents(slot, result) && slot.getCount() + result.getCount() <= slot.getMaxStackSize()) { slot.grow(result.getCount()); placed = true; break; }
            }
            if (!placed) return false;
        }
        return true;
    }
    private boolean outputFluidFits(ArcRecipe.FluidSpec spec) {
        return (outputAmount == 0 || outputFluid.equals(spec.fluid())) && outputAmount + spec.amount() <= TANK_CAPACITY;
    }
    private void insertResult(ItemStack result) {
        for (int i = OUTPUT_SLOT; i < OUTPUT_SLOT + NUM_OUTPUTS; i++) {
            ItemStack slot = items.get(i);
            if (slot.isEmpty()) { items.set(i, result.copy()); return; }
            if (ItemStack.isSameItemSameComponents(slot, result) && slot.getCount() + result.getCount() <= slot.getMaxStackSize()) { slot.grow(result.getCount()); return; }
        }
    }
    private void damageTool() {
        ItemStack tool = items.get(TOOL_SLOT);
        if (tool.isEmpty()) return;
        if (tool.isDamageableItem()) {
            int next = tool.getDamageValue() + 1;
            if (next >= tool.getMaxDamage()) items.set(TOOL_SLOT, ItemStack.EMPTY);
            else tool.setDamageValue(next);
        } else if (tool.getMaxStackSize() > 1) {
            tool.shrink(1);
            if (tool.isEmpty()) items.set(TOOL_SLOT, ItemStack.EMPTY);
        }
    }

    private void setLit(boolean lit) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(wayoftime.bloodmagic.common.block.BlockAlchemicalReactionChamber.LIT)
                && state.getValue(wayoftime.bloodmagic.common.block.BlockAlchemicalReactionChamber.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(wayoftime.bloodmagic.common.block.BlockAlchemicalReactionChamber.LIT, lit), 3);
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("block.bloodmagic.alchemicalreactionchamber"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return level instanceof ServerLevel && stillValid(player) ? new ArcMenu(id, inventory, this) : null;
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("input_fluid", Identifier.CODEC, inputFluid); out.putInt("input_amount", inputAmount);
        out.store("output_fluid", Identifier.CODEC, outputFluid); out.putInt("output_amount", outputAmount);
        if (recipeId != null && definition != null) { out.store("recipe_id", Recipe.KEY_CODEC, recipeId); out.store("recipe_definition", ArcRecipe.CODEC.codec(), definition); }
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        inputFluid = in.read("input_fluid", Identifier.CODEC).orElse(BuiltInRegistries.FLUID.getKey(Fluids.EMPTY));
        inputAmount = Math.clamp(in.getIntOr("input_amount", 0), 0, TANK_CAPACITY);
        outputFluid = in.read("output_fluid", Identifier.CODEC).orElse(BuiltInRegistries.FLUID.getKey(Fluids.EMPTY));
        outputAmount = Math.clamp(in.getIntOr("output_amount", 0), 0, TANK_CAPACITY);
        recipeId = in.read("recipe_id", Recipe.KEY_CODEC).orElse(null);
        definition = in.read("recipe_definition", ArcRecipe.CODEC.codec()).orElse(null);
        progress = Math.min(progress, WORK_TICKS); status = 0;
    }
}


