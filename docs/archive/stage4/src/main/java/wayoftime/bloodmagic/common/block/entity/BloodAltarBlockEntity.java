package wayoftime.bloodmagic.common.block.entity;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import wayoftime.bloodmagic.altar.AltarState;
import wayoftime.bloodmagic.altar.AltarStructure;
import wayoftime.bloodmagic.altar.AltarUpgrades;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.network.AltarProgressPayload;
import wayoftime.bloodmagic.recipe.BloodAltarRecipe;
import wayoftime.bloodmagic.recipe.BloodAltarRecipeInput;
import wayoftime.bloodmagic.common.menu.AltarMenu;

/** Data foundation ported from upstream TileAltar, TileInventory and BloodAltar. */
public final class BloodAltarBlockEntity extends BlockEntity implements Container, MenuProvider {
    private AltarState altarState = AltarState.EMPTY;
    private ItemStack storedItem = ItemStack.EMPTY;
    private ItemStack lastInventorySnapshot = ItemStack.EMPTY;
    private boolean syncPending = true;
    private int syncCooldown;
    private AltarUpgrades upgrades = AltarUpgrades.EMPTY;
    private int structureCooldown;
    private boolean structureAvailable = true;
    private ResourceKey<Recipe<?>> recipeId;
    private BloodAltarRecipe recipeDefinition;
    private int craftCooldown;

    public BloodAltarBlockEntity(BlockPos pos, BlockState state) {
        super(BloodMagicBlockEntities.BLOOD_ALTAR.get(), pos, state);
    }

    public AltarState getAltarState() {
        return altarState;
    }

    /** Call on the server thread. A detached entity may also be populated before placement. */
    public void setAltarState(AltarState state) {
        Objects.requireNonNull(state);
        if (level != null && level.isClientSide()) {
            return;
        }
        if (!altarState.equals(state)) {
            altarState = state;
            setChanged();
            syncPending = true;
        }
    }

    /** Returns actual accepted LP; negative amounts cannot drain the tank. */
    public int fillMainTank(int amount) {
        if (level != null && level.isClientSide()) {
            return 0;
        }
        int accepted = Math.min(Math.max(0, amount), altarState.capacity() - altarState.blood());
        setAltarState(altarState.withBlood(altarState.blood() + accepted));
        return accepted;
    }

    public int drainMainTank(int amount) {
        if (level != null && level.isClientSide()) {
            return 0;
        }
        int drained = Math.min(Math.max(0, amount), altarState.blood());
        setAltarState(altarState.withBlood(altarState.blood() - drained));
        return drained;
    }

    /** Defensive copies prevent silent inventory changes that bypass saving/sync. */
    public ItemStack getStoredItem() {
        return storedItem.copy();
    }

    public void setStoredItem(ItemStack stack) {
        if (level != null && level.isClientSide()) {
            return;
        }
        ItemStack copy = stack.copy();
        copy.limitSize(copy.getMaxStackSize());
        if (!ItemStack.matches(storedItem, copy)) {
            storedItem = copy;
            setChanged();
        }
    }

