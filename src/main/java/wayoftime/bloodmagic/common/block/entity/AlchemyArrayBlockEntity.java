package wayoftime.bloodmagic.common.block.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.core.data.SoulNetworks;
import wayoftime.bloodmagic.recipe.AlchemyArrayRecipe;
import wayoftime.bloodmagic.recipe.ArrayRecipeInput;

public final class AlchemyArrayBlockEntity extends MachineInventory {
    private UUID owner;
    private Direction rotation = Direction.NORTH;
    private ResourceKey<Recipe<?>> recipeId;
    private AlchemyArrayRecipe definition;
    private boolean insufficientLP;
    public AlchemyArrayBlockEntity(BlockPos pos, BlockState state) { super(BloodMagicBlockEntities.ALCHEMY_ARRAY.get(), pos, state, 2); }
    public void initialize(UUID placer, Direction direction) { owner = placer; rotation = direction; setChanged(); sync(); }
    public UUID owner() { return owner; }
    public Direction rotation() { return rotation; }
    public Identifier texture() { return definition == null ? AlchemyArrayRecipe.DEFAULT_TEXTURE : definition.texture(); }
    @Override public int getMaxStackSize() { return 1; }
    @Override public int duration() { return definition == null ? 200 : definition.ticks(); }
    @Override public int outputSlot() { return -1; }
    @Override public int catalystSlot() { return 1; }
    @Override public int status() { return insufficientLP ? 1 : 0; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < 2 && getItem(slot).isEmpty(); }
    @Override protected void inventoryChanged() { super.inventoryChanged(); recipeId = null; definition = null; insufficientLP = false; }
    public Optional<RecipeHolder<AlchemyArrayRecipe>> findRecipe() {
        return level instanceof ServerLevel server ? server.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.ARRAY_TYPE.get(),
                new ArrayRecipeInput(getItem(0), getItem(1)), server, recipeId) : Optional.empty();
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyArrayBlockEntity array) {
        if (!(level instanceof ServerLevel server) || array.isRemoved()) return;
        // Never ask the chunk source to load a neighbour for a support check.
        if (!wayoftime.bloodmagic.ritual.WorldAccess.loaded(level, pos.below())) return;
        if (!state.canSurvive(level, pos)) { level.destroyBlock(pos, false); return; }
        array.setChanged(); // Detect direct Container/Slot mutations before using saved progress.
        var found = array.findRecipe();
        if (found.isEmpty()) {
            if (array.definition != null || array.progress != 0) { array.inventoryChanged(); array.setChanged(); array.sync(); }
            return;
        }
        var holder = found.get();
        var recipe = holder.value();
        if (!holder.id().equals(array.recipeId) || !recipe.sameDefinition(array.definition)) {
            array.progress = 0; array.recipeId = holder.id(); array.definition = recipe; array.sync();
        }
        array.insufficientLP = recipe.lpCost() > 0 && (array.owner == null || SoulNetworks.get(server).get(array.owner).currentEssence() < recipe.lpCost());
        if (array.insufficientLP) return;
        array.progress = Math.min(recipe.ticks(), array.progress + 1);
        array.setChanged();
        if (array.progress < recipe.ticks()) return;
        ItemStack result = recipe.assemble(new ArrayRecipeInput(array.getItem(0), array.getItem(1)));
        if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) return;
        if (recipe.lpCost() > 0 && !SoulNetworks.get(server).consume(array.owner, recipe.lpCost())) return;
        // A cancelled entity spawn must leave inputs and LP untouched; only commit after successful spawn.
        ItemEntity drop = new ItemEntity(server, pos.getX() + .5, pos.getY() + .25, pos.getZ() + .5, result);
        boolean spawned = false;
        try {
            spawned = server.addFreshEntity(drop);
            if (!spawned) return;
        } finally {
            if (!spawned && recipe.lpCost() > 0) SoulNetworks.get(server).fill(array.owner, recipe.lpCost(), Integer.MAX_VALUE);
        }
        array.clearContent(); // Clears both ingredients before the removal hook can drop them.
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!stillValid(player) || !player.mayBuild() || !level.mayInteract(player, worldPosition)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() && player.isShiftKeyDown()) {
            int slot = getItem(1).isEmpty() ? 0 : 1;
            ItemStack returned = removeItem(slot, 1);
            if (!player.getInventory().add(returned)) player.drop(returned, false);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (held.isEmpty()) return InteractionResult.SUCCESS_SERVER;
        int slot = getItem(0).isEmpty() ? 0 : getItem(1).isEmpty() ? 1 : -1;
        if (slot < 0) return InteractionResult.FAIL;
        if (owner == null) owner = player.getUUID();
        setItem(slot, held.copyWithCount(1));
        if (!player.hasInfiniteMaterials()) held.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
        out.store("rotation", Direction.CODEC, rotation);
        if (recipeId != null && definition != null) {
            out.store("recipe_id", Recipe.KEY_CODEC, recipeId);
            out.store("recipe_definition", AlchemyArrayRecipe.CODEC.codec(), definition);
        }
        out.putBoolean("insufficient_lp", insufficientLP);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        rotation = in.read("rotation", Direction.CODEC).filter(d -> d.getAxis().isHorizontal()).orElse(Direction.NORTH);
        recipeId = in.read("recipe_id", Recipe.KEY_CODEC).orElse(null);
        definition = in.read("recipe_definition", AlchemyArrayRecipe.CODEC.codec()).orElse(null);
        progress = Math.min(progress, duration());
        insufficientLP = in.getBooleanOr("insufficient_lp", false);
    }
}
