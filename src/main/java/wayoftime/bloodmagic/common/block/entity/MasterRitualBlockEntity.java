package wayoftime.bloodmagic.common.block.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.BlockDemonCrystal;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemDemonCrystal;
import wayoftime.bloodmagic.core.data.*;
import wayoftime.bloodmagic.ritual.*;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;
import wayoftime.bloodmagic.will.DemonWillHolder;

public final class MasterRitualBlockEntity extends BlockEntity {
    private Binding owner;
    private BasicRitual ritual = BasicRitual.WATER;
    private boolean active;
    private int cooldown;
    private Direction direction = Direction.NORTH;
    private BlockPos quarryCursor;
    private final Map<String, RitualArea> ranges = new LinkedHashMap<>();
    private final List<EnumDemonWillType> activeWillConfig = new ArrayList<>();
    private static final Codec<List<SavedRange>> RANGES_CODEC = SavedRange.CODEC.listOf();
    public MasterRitualBlockEntity(BlockPos pos,BlockState state) {
        super(BloodMagicBlockEntities.MASTER_RITUAL.get(),pos,state);
        ensureRitualRanges();
    }
    public Binding owner() { return owner; }
    public BasicRitual ritual() { return ritual; }
    public boolean active() { return active; }
    public void select(BasicRitual ritual) {
        if(level instanceof ServerLevel && !active && ritual != null) {
            this.ritual=ritual;
            ensureRitualRanges();
            changed();
        }
    }
    public Direction direction() { return direction; }
    public void cycleDirection(Player player) {
        if (!validUser(player)) return;
        direction = switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            default -> Direction.NORTH;
        };
        player.sendSystemMessage(Component.translatable("message.bloodmagic.ritual.direction",
                Component.translatable("direction.minecraft." + direction.getSerializedName())));
        changed();
    }
    public BlockPos quarryCursor() { return quarryCursor; }
    public void setQuarryCursor(BlockPos cursor) { quarryCursor = cursor; changed(); }
    public ServerPlayer fakeUser() {
        return level instanceof ServerLevel server && owner != null
                ? FakePlayerFactory.get(server, new GameProfile(owner.ownerId(), owner.ownerName())) : null;
    }
    public List<EnumDemonWillType> activeWillConfig() { return List.copyOf(activeWillConfig); }
    public RitualArea range(String name) { return ranges.get(name); }
    public DemonWillHolder willHolder() { return level == null ? null : WorldDemonWillHandler.getWillHolder(level, worldPosition); }
    public double configuredWill(EnumDemonWillType type) {
        if (!activeWillConfig.contains(type)) return 0;
        DemonWillHolder holder = willHolder();
        return holder == null ? 0 : holder.getWill(type);
    }
    public double drainWill(EnumDemonWillType type, double amount) {
        if (!activeWillConfig.contains(type) || !(amount > 0)) return 0;
        return WorldDemonWillHandler.drainWill(level, worldPosition, type, amount, true);
    }
    public int currentLp() {
        return level instanceof ServerLevel server && owner != null ? SoulNetworks.get(server).get(owner.ownerId()).currentEssence() : 0;
    }
    public boolean payLp(int amount) {
        if (amount <= 0) return true;
        return level instanceof ServerLevel server && owner != null && SoulNetworks.get(server).consume(owner.ownerId(), amount);
    }
    public int maxVolumeForRange(String range) { return ritual.maxVolumeForRange(range, activeWillConfig, willHolder()); }
    public int maxVerticalForRange(String range) { return ritual.maxVerticalForRange(range, activeWillConfig, willHolder()); }
    public int maxHorizontalForRange(String range) { return ritual.maxHorizontalForRange(range, activeWillConfig, willHolder()); }
    public boolean rangeWithinLimits(String range) {
        RitualArea area = ranges.get(range);
        return area != null && area.isWithinLimits(maxVerticalForRange(range), maxHorizontalForRange(range));
    }
    public void resetRitualRanges() {
        ranges.clear();
        ensureRitualRanges();
        changed();
    }
    private void ensureRitualRanges() {
        for (RitualRangeSpec spec : ritual.rangeSpecs()) ranges.putIfAbsent(spec.name(), spec.area().copy());
    }
    public void setActiveWillConfig(Player player, List<EnumDemonWillType> types) {
        if (!validUser(player) || types == null) return;
        activeWillConfig.clear();
        for (EnumDemonWillType type : types) {
            if (type != null && !activeWillConfig.contains(type) && activeWillConfig.size() < EnumDemonWillType.values().length)
                activeWillConfig.add(type);
        }
        changed();
    }
    public EnumReaderBoundaries setBlockRangeByBounds(Player player, String rangeName, BlockPos first, BlockPos second) {
        if (!validUser(player)) return EnumReaderBoundaries.UNKNOWN_RANGE;
        RitualArea area = ranges.get(rangeName);
        if (area == null || ritual.rangeSpec(rangeName) == null) return EnumReaderBoundaries.UNKNOWN_RANGE;
        RitualArea proposed = RitualArea.between(first, second);
        int maxVolume = maxVolumeForRange(rangeName);
        if (maxVolume > 0 && proposed.volume() > maxVolume) return EnumReaderBoundaries.VOLUME_TOO_LARGE;
        if (!proposed.isWithinLimits(maxVerticalForRange(rangeName), maxHorizontalForRange(rangeName)))
            return EnumReaderBoundaries.NOT_WITHIN_BOUNDARIES;
        area.resizeByBlockPositions(first, second);
        changed();
        return EnumReaderBoundaries.SUCCESS;
    }
    public void provideRitualInformation(Player player) {
        if (!validUser(player)) return;
        player.sendSystemMessage(Component.translatable("message.bloodmagic.ritual.info",
                Component.translatable("ritual.bloodmagic." + ritual.getSerializedName()),
                ritual.activationCost, ritual.cost, ritual.refreshInterval(activeWillConfig, willHolder())));
        player.sendSystemMessage(Component.translatable("message.bloodmagic.ritual.direction",
                Component.translatable("direction.minecraft." + direction.getSerializedName())));
        provideWillInformation(player);
        for (RitualRangeSpec spec : ritual.rangeSpecs()) provideRangeInformation(player, spec.name());
    }
    public void provideRangeInformation(Player player, String rangeName) {
        if (!validUser(player) || rangeName == null || rangeName.isEmpty()) return;
        RitualArea area = ranges.get(rangeName);
        if (area == null) { player.sendSystemMessage(Component.translatable("ritual.bloodmagic.blockRange.noRange")); return; }
        player.sendSystemMessage(Component.translatable("message.bloodmagic.ritual.range", rangeName,
                area.minimum().toShortString(), area.maximum().offset(-1, -1, -1).toShortString(), area.volume(),
                maxVerticalForRange(rangeName), maxHorizontalForRange(rangeName), maxVolumeForRange(rangeName)));
    }
    public void provideWillInformation(Player player) {
        if (!validUser(player)) return;
        if (activeWillConfig.isEmpty()) {
            player.sendSystemMessage(Component.translatable("ritual.bloodmagic.willConfig.void"));
            return;
        }
        List<Component> names = activeWillConfig.stream().map(type -> (Component) Component.translatable(
                "tooltip.bloodmagic.currentBaseType." + type.getSerializedName())).toList();
        Component joined = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) joined = joined.copy().append(", ");
            joined = joined.copy().append(names.get(i));
        }
        player.sendSystemMessage(Component.translatable("ritual.bloodmagic.willConfig.set", joined));
    }
    public boolean activate(Player player,Binding binding) {
        return activate(player, binding, 0);
    }
    public boolean activate(Player player,Binding binding,int crystalLevel) {
        if(!(level instanceof ServerLevel server) || binding==null || !validUser(player) || crystalLevel < 0)return false;
        if(active)return owner!=null && owner.ownerId().equals(binding.ownerId());
        BasicRitual chosen=ritual;
        if(chosen.scan(level,worldPosition)!=BasicRitual.Scan.COMPLETE) {
            chosen=null;
            for(var candidate:BasicRitual.values())if(candidate.scan(level,worldPosition)==BasicRitual.Scan.COMPLETE){chosen=candidate;break;}
        }
        if(chosen==null || level.hasNeighborSignal(worldPosition))return false;
        if(!player.hasInfiniteMaterials() && !SoulNetworks.get(server).consume(binding.ownerId(),chosen.activationCost))return false;
        owner=binding; ritual=chosen; ensureRitualRanges(); active=true; cooldown=0;changed();return true;
    }
    public boolean stop(Player player,Binding binding) {
        if(!validUser(player) || owner==null || binding==null || !owner.ownerId().equals(binding.ownerId()))return false;
        active=false;cooldown=0;changed();return true;
    }
    private boolean validUser(Player player) { return player!=null && player.isAlive() && !player.isSpectator() && player.level()==level && player.mayBuild()
            && player.isWithinBlockInteractionRange(worldPosition,0) && level.mayInteract(player,worldPosition); }
    public static void serverTick(Level level,BlockPos pos,BlockState state,MasterRitualBlockEntity master) {
        if(!(level instanceof ServerLevel server) || master.isRemoved() || !master.active || master.owner==null)return;
        var scan=master.ritual.scan(level,pos);
        if(scan==BasicRitual.Scan.UNLOADED)return;
        if(scan==BasicRitual.Scan.INVALID){master.active=false;master.changed();return;}
        if(level.hasNeighborSignal(pos))return;
        if(!master.rangeWithinLimitsForEffects())return;
        if(master.cooldown>0){master.cooldown--;master.setChanged();return;}
        master.cooldown=master.ritual.refreshInterval(master.activeWillConfig, master.willHolder())-1;master.setChanged();
        master.perform(server);
    }
    private void perform(ServerLevel server) {
        if (SoulNetworks.get(server).get(owner.ownerId()).currentEssence() < ritual.cost) return;
        ServerPlayer actor = FakePlayerFactory.get(server, new GameProfile(owner.ownerId(), owner.ownerName()));
        if (DemonWillRitualEffects.supports(ritual)) {
            DemonWillRitualEffects.perform(this, server, actor);
            return;
        }
        switch (ritual) {
            case WATER -> {
                BlockPos target = worldPosition.above();
                if (WorldAccess.loaded(server, target) && server.isEmptyBlock(target)
                        && !server.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, target)
                        && WorldAccess.mayPlace(server, target, Blocks.WATER.defaultBlockState(), actor))
                    paid(server, () -> server.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState()));
            }
            case GREEN_GROVE -> {
                for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-1, 2, -1), worldPosition.offset(1, 2, 1))) {
                    if (!WorldAccess.loaded(server, p)) return;
                    BlockState before = server.getBlockState(p);
                    if (!(before.getBlock() instanceof CropBlock crop) || crop.isMaxAge(before) || server.getRandom().nextDouble() >= .3) continue;
                    if (!WorldAccess.mayPlace(server, p, before, actor)) continue;
                    paid(server, () -> { before.randomTick(server, p, server.getRandom()); return !server.getBlockState(p).equals(before); });
                }
            }
            case CRUSHING -> {
                for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-1, -3, -1), worldPosition.offset(1, -1, 1))) {
                    if (!WorldAccess.loaded(server, p)) return;
                    BlockState before = server.getBlockState(p);
                    if (before.isAir() || !before.getFluidState().isEmpty() || before.getDestroySpeed(server, p) < 0 || before.hasBlockEntity()
                            || RitualRune.isRune(before) || before.is(BloodMagicBlocks.MASTER_RITUAL_STONE.get())) continue;
                    if (!WorldAccess.mayBreak(server, p, actor)) continue;
                    var drops = Block.getDrops(before, server, p, null, actor, new ItemStack(Items.DIAMOND_PICKAXE));
                    if (paid(server, () -> server.destroyBlock(p, false, actor))) { for (var stack : drops) deliver(server, stack); break; }
                }
            }
            case ANIMAL_GROWTH -> paid(server, () -> {
                boolean changed = false;
                for (Animal animal : server.getEntitiesOfClass(Animal.class, area(worldPosition, 2, 2, 2))) {
                    if (animal.isBaby()) { animal.setAge(0); changed = true; }
                }
                return changed;
            });
            case CONDOR -> paid(server, () -> affectPlayers(server, 15, 30, 15, player -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, true, false)); return true;
            }));
            case CRYSTAL_HARVEST -> paid(server, () -> {
                for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-3, 2, -3), worldPosition.offset(3, 6, 3)))
                    if (server.getBlockEntity(p) instanceof DemonCrystalBlockEntity crystal && crystal.dropSingleCrystal()) return true;
                return false;
            });
            case CRYSTAL_SPLIT -> paid(server, () -> splitCrystal(server));
            case ELLIPSOID, SPHERE -> paid(server, () -> buildFromChest(server, ritual == BasicRitual.SPHERE, actor));
            case FEATHERED_KNIFE -> paid(server, () -> sacrificeNearby(server));
            case FELLING -> paid(server, () -> fellTree(server, actor));
            case FORSAKEN_SOUL -> paid(server, () -> {
                for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-3, -7, -3), worldPosition.offset(3, -2, 3)))
                    if (server.getBlockEntity(p) instanceof DemonCrystalBlockEntity crystal && crystal.getInjectedWill() < 400) {
                        crystal.applyCatalyst(200, 10, 25); return true;
                    }
                return false;
            });
            case FULL_STOMACH -> paid(server, () -> affectPlayers(server, 8, 8, 8, player -> {
                player.getFoodData().eat(20, 1.0F); return true;
            }));
            case GEODE -> paid(server, () -> createGeode(server));
            case GROUNDING -> paid(server, () -> extinguishAround(server));
            case HARVEST -> paid(server, () -> harvestCrops(server, actor));
            case JUMPING -> paid(server, () -> {
                boolean changed = false;
                for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, area(worldPosition, 0, 5, 0))) {
                    entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 1, true, false)); changed = true;
                }
                return changed;
            });
            case LAVA -> {
                BlockPos target = worldPosition.above();
                if (WorldAccess.loaded(server, target) && server.isEmptyBlock(target)
                        && WorldAccess.mayPlace(server, target, Blocks.LAVA.defaultBlockState(), actor))
                    paid(server, () -> server.setBlockAndUpdate(target, Blocks.LAVA.defaultBlockState()));
            }
            case MAGNETISM -> paid(server, () -> {
                boolean changed = false;
                for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, area(worldPosition, 1, 1, 1))) {
                    item.setPos(worldPosition.getX() + .5, worldPosition.getY() + 1.2, worldPosition.getZ() + .5); changed = true;
                }
                return changed;
            });
            case METEOR -> paid(server, () -> {
                BlockPos target = worldPosition.above(40);
                if (!server.isInWorldBounds(target) || !WorldAccess.loaded(server, target)) return false;
                var falling = FallingBlockEntity.fall(server, target, Blocks.MAGMA_BLOCK.defaultBlockState());
                falling.setHurtsEntities(6.0F, 40); return true;
            });
            case PLACER -> paid(server, () -> placeFromChest(server, actor));
            case REGENERATION -> paid(server, () -> {
                boolean changed = false;
                for (Player player : server.getEntitiesOfClass(Player.class, area(worldPosition, 15, 15, 15)))
                    if (player.getHealth() < player.getMaxHealth()) { player.heal(2); changed = true; }
                for (LivingEntity mob : server.getEntitiesOfClass(LivingEntity.class, area(worldPosition, 15, 15, 15))) {
                    if (mob instanceof Player || mob.getHealth() <= 0) continue;
                    mob.hurtServer(server, mob.damageSources().magic(), 2); changed = true;
                }
                return changed;
            });
            case SPEED -> paid(server, () -> affectPlayers(server, 2, 5, 2, player -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, true, false)); return true;
            }));
            case WELL_OF_SUFFERING -> paid(server, () -> hurtMobsIntoAltar(server));
            case YAWNING_VOID -> paid(server, () -> quarry(server, actor));
            case ZEPHYR -> paid(server, () -> pullItemsToChest(server));
            case CRAFTING -> paid(server, () -> craftFromChest(server));
            case ARMOUR_EVOLVE, DOWNGRADE, SIMPLE_DUNGEON, STANDARD_DUNGEON, UPGRADE_REMOVE -> { }
        }
    }
    private AABB area(BlockPos center, int x, int y, int z) { return new AABB(center).inflate(x, y, z); }
    private boolean affectPlayers(ServerLevel server, int x, int y, int z, java.util.function.Predicate<Player> action) {
        boolean changed = false;
        for (Player player : server.getEntitiesOfClass(Player.class, area(worldPosition, x, y, z))) if (action.test(player)) changed = true;
        return changed;
    }
    private boolean splitCrystal(ServerLevel server) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-3, 2, -3), worldPosition.offset(3, 6, 3))) {
            if (!server.getBlockState(p).is(BloodMagicBlocks.RAW_DEMON_CRYSTAL.get())) continue;
            if (server.getBlockEntity(p) instanceof DemonCrystalBlockEntity c && c.getCrystalCount() < 7) continue;
            var types = new Block[]{BloodMagicBlocks.CORROSIVE_DEMON_CRYSTAL.get(), BloodMagicBlocks.DESTRUCTIVE_DEMON_CRYSTAL.get(),
                    BloodMagicBlocks.STEADFAST_DEMON_CRYSTAL.get(), BloodMagicBlocks.VENGEFUL_DEMON_CRYSTAL.get()};
            var state = server.getBlockState(p); int age = state.getValue(BlockDemonCrystal.AGE);
            server.setBlockAndUpdate(p, types[Math.floorMod(p.hashCode(), types.length)].defaultBlockState()
                    .setValue(BlockDemonCrystal.AGE, Math.max(0, age / 2)).setValue(BlockDemonCrystal.ATTACHED, state.getValue(BlockDemonCrystal.ATTACHED)));
            return true;
        }
        return false;
    }
    private boolean buildFromChest(ServerLevel server, boolean sphere, ServerPlayer actor) {
        ItemStack block = peekChestBlock(server);
        if (block.isEmpty() || !(block.getItem() instanceof BlockItem bi)) return false;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-4, -1, -4), worldPosition.offset(4, 4, 4))) {
            if (!server.isEmptyBlock(p) || sphere && p.distToCenterSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) > 18) continue;
            if (!WorldAccess.mayPlace(server, p, bi.getBlock().defaultBlockState(), actor)) continue;
            server.setBlockAndUpdate(p, bi.getBlock().defaultBlockState()); takeChestBlock(server); return true;
        }
        return false;
    }
    private boolean sacrificeNearby(ServerLevel server) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-5, -10, -5), worldPosition.offset(5, 10, 5)))
            if (server.getBlockEntity(p) instanceof BloodAltarBlockEntity altar)
                for (Player player : server.getEntitiesOfClass(Player.class, area(worldPosition, 15, 20, 15)))
                    if (altar.sacrificeHealth(player) > 0) return true;
        return false;
    }
    private boolean fellTree(ServerLevel server, ServerPlayer actor) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-10, -3, -10), worldPosition.offset(11, 27, 11))) {
            var state = server.getBlockState(p);
            if (!state.is(net.minecraft.tags.BlockTags.LOGS) || !WorldAccess.mayBreak(server, p, actor)) continue;
            var drops = Block.getDrops(state, server, p, null, actor, new ItemStack(Items.DIAMOND_AXE));
            server.destroyBlock(p, false, actor); drops.forEach(stack -> deliver(server, stack)); return true;
        }
        return false;
    }
    private boolean createGeode(ServerLevel server) {
        BlockPos center = worldPosition.below(6); boolean changed = false;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
            double d = Math.sqrt(p.distToCenterSqr(center.getX() + .5, center.getY() + .5, center.getZ() + .5));
            if (d <= 2.2) server.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            else if (d <= 3.4) server.setBlockAndUpdate(p, Blocks.CALCITE.defaultBlockState());
            else if (d < 4) server.setBlockAndUpdate(p, Blocks.SMOOTH_BASALT.defaultBlockState()); else continue;
            changed = true;
        }
        return changed;
    }
    private boolean extinguishAround(ServerLevel server) {
        boolean changed = false;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-8, -8, -8), worldPosition.offset(8, 8, 8)))
            if (server.getBlockState(p).is(Blocks.FIRE)) { server.removeBlock(p, false); changed = true; }
        return changed;
    }
    private boolean harvestCrops(ServerLevel server, ServerPlayer actor) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-3, -1, -3), worldPosition.offset(3, 1, 3))) {
            var state = server.getBlockState(p);
            if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) continue;
            var drops = Block.getDrops(state, server, p, null, actor, new ItemStack(Items.DIAMOND_HOE));
            server.setBlockAndUpdate(p, state.setValue(CropBlock.AGE, 0)); drops.forEach(stack -> deliver(server, stack)); return true;
        }
        return false;
    }
    private boolean placeFromChest(ServerLevel server, ServerPlayer actor) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-2, 1, -2), worldPosition.offset(2, 1, 2))) {
            ItemStack stack = peekChestBlock(server);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi) || !server.isEmptyBlock(p)
                    || !WorldAccess.mayPlace(server, p, bi.getBlock().defaultBlockState(), actor)) continue;
            server.setBlockAndUpdate(p, bi.getBlock().defaultBlockState()); takeChestBlock(server); return true;
        }
        return false;
    }
    private boolean hurtMobsIntoAltar(ServerLevel server) {
        BloodAltarBlockEntity altar = null;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-5, -10, -5), worldPosition.offset(5, 10, 5)))
            if (server.getBlockEntity(p) instanceof BloodAltarBlockEntity found) { altar = found; break; }
        if (altar == null) return false;
        for (LivingEntity mob : server.getEntitiesOfClass(LivingEntity.class, area(worldPosition, 10, 10, 10)))
            if (!(mob instanceof Player) && mob.hurtServer(server, mob.damageSources().magic(), 2)) { altar.fillMainTank(200); return true; }
        return false;
    }
    private boolean quarry(ServerLevel server, ServerPlayer actor) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-1, -3, -1), worldPosition.offset(1, -1, 1))) {
            var state = server.getBlockState(p);
            if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(server, p) < 0 || !WorldAccess.mayBreak(server, p, actor)) continue;
            var drops = Block.getDrops(state, server, p, null, actor, new ItemStack(Items.DIAMOND_PICKAXE));
            server.destroyBlock(p, false, actor); drops.forEach(stack -> deliver(server, stack)); return true;
        }
        return false;
    }
    private boolean pullItemsToChest(ServerLevel server) {
        if (!(server.getBlockEntity(worldPosition.above()) instanceof Container)) return false;
        for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, area(worldPosition, 5, 5, 5))) {
            item.setPos(worldPosition.getX() + .5, worldPosition.getY() + 1.2, worldPosition.getZ() + .5); return true;
        }
        return false;
    }
    private boolean craftFromChest(ServerLevel server) {
        if (!(server.getBlockEntity(worldPosition.above()) instanceof Container chest)) return false;
        java.util.List<ItemStack> grid = new java.util.ArrayList<>(9);
        for (int i = 0; i < 9; i++) grid.add(i < chest.getContainerSize() ? chest.getItem(i).copy() : ItemStack.EMPTY);
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var found = server.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, server);
        if (found.isEmpty()) return false;
        ItemStack result = found.get().value().assemble(input);
        if (result.isEmpty()) return false;
        int[] consumed = new int[9];
        for (int i = 0; i < 9; i++) consumed[i] = grid.get(i).isEmpty() ? 0 : 1;
        for (int i = 0; i < 9; i++) if (consumed[i] > 0) chest.getItem(i).shrink(consumed[i]);
        chest.setChanged();
        if (server.getBlockEntity(worldPosition.above(2)) instanceof Container output) {
            for (int i = 0; i < output.getContainerSize() && !result.isEmpty(); i++) {
                ItemStack slot = output.getItem(i);
                if (!slot.isEmpty() && !ItemStack.isSameItemSameComponents(slot, result)) continue;
                int room = Math.min(output.getMaxStackSize(result), result.getMaxStackSize()) - slot.getCount();
                int count = Math.min(room, result.getCount());
                if (count <= 0) continue;
                if (slot.isEmpty()) output.setItem(i, result.copyWithCount(count)); else slot.grow(count);
                result.shrink(count); output.setChanged();
            }
        }
        if (!result.isEmpty()) deliver(server, result);
        return true;
    }
    private ItemStack peekChestBlock(ServerLevel server) {
        if (!(server.getBlockEntity(worldPosition.above()) instanceof Container chest)) return ItemStack.EMPTY;
        for (int i = 0; i < chest.getContainerSize(); i++) { var stack = chest.getItem(i); if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return stack; }
        return ItemStack.EMPTY;
    }
    private void takeChestBlock(ServerLevel server) {
        if (!(server.getBlockEntity(worldPosition.above()) instanceof Container chest)) return;
        for (int i = 0; i < chest.getContainerSize(); i++) { var stack = chest.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) { stack.shrink(1); chest.setChanged(); return; }
        }
    }    private boolean paid(ServerLevel server,java.util.function.BooleanSupplier action) {
        var networks=SoulNetworks.get(server);
        if(!networks.consume(owner.ownerId(),ritual.cost))return false;
        boolean success=false;
        try {success=action.getAsBoolean();return success;}
        finally {if(!success)networks.fill(owner.ownerId(),ritual.cost,Integer.MAX_VALUE);}
    }
    private void deliver(ServerLevel server,ItemStack stack) {
        BlockPos target=worldPosition.above();
        if(WorldAccess.loaded(server,target) && server.getBlockEntity(target) instanceof Container container) {
            int[] slots=container instanceof WorldlyContainer sided?sided.getSlotsForFace(Direction.DOWN):java.util.stream.IntStream.range(0,container.getContainerSize()).toArray();
            for(int slot:slots) {
                if(!container.canPlaceItem(slot,stack) || container instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot,stack,Direction.DOWN))continue;
                ItemStack existing=container.getItem(slot);
                if(!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,stack))continue;
                int count=Math.min(stack.getCount(),Math.min(container.getMaxStackSize(stack),stack.getMaxStackSize())-existing.getCount());
                if(count<=0)continue;
                if(existing.isEmpty())container.setItem(slot,stack.copyWithCount(count));else existing.grow(count);
                stack.shrink(count);container.setChanged();if(stack.isEmpty())return;
            }
        }
        Containers.dropItemStack(server,target.getX()+.5,target.getY()+.5,target.getZ()+.5,stack);
    }
    private void changed(){setChanged();if(level instanceof ServerLevel)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    public boolean rangeWithinLimitsForEffects() {
        for (RitualRangeSpec spec : ritual.rangeSpecs()) if (!rangeWithinLimits(spec.name())) return false;
        return true;
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        if(owner!=null)out.store("owner",Binding.CODEC,owner);
        out.store("ritual",BasicRitual.CODEC,ritual);
        out.putBoolean("active",active);
        out.putInt("cooldown",cooldown);
        out.putString("direction",direction.getSerializedName());
        if(quarryCursor!=null)out.store("quarry_cursor",BlockPos.CODEC,quarryCursor);
        out.store("ranges",RANGES_CODEC,ranges.entrySet().stream().map(entry -> new SavedRange(entry.getKey(),entry.getValue())).toList());
        for(EnumDemonWillType type:activeWillConfig)out.putBoolean("EnumWill"+type,true);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        owner=in.read("owner",Binding.CODEC).orElse(null);
        var saved=in.read("ritual",BasicRitual.CODEC);
        ritual=saved.orElse(BasicRitual.WATER);
        active=in.getBooleanOr("active",false)&&owner!=null&&saved.isPresent();
        cooldown=Math.clamp(in.getIntOr("cooldown",0),0,Math.max(1,ritual.interval));
        direction=Direction.byName(in.getStringOr("direction","north"));
        if(direction==null||direction.getAxis()==Direction.Axis.Y)direction=Direction.NORTH;
        quarryCursor=in.read("quarry_cursor",BlockPos.CODEC).orElse(null);
        ranges.clear();
        in.read("ranges",RANGES_CODEC).ifPresent(list -> list.forEach(range -> ranges.put(range.name(),range.area().copy())));
        ensureRitualRanges();
        activeWillConfig.clear();
        for(EnumDemonWillType type:EnumDemonWillType.values())if(in.getBooleanOr("EnumWill"+type,false))activeWillConfig.add(type);
    }
    private record SavedRange(String name,RitualArea area) {
        private static final Codec<SavedRange> CODEC=RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(SavedRange::name),
                RitualArea.CODEC.fieldOf("area").forGetter(SavedRange::area)
        ).apply(instance,SavedRange::new));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}