    @Override
    public void setChanged() {
        // Slot/moveItemStackTo mutate live stacks in place. Detect count AND component changes,
        // but never reset a paid recipe merely because the ticker marks LP as dirty.
        if ((level == null || !level.isClientSide()) && !ItemStack.matches(storedItem, lastInventorySnapshot)) {
            lastInventorySnapshot = storedItem.copy(); // Update first: clearRecipe can call setChanged again.
            clearRecipe();
            craftCooldown = 0;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        super.setChanged();
    }

    @Override
    public int getContainerSize() { return 1; }

    @Override
    public boolean isEmpty() { return storedItem.isEmpty(); }

    /** Container requires a live stack; external readers should use defensive getStoredItem(). */
    @Override
    public ItemStack getItem(int slot) { return slot == 0 ? storedItem : ItemStack.EMPTY; }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) setStoredItem(stack);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0 || (level != null && level.isClientSide())) return ItemStack.EMPTY;
        ItemStack removed = storedItem.split(amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        // Clearing paid progress must accompany every extraction, including this Container entry point.
        if (slot != 0 || (level != null && level.isClientSide())) return ItemStack.EMPTY;
        ItemStack result = storedItem;
        setStoredItem(ItemStack.EMPTY);
        return result;
    }

    @Override
    public void clearContent() { setStoredItem(ItemStack.EMPTY); }

    @Override
    public boolean stillValid(Player player) {
        return level != null && player.level() == level && !isRemoved() && !player.isSpectator()
                && player.isAlive() && Container.stillValidBlockEntity(this, player);
    }

    @Override
    public Component getDisplayName() { return Component.translatable("block.bloodmagic.altar"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return level instanceof ServerLevel && stillValid(player) ? new AltarMenu(containerId, inventory, this) : null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // 26.1.2 ValueInput carries the registry lookup; no extra Provider parameter here.
        altarState = AltarState.load(input.childOrEmpty("altar"));
        storedItem = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        storedItem.limitSize(storedItem.getMaxStackSize());
        lastInventorySnapshot = storedItem.copy();
        recipeId = input.read("recipe_id", Recipe.KEY_CODEC).orElse(null);
        recipeDefinition = input.read("recipe_definition", BloodAltarRecipe.CODEC.codec()).orElse(null);
        craftCooldown = Math.clamp(input.getIntOr("craft_cooldown", 0), 0, 30);
        upgrades = AltarUpgrades.EMPTY;
        structureCooldown = 0;
        structureAvailable = true;
        syncPending = true;
        // TODO: Follow-up - explicit conversion of 1.20.1 bloodAltar/Items NBT if old-world import is supported.
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        altarState.save(output.child("altar"));
        output.store("item", ItemStack.OPTIONAL_CODEC, storedItem);
        if (recipeId != null && recipeDefinition != null) {
            output.store("recipe_id", Recipe.KEY_CODEC, recipeId);
            output.store("recipe_definition", BloodAltarRecipe.CODEC.codec(), recipeDefinition);
        }
        output.putInt("craft_cooldown", craftCooldown);
    }

    /** Initial chunk tracking and vanilla BE updates must also carry the current snapshot. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void applyClientSnapshot(AltarState state) {
        if (level != null && level.isClientSide()) {
            altarState = state;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BloodAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel serverLevel) || altar.isRemoved()) {
            return;
        }
        altar.tickGameplay(serverLevel);
        if (altar.syncCooldown > 0) {
            altar.syncCooldown--;
        }
        if (altar.syncPending && altar.syncCooldown == 0) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, ChunkPos.containing(pos),
                    new AltarProgressPayload(level.dimension(), pos, altar.altarState));
            altar.syncPending = false;
            altar.syncCooldown = 5;
        }
    }

    public AltarUpgrades getUpgrades() {
        return upgrades;
    }

    /** Never force-load structure chunks. An incomplete read pauses an existing higher tier. */
    public boolean refreshStructure() {
        if (!(level instanceof ServerLevel)) return false;
        AltarStructure.Scan scan = AltarStructure.scan(level, worldPosition);
        structureAvailable = scan.complete() || scan.tier() >= altarState.tier();
        structureCooldown = 20;
        if (!structureAvailable) return false;
        upgrades = scan.upgrades();
        setAltarState(new AltarState(altarState.blood(), upgrades.capacity(), altarState.inputBlood(),
                altarState.outputBlood(), upgrades.bufferCapacity(), scan.tier(), altarState.progress(),
                altarState.requiredBlood(), altarState.active()));
        return true;
    }

    public Optional<RecipeHolder<BloodAltarRecipe>> findRecipe() {
        if (!(level instanceof ServerLevel serverLevel) || storedItem.isEmpty()) return Optional.empty();
        // Resolve through the current manager on every tick: /reload must not leave stale recipe objects running.
        return serverLevel.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.ALTAR_TYPE.get(),
                new BloodAltarRecipeInput(storedItem, altarState.tier()), serverLevel, recipeId);
    }

    private void tickGameplay(ServerLevel serverLevel) {
        if (--structureCooldown <= 0) refreshStructure();
        if (!structureAvailable) return;
        if (craftCooldown > 0) {
            craftCooldown--;
            setChanged();
            return;
        }
        Optional<RecipeHolder<BloodAltarRecipe>> match = findRecipe();
        if (match.isEmpty()) {
            clearRecipe();
            return;
        }
        RecipeHolder<BloodAltarRecipe> holder = match.get();
        BloodAltarRecipe recipe = holder.value();
        BloodAltarRecipeInput input = new BloodAltarRecipeInput(storedItem, altarState.tier());
        int required = recipe.totalBlood(input);
        if (!holder.id().equals(recipeId) || !recipe.sameDefinition(recipeDefinition)
                || required != altarState.requiredBlood()) {
            clearRecipe();
            recipeId = holder.id();
            recipeDefinition = recipe;
            setChanged();
        }
        int progress = altarState.progress();
        int consumed = Math.min(Math.min(upgrades.consumption(recipe.consumptionRate()), altarState.blood()),
                required - progress);
        if (consumed > 0) progress += consumed;
        else progress = Math.max(0, progress - upgrades.decay(recipe.drainRate()));
        setAltarState(new AltarState(altarState.blood() - consumed, altarState.capacity(), altarState.inputBlood(),
                altarState.outputBlood(), altarState.bufferCapacity(), altarState.tier(), progress, required, true));
        if (progress >= required) {
            setStoredItem(recipe.assemble(input));
            clearRecipe(); // Also reset a datapack recipe whose result equals its input.
            craftCooldown = 30; // Upstream gives the player time to retrieve a slate before the next tier starts.
            setChanged();
        }
        // TODO: fluid/item capabilities and buffer transfer; charging/orb/soul-network and redstone subsystems.
    }

    private void clearRecipe() {
        if (recipeId != null || recipeDefinition != null) setChanged();
        recipeId = null;
        recipeDefinition = null;
        setAltarState(new AltarState(altarState.blood(), altarState.capacity(), altarState.inputBlood(),
                altarState.outputBlood(), altarState.bufferCapacity(), altarState.tier(), 0, 0, false));
    }

    /** Explicit player action; rune blocks only modify conversion, they never supply free LP. */
    public int sacrificeHealth(Player player) {
        if (!(level instanceof ServerLevel serverLevel) || player.level() != level || player.isSpectator()
                || !player.isAlive() || !player.isWithinBlockInteractionRange(worldPosition, 0)
                || !refreshStructure() || altarState.blood() >= altarState.capacity()) return 0;
        if (player.hasInfiniteMaterials()) return fillMainTank(upgrades.sacrifice(200, true));
        if (player.getHealth() <= 2.0F) return 0;
        float before = player.getHealth();
        if (!player.hurtServer(serverLevel, player.damageSources().magic(), 2.0F)) return 0;
        int baseLP = Math.round(Math.max(0, before - player.getHealth()) * 100);
        return fillMainTank(upgrades.sacrifice(baseLP, true));
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSpectator()) return InteractionResult.PASS;
        if (level == null || player.level() != level) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!player.isWithinBlockInteractionRange(worldPosition, 0)) return InteractionResult.FAIL;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() && player.isShiftKeyDown()) {
            return sacrificeHealth(player) > 0 ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        if (held.isEmpty()) {
            // Ordinary empty-hand use always opens the menu, even when the altar contains output.
            // Sneaking sacrifice was handled above. Inventory extraction belongs to the menu now.
            return player.openMenu(this).isPresent() ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown() || !storedItem.isEmpty()) return InteractionResult.FAIL;
        refreshStructure();
        setStoredItem(held.copyWithCount(1));
        if (!player.hasInfiniteMaterials()) held.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState newState) {
        if (level instanceof ServerLevel && !storedItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, storedItem);
            storedItem = ItemStack.EMPTY;
        }
        super.preRemoveSideEffects(pos, newState);
    }
}
