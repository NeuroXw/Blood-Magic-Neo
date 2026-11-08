package wayoftime.bloodmagic.common.block.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.item.ItemDemonWill;
import wayoftime.bloodmagic.common.registries.BloodMagicWill;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.recipe.AlchemyTableRecipe;
import wayoftime.bloodmagic.recipe.HellfireForgeRecipe;
import wayoftime.bloodmagic.recipe.MachineRecipeInput;
import wayoftime.bloodmagic.ritual.BasicRitual;
import wayoftime.bloodmagic.ritual.RitualArea;
import wayoftime.bloodmagic.ritual.WorldAccess;

/** Faithful gameplay implementation for the twelve upstream rituals that read Demon Will. */
final class DemonWillRitualEffects {
    private static final TagKey<Block> GEODE_HARVESTABLE = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(BloodMagic.MODID, "geode_harvestable"));
    private static final TagKey<Block> GEODE_ACCELERATABLE = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(BloodMagic.MODID, "geode_acceleratable"));
    private static final double EPSILON = 1.0e-9;

    private DemonWillRitualEffects() {
    }

    static boolean supports(BasicRitual ritual) {
        return ritual != null && ritual.usesDemonWillConfig();
    }

    static void perform(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        switch (master.ritual()) {
            case WATER -> water(master, level, actor);
            case GREEN_GROVE -> greenGrove(master, level, actor);
            case CRUSHING -> crushing(master, level, actor);
            case ANIMAL_GROWTH -> animalGrowth(master, level);
            case FEATHERED_KNIFE -> featheredKnife(master, level);
            case GEODE -> geode(master, level, actor);
            case GROUNDING -> grounding(master, level);
            case LAVA -> lava(master, level, actor);
            case REGENERATION -> regeneration(master, level);
            case SPEED -> speed(master, level);
            case CRAFTING -> crafting(master, level, actor);
            case YAWNING_VOID -> yawningVoid(master, level);
            default -> { }
        }
    }

    private static void water(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        if (master.currentLp() < 25) return;
        RitualArea waterArea = master.range("waterRange");
        if (waterArea == null) return;
        double raw = master.configuredWill(EnumDemonWillType.DEFAULT);
        int effects = 0;
        int lp = 0;
        for (BlockPos pos : waterArea.positions(master.getBlockPos())) {
            if (lp + 25 > master.currentLp()) break;
            if (!level.isEmptyBlock(pos)
                    || level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, pos)
                    || !WorldAccess.mayPlace(level, pos, Blocks.WATER.defaultBlockState(), actor)) continue;
            if (level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState())) {
                effects++;
                lp += 25;
            }
        }

        double drained = 0;
        if (raw > 0) {
            int tankLp = Math.max((int) (20 - raw / 10), 0);
            if (master.currentLp() >= lp + tankLp) {
                int filled = fillFluid(master, level, "waterTank", Fluids.WATER, 1000);
                if (filled > 0) {
                    double ratio = filled / 1000.0;
                    drained = Math.min(1, raw / 1000.0) * ratio;
                    lp += (int) Math.ceil(tankLp * ratio);
                }
            }
        }
        if (effects == 0 && drained <= EPSILON) return;
        if (!master.payLp(lp)) return;
        if (drained > 0) master.drainWill(EnumDemonWillType.DEFAULT, drained);
    }

    private static void greenGrove(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        if (master.currentLp() < 20) return;
        RitualArea growArea = master.range("growing");
        if (growArea == null) return;
        double raw = master.configuredWill(EnumDemonWillType.DEFAULT);
        double corrosive = master.configuredWill(EnumDemonWillType.CORROSIVE);
        double steadfast = master.configuredWill(EnumDemonWillType.STEADFAST);
        double vengeful = master.configuredWill(EnumDemonWillType.VENGEFUL);
        double growthChance = vengeful > 0 ? 0.3 + vengeful / 200.0 : 0.3;
        boolean consumeRaw = raw >= 0.05;
        boolean consumeVengeful = vengeful >= 0.05;
        int maxGrowths = master.currentLp() / 20;
        int growths = 0;
        double rawDrain = 0;
        double vengefulDrain = 0;

        for (BlockPos pos : growArea.positions(master.getBlockPos())) {
            if (growths >= maxGrowths) break;
            BlockState state = level.getBlockState(pos);
            boolean growable = state.getBlock() instanceof BonemealableBlock
                    || state.getBlock() instanceof CactusBlock || state.getBlock() instanceof SugarCaneBlock;
            if (!growable || level.getRandom().nextDouble() >= growthChance) continue;
            state.randomTick(level, pos, level.getRandom());
            BlockState grown = level.getBlockState(pos);
            if (grown.equals(state)) continue;
            level.levelEvent(2005, pos, 0);
            growths++;
            if (consumeRaw && raw >= 0.05) { raw -= 0.05; rawDrain += 0.05; }
            if (consumeVengeful && vengeful >= 0.05) { vengeful -= 0.05; vengefulDrain += 0.05; }
        }

        double steadfastDrain = 0;
        RitualArea hydrateArea = master.range("hydrate");
        if (steadfast >= 0.05 && hydrateArea != null) {
            steadfastDrain += plantSeeds(master, level, hydrateArea, actor) * 0.05;
            for (BlockPos pos : hydrateArea.positions(master.getBlockPos())) {
                if (steadfast < 0.05) break;
                BlockState state = level.getBlockState(pos);
                BlockState replacement = null;
                if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
                    replacement = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);
                } else if (state.is(Blocks.FARMLAND) && state.getValue(FarmlandBlock.MOISTURE) < 7) {
                    replacement = state.setValue(FarmlandBlock.MOISTURE, 7);
                }
                if (replacement != null && WorldAccess.mayPlace(level, pos, replacement, actor)) {
                    level.setBlockAndUpdate(pos, replacement);
                    steadfast -= 0.05;
                    steadfastDrain += 0.05;
                }
            }
        }

        double corrosiveDrain = 0;
        RitualArea leechArea = master.range("leech");
        if (corrosive >= 0.2 && leechArea != null) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, leechArea.aabb(master.getBlockPos()))) {
                if (corrosive < 0.2) break;
                if (entity instanceof Player || entity.hasEffect(BloodMagicWill.PLANT_LEECH)) continue;
                entity.addEffect(new MobEffectInstance(BloodMagicWill.PLANT_LEECH, 200, 0));
                corrosive -= 0.2;
                corrosiveDrain += 0.2;
            }
        }

        int lp = growths * 20;
        if (growths == 0 && rawDrain <= EPSILON && vengefulDrain <= EPSILON
                && steadfastDrain <= EPSILON && corrosiveDrain <= EPSILON) return;
        if (!master.payLp(lp)) return;
        if (rawDrain > 0) master.drainWill(EnumDemonWillType.DEFAULT, rawDrain);
        if (vengefulDrain > 0) master.drainWill(EnumDemonWillType.VENGEFUL, vengefulDrain);
        if (steadfastDrain > 0) master.drainWill(EnumDemonWillType.STEADFAST, steadfastDrain);
        if (corrosiveDrain > 0) master.drainWill(EnumDemonWillType.CORROSIVE, corrosiveDrain);
    }

    private static int plantSeeds(MasterRitualBlockEntity master, ServerLevel level, RitualArea area, ServerPlayer actor) {
        int planted = 0;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area.aabb(master.getBlockPos()))) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)
                    || !(blockItem.getBlock() instanceof CropBlock)) continue;
            for (BlockPos pos : area.positions(master.getBlockPos())) {
                BlockPos soil = pos.below();
                if (!level.getBlockState(soil).is(Blocks.FARMLAND) || !level.isEmptyBlock(pos)
                        || !WorldAccess.mayPlace(level, pos, blockItem.getBlock().defaultBlockState(), actor)) continue;
                level.setBlockAndUpdate(pos, blockItem.getBlock().defaultBlockState());
                stack.shrink(1);
                if (stack.isEmpty()) itemEntity.discard();
                planted++;
                break;
            }
        }
        return planted;
    }

    private static void crushing(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        if (master.currentLp() < 7) return;
        Container chest = container(master, level, "chest");
        if (chest == null || !hasRoom(chest)) return;
        RitualArea crushingArea = master.range("crushingRange");
        if (crushingArea == null) return;
        boolean silk = master.configuredWill(EnumDemonWillType.STEADFAST) >= 0.2;
        boolean fortune = !silk && master.configuredWill(EnumDemonWillType.DESTRUCTIVE) >= 0.2;

        for (BlockPos pos : crushingArea.positions(master.getBlockPos())) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0 || !WorldAccess.mayBreak(level, pos, actor)) continue;
            if (state.is(BloodMagicBlocks.MASTER_RITUAL_STONE.get()) || state.is(BloodMagicBlocks.RITUAL_STONE.get())
                    || state.is(BloodMagicBlocks.WATER_RITUAL_STONE.get()) || state.is(BloodMagicBlocks.EARTH_RITUAL_STONE.get())
                    || state.is(BloodMagicBlocks.FIRE_RITUAL_STONE.get()) || state.is(BloodMagicBlocks.AIR_RITUAL_STONE.get())
                    || state.is(BloodMagicBlocks.DUSK_RITUAL_STONE.get())) continue;
            ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
            if (silk) EnchantmentHelper.setEnchantments(tool, enchantments(level, Enchantments.SILK_TOUCH, 1));
            else if (fortune) EnchantmentHelper.setEnchantments(tool, enchantments(level, Enchantments.FORTUNE, 3));
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, actor, tool);
            level.removeBlock(pos, false);
            for (ItemStack drop : drops) insertOrDrop(level, chest, drop, pos);
            if (!master.payLp(7)) return;
            if (master.configuredWill(EnumDemonWillType.DEFAULT) >= 0.05) master.drainWill(EnumDemonWillType.DEFAULT, 0.05);
            if (silk) master.drainWill(EnumDemonWillType.STEADFAST, 0.2);
            if (fortune) master.drainWill(EnumDemonWillType.DESTRUCTIVE, 0.2);
            return;
        }
    }

    private static void animalGrowth(MasterRitualBlockEntity master, ServerLevel level) {
        if (master.currentLp() < 2) return;
        RitualArea growthArea = master.range("growing");
        if (growthArea == null) return;
        Container chest = container(master, level, "chest");
        double raw = master.configuredWill(EnumDemonWillType.DEFAULT);
        double vengeful = master.configuredWill(EnumDemonWillType.VENGEFUL);
        double steadfast = master.configuredWill(EnumDemonWillType.STEADFAST);
        double destructive = master.configuredWill(EnumDemonWillType.DESTRUCTIVE);
        int maxGrowths = master.currentLp() / 2;
        int growths = 0;
        boolean performed = false;
        double vengefulDrain = 0;
        double steadfastDrain = 0;
        double destructiveDrain = 0;

        for (Animal animal : level.getEntitiesOfClass(Animal.class, growthArea.aabb(master.getBlockPos()))) {
            if (animal.getAge() < 0) {
                animal.ageUp(5);
                growths++;
                performed = true;
            } else if (animal.getAge() > 0 && vengeful >= 0.02) {
                animal.setAge(Math.max(0, animal.getAge() - (10 + (int) (vengeful / 5))));
                vengeful -= 0.02;
                vengefulDrain += 0.02;
                performed = true;
            } else if (animal.getAge() == 0) {
                if (destructive >= 1 && !animal.hasEffect(BloodMagicWill.SACRIFICIAL_LAMB)) {
                    animal.addEffect(new MobEffectInstance(BloodMagicWill.SACRIFICIAL_LAMB, 1200));
                    destructive -= 1;
                    destructiveDrain += 1;
                    performed = true;
                }
                if (steadfast >= 0.1 && chest != null && !animal.isInLove()) {
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                        ItemStack food = chest.getItem(slot);
                        if (food.isEmpty() || !animal.isFood(food)) continue;
                        animal.setInLove(null);
                        food.shrink(1);
                        chest.setChanged();
                        steadfast -= 0.1;
                        steadfastDrain += 0.1;
                        performed = true;
                        break;
                    }
                }
            }
            if (growths >= maxGrowths) break;
        }
        if (!performed) return;
        if (!master.payLp(growths * 2)) return;
        if (raw >= 0.05) master.drainWill(EnumDemonWillType.DEFAULT, 0.05);
        if (vengefulDrain > 0) master.drainWill(EnumDemonWillType.VENGEFUL, vengefulDrain);
        if (steadfastDrain > 0) master.drainWill(EnumDemonWillType.STEADFAST, steadfastDrain);
        if (destructiveDrain > 0) master.drainWill(EnumDemonWillType.DESTRUCTIVE, destructiveDrain);
    }

    private static void featheredKnife(MasterRitualBlockEntity master, ServerLevel level) {
        if (master.currentLp() < 20) return;
        RitualArea altarArea = master.range("altar");
        RitualArea damageArea = master.range("damage");
        if (altarArea == null || damageArea == null) return;
        BloodAltarBlockEntity altar = null;
        for (BlockPos pos : altarArea.positions(master.getBlockPos())) {
            if (level.getBlockEntity(pos) instanceof BloodAltarBlockEntity found) { altar = found; break; }
        }
        if (altar == null) return;
        double destructive = master.configuredWill(EnumDemonWillType.DESTRUCTIVE);
        double raw = master.configuredWill(EnumDemonWillType.DEFAULT);
        float healthThreshold = master.configuredWill(EnumDemonWillType.STEADFAST) >= 10 ? 0.7F : 0.3F;
        boolean vengeful = master.configuredWill(EnumDemonWillType.VENGEFUL) >= 10;
        boolean incense = master.configuredWill(EnumDemonWillType.CORROSIVE) >= 10;
        int maxEffects = master.currentLp() / 20;
        int effects = 0;
        double destructiveDrain = 0;
        Binding owner = master.owner();
        for (Player player : level.getEntitiesOfClass(Player.class, damageArea.aabb(master.getBlockPos()))) {
            if (effects >= maxEffects) break;
            if (incense && player.hasEffect(BloodMagicWill.SOUL_FRAY)) continue;
            float threshold = owner != null && vengeful && !player.getUUID().equals(owner.ownerId()) ? 0.1F : healthThreshold;
            if (player.getHealth() / player.getMaxHealth() <= threshold) continue;
            float sacrificed = player.getHealth() - player.getMaxHealth() * threshold;
            if (!(sacrificed > 0)) continue;
            double modifier = 1;
            if (destructive >= 0.05 * sacrificed) {
                modifier *= 1 + destructive * 0.2 / 100.0;
                destructive -= 0.05 * sacrificed;
                destructiveDrain += 0.05 * sacrificed;
            }
            if (incense) player.addEffect(new MobEffectInstance(BloodMagicWill.SOUL_FRAY, 1200));
            player.setHealth(player.getHealth() - sacrificed);
            altar.fillMainTank((int) Math.floor(100 * sacrificed * modifier));
            effects++;
        }
        if (effects == 0) return;
        if (!master.payLp(effects * 20)) return;
        if (destructiveDrain > 0) master.drainWill(EnumDemonWillType.DESTRUCTIVE, destructiveDrain);
        if (raw >= 0.05) master.drainWill(EnumDemonWillType.DEFAULT, 0.05);
    }

    private static void geode(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        if (master.currentLp() < 50) return;
        RitualArea harvestArea = master.range("harvest");
        if (harvestArea == null) return;
        Container chest = container(master, level, "chest");
        double raw = master.configuredWill(EnumDemonWillType.DEFAULT);
        double destructive = master.configuredWill(EnumDemonWillType.DESTRUCTIVE);
        double steadfast = master.configuredWill(EnumDemonWillType.STEADFAST);
        double corrosive = master.configuredWill(EnumDemonWillType.CORROSIVE);
        double vengeful = master.configuredWill(EnumDemonWillType.VENGEFUL);
        boolean doStore = raw > 50 && chest != null;
        boolean doFortune = destructive > 50;
        boolean doSilk = steadfast > 50;
        double rawDrain = 0;
        double destructiveDrain = 0;
        double steadfastDrain = 0;
        double corrosiveDrain = 0;
        double vengefulDrain = 0;

        for (BlockPos pos : harvestArea.positions(master.getBlockPos())) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(GEODE_HARVESTABLE)) continue;
            if (doFortune && destructive < 1) continue;
            if (doSilk && steadfast < 1) continue;
            ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
            if (doSilk) EnchantmentHelper.setEnchantments(tool, enchantments(level, Enchantments.SILK_TOUCH, 1));
            else if (doFortune) EnchantmentHelper.setEnchantments(tool, enchantments(level, Enchantments.FORTUNE, 3));
            List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), actor, tool);
            if (!WorldAccess.mayBreak(level, pos, actor)) continue;
            level.removeBlock(pos, false);
            if (doSilk) { steadfast -= 1; steadfastDrain += 1; }
            else if (doFortune) { destructive -= 1; destructiveDrain += 1; }
            for (ItemStack drop : drops) {
                if (doStore && raw >= 1) {
                    insertOrDrop(level, chest, drop, pos);
                    raw -= 1;
                    rawDrain += 1;
                } else dropStack(level, pos, drop);
            }
        }

        int harmed = 0;
        RitualArea harmArea = master.range("harm");
        if (vengeful > 50 && harmArea != null) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, harmArea.aabb(master.getBlockPos()))) {
                if (harmed >= 8 || vengeful < 0.125) break;
                if (entity instanceof Player) continue;
                if (entity.hurtServer(level, level.damageSources().magic(), 2)) {
                    vengeful -= 0.125;
                    vengefulDrain += 0.125;
                    harmed++;
                }
            }
        }

        RitualArea accelerationArea = master.range("acceleration");
        if (accelerationArea != null) {
            for (BlockPos pos : accelerationArea.positions(master.getBlockPos())) {
                BlockState state = level.getBlockState(pos);
                if (!state.is(GEODE_ACCELERATABLE) && !state.is(Blocks.BUDDING_AMETHYST)) continue;
                for (int i = 0; i < harmed; i++) state.randomTick(level, pos, level.getRandom());
                if (corrosive > 50 && corrosive >= 0.5) {
                    state.randomTick(level, pos, level.getRandom());
                    state.randomTick(level, pos, level.getRandom());
                    corrosive -= 0.5;
                    corrosiveDrain += 0.5;
                }
            }
        }

        if (!master.payLp(50)) return;
        if (rawDrain > 0) master.drainWill(EnumDemonWillType.DEFAULT, rawDrain);
        if (destructiveDrain > 0) master.drainWill(EnumDemonWillType.DESTRUCTIVE, destructiveDrain);
        if (steadfastDrain > 0) master.drainWill(EnumDemonWillType.STEADFAST, steadfastDrain);
        if (corrosiveDrain > 0) master.drainWill(EnumDemonWillType.CORROSIVE, corrosiveDrain);
        if (vengefulDrain > 0) master.drainWill(EnumDemonWillType.VENGEFUL, vengefulDrain);
    }

    private static void grounding(MasterRitualBlockEntity master, ServerLevel level) {
        RitualArea area = master.range("groundingRange");
        if (area == null) return;
        int cost = Math.max(1, area.volume() / 10000);
        if (master.currentLp() < cost) return;
        double raw = master.configuredWill(EnumDemonWillType.DEFAULT);
        double corrosive = master.configuredWill(EnumDemonWillType.CORROSIVE);
        double destructive = master.configuredWill(EnumDemonWillType.DESTRUCTIVE);
        double steadfast = master.configuredWill(EnumDemonWillType.STEADFAST);
        double vengeful = master.configuredWill(EnumDemonWillType.VENGEFUL);
        double rawDrain = 0, corrosiveDrain = 0, destructiveDrain = 0, steadfastDrain = 0, vengefulDrain = 0;
        int effects = 0;
        int maxEffects = master.currentLp() / cost;
        long time = level.getGameTime();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area.aabb(master.getBlockPos()))) {
            if (effects >= maxEffects) break;
            if (entity instanceof Player player && player.isCreative()) continue;
            effects++;
            if (entity instanceof Player) {
                if (time % 10 != 0 || raw < 0.1) continue;
                raw -= 0.1;
                rawDrain += 0.1;
            } else if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                if (steadfast < 0.1) continue;
                steadfast -= 0.1;
                steadfastDrain += 0.1;
            } else if (time % 10 != 0) continue;

            if (corrosive >= 0.1) {
                entity.addEffect(new MobEffectInstance(BloodMagicWill.SUSPENDED, 20, 0));
                corrosive -= 0.1;
                corrosiveDrain += 0.1;
            } else if (vengeful >= 0.1) {
                entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 10));
                vengeful -= 0.1;
                vengefulDrain += 0.1;
            } else {
                entity.addEffect(new MobEffectInstance(BloodMagicWill.GROUNDED, 20, 0));
                entity.addEffect(new MobEffectInstance(BloodMagicWill.GRAVITY, 20, 0));
            }
            if (destructive >= 0.1) {
                entity.addEffect(new MobEffectInstance(BloodMagicWill.HEAVY_HEART, 100, 1));
                destructive -= 0.1;
                destructiveDrain += 0.1;
            }
        }
        if (effects == 0) return;
        if (!master.payLp(cost * effects)) return;
        if (rawDrain > 0) master.drainWill(EnumDemonWillType.DEFAULT, rawDrain);
        if (corrosiveDrain > 0) master.drainWill(EnumDemonWillType.CORROSIVE, corrosiveDrain);
        if (destructiveDrain > 0) master.drainWill(EnumDemonWillType.DESTRUCTIVE, destructiveDrain);
        if (steadfastDrain > 0) master.drainWill(EnumDemonWillType.STEADFAST, steadfastDrain);
        if (vengefulDrain > 0) master.drainWill(EnumDemonWillType.VENGEFUL, vengefulDrain);
    }

    private static void lava(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        RitualArea lavaArea = master.range("lavaRange");
        if (lavaArea == null) return;
        double raw = master.configuredWill(EnumDemonWillType.DEFAULT);
        int lp = 0;
        double rawDrained = 0;
        for (BlockPos pos : lavaArea.positions(master.getBlockPos())) {
            int cost = Math.max((int) (500 - raw), 0);
            if (master.currentLp() < lp + cost) break;
            BlockState state = level.getBlockState(pos);
            if (!level.isEmptyBlock(pos) && !state.getFluidState().isEmpty() && !state.getFluidState().isSource()) continue;
            if (!WorldAccess.mayPlace(level, pos, Blocks.LAVA.defaultBlockState(), actor)) continue;
            level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
            lp += cost;
            if (raw > 0) {
                double drain = Math.min(1, raw / 500.0);
                raw -= drain;
                rawDrained += drain;
            }
        }

        if (raw > 0) {
            int cost = Math.max((int) (500 - raw), 0);
            if (master.currentLp() >= lp + cost) {
                int filled = fillFluid(master, level, "lavaTank", Fluids.LAVA, 1000);
                if (filled > 0) {
                    double ratio = filled / 1000.0;
                    rawDrained += Math.min(1, raw / 500.0) * ratio;
                    lp += (int) Math.ceil(cost * ratio);
                }
            }
        }

        double vengeful = master.configuredWill(EnumDemonWillType.VENGEFUL);
        RitualArea fuseArea = master.range("fireFuse");
        if (vengeful >= 1 && fuseArea != null) {
            double drained = 0;
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, fuseArea.aabb(master.getBlockPos()))) {
                if (vengeful < 1) break;
                if (entity instanceof Player || entity.hasEffect(BloodMagicWill.FIRE_FUSE)) continue;
                entity.addEffect(new MobEffectInstance(BloodMagicWill.FIRE_FUSE, 100, 0));
                vengeful -= 1;
                drained += 1;
            }
            if (drained > 0) master.drainWill(EnumDemonWillType.VENGEFUL, drained);
        }

        double steadfast = master.configuredWill(EnumDemonWillType.STEADFAST);
        RitualArea resistArea = master.range("fireResist");
        if (steadfast >= 0.5 && resistArea != null) {
            double drained = 0;
            for (Player player : level.getEntitiesOfClass(Player.class, resistArea.aabb(master.getBlockPos()))) {
                if (steadfast < 0.5) break;
                var effect = player.getEffect(MobEffects.FIRE_RESISTANCE);
                if (effect != null && effect.getDuration() >= 2) continue;
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));
                steadfast -= 0.5;
                drained += 0.5;
            }
            if (drained > 0) master.drainWill(EnumDemonWillType.STEADFAST, drained);
        }

        double corrosive = master.configuredWill(EnumDemonWillType.CORROSIVE);
        RitualArea damageArea = master.range("fireDamage");
        if (corrosive >= 0.2 && damageArea != null && level.getGameTime() % 20 == 0) {
            double drained = 0;
            float damage = (float) (1 + corrosive * 0.05);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, damageArea.aabb(master.getBlockPos()))) {
                if (corrosive < 0.2) break;
                if (!entity.isAlive() || entity.fireImmune()) continue;
                if (entity.hurtServer(level, level.damageSources().magic(), damage)) {
                    corrosive -= 0.2;
                    drained += 0.2;
                }
            }
            if (drained > 0) master.drainWill(EnumDemonWillType.CORROSIVE, drained);
        }

        if (lp == 0 && rawDrained <= EPSILON) return;
        if (!master.payLp(lp)) return;
        if (rawDrained > 0) master.drainWill(EnumDemonWillType.DEFAULT, rawDrained);
    }

    private static void regeneration(MasterRitualBlockEntity master, ServerLevel level) {
        if (master.currentLp() < 100) return;
        RitualArea healArea = master.range("heal");
        RitualArea vampireArea = master.range("vampire");
        if (healArea == null || vampireArea == null) return;
        double corrosive = master.configuredWill(EnumDemonWillType.CORROSIVE);
        double corrosiveDrain = 0;
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, vampireArea.aabb(master.getBlockPos()))
                .stream().filter(entity -> !(entity instanceof Player)).toList();

        if (corrosive >= 0.04) {
            for (Player player : level.getEntitiesOfClass(Player.class, healArea.aabb(master.getBlockPos()))) {
                if (corrosive < 0.04 || player.getHealth() > player.getMaxHealth() - 1) continue;
                for (LivingEntity enemy : enemies) {
                    if (!enemy.isAlive()) continue;
                    float before = enemy.getHealth();
                    float amount = Math.min(player.getMaxHealth() - player.getHealth(), 1);
                    enemy.hurtServer(level, level.damageSources().magic(), amount);
                    float stolen = before - enemy.getHealth();
                    if (stolen > 0) {
                        player.heal(stolen);
                        corrosive -= 0.04;
                        corrosiveDrain += 0.04;
                        break;
                    }
                }
            }
        }

        int cost = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, healArea.aabb(master.getBlockPos()))) {
            if (entity.getHealth() > entity.getMaxHealth() - 1
                    || !entity.canBeAffected(new MobEffectInstance(MobEffects.REGENERATION))) continue;
            int entityCost = entity instanceof Player ? 100 : 10;
            if (master.currentLp() < cost + entityCost) break;
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, false, false));
            cost += entityCost;
        }
        if (cost == 0 && corrosiveDrain <= EPSILON) return;
        if (!master.payLp(cost)) return;
        if (corrosiveDrain > 0) master.drainWill(EnumDemonWillType.CORROSIVE, corrosiveDrain);
    }

    private static void speed(MasterRitualBlockEntity master, ServerLevel level) {
        if (master.currentLp() < 5) return;
        RitualArea area = master.range("sanicRange");
        if (area == null) return;
        double raw = master.configuredWill(EnumDemonWillType.DEFAULT);
        double corrosive = master.configuredWill(EnumDemonWillType.CORROSIVE);
        double destructive = master.configuredWill(EnumDemonWillType.DESTRUCTIVE);
        double vengeful = master.configuredWill(EnumDemonWillType.VENGEFUL);
        double steadfast = master.configuredWill(EnumDemonWillType.STEADFAST);
        double rawDrain = 0, corrosiveDrain = 0, destructiveDrain = 0, vengefulDrain = 0, steadfastDrain = 0;
        boolean any = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area.aabb(master.getBlockPos()))) {
            if (entity.isShiftKeyDown()) continue;
            boolean transportChildren = destructive < 0.05;
            boolean transportAdults = vengeful < 0.05;
            if (entity.isBaby() && !transportChildren || !entity.isBaby() && !transportAdults) continue;
            if (entity instanceof Player && (transportChildren ^ transportAdults)) continue;
            if (!transportChildren) { destructive -= 0.05; destructiveDrain += 0.05; }
            if (!transportAdults) { vengeful -= 0.05; vengefulDrain += 0.05; }
            double speed = 3 + raw / 40.0;
            if (raw >= 0.1) { raw -= 0.1; rawDrain += 0.1; }
            if (corrosive >= 0.05) {
                corrosive -= 0.05;
                corrosiveDrain += 0.05;
                speed += corrosive / 40.0;
            }
            double x = 0, z = 0;
            switch (master.direction()) {
                case NORTH -> z = -speed;
                case SOUTH -> z = speed;
                case WEST -> x = -speed;
                case EAST -> x = speed;
                default -> { }
            }
            entity.fallDistance = 0;
            entity.setDeltaMovement(x, 1.2 + raw / 200.0, z);
            if (entity instanceof ServerPlayer player) player.hurtMarked = true;
            if (steadfast >= 0.05) {
                entity.addEffect(new MobEffectInstance(BloodMagicWill.SOFT_FALL, 100, 0));
                steadfast -= 0.05;
                steadfastDrain += 0.05;
            }
            any = true;
        }
        if (!any || !master.payLp(5)) return;
        if (rawDrain > 0) master.drainWill(EnumDemonWillType.DEFAULT, rawDrain);
        if (corrosiveDrain > 0) master.drainWill(EnumDemonWillType.CORROSIVE, corrosiveDrain);
        if (destructiveDrain > 0) master.drainWill(EnumDemonWillType.DESTRUCTIVE, destructiveDrain);
        if (vengefulDrain > 0) master.drainWill(EnumDemonWillType.VENGEFUL, vengefulDrain);
        if (steadfastDrain > 0) master.drainWill(EnumDemonWillType.STEADFAST, steadfastDrain);
    }

    private static void crafting(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        if (master.currentLp() < 10) return;
        List<EnumDemonWillType> config = master.activeWillConfig();
        if (config.contains(EnumDemonWillType.STEADFAST)) {
            if (master.configuredWill(EnumDemonWillType.STEADFAST) < 0.1) return;
            if (craftAtHellfireForge(master, level, actor)) {
                master.drainWill(EnumDemonWillType.STEADFAST, 0.1);
            }
            return;
        }
        if (config.contains(EnumDemonWillType.CORROSIVE)) {
            if (master.configuredWill(EnumDemonWillType.CORROSIVE) < 0.1) return;
            if (craftAtAlchemyTable(master, level, actor)) {
                master.drainWill(EnumDemonWillType.CORROSIVE, 0.1);
            }
            return;
        }
        if (craftVanilla(master, level, actor)) master.payLp(10);
    }

    private static boolean craftVanilla(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        Container input = container(master, level, "inputRange");
        Container recipeSource = container(master, level, "recipeRange");
        Container output = container(master, level, "outputRange");
        if (input == null) input = containerAbove(master, level);
        if (input == null) return false;
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) grid.add(i < input.getContainerSize() ? input.getItem(i).copy() : ItemStack.EMPTY);
        var craftingInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var found = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, craftingInput, level);
        if (found.isEmpty()) return false;
        ItemStack result = found.get().value().assemble(craftingInput);
        if (result.isEmpty()) return false;
        if (output != null && !canInsert(output, result)) return false;
        for (int i = 0; i < Math.min(9, input.getContainerSize()); i++) if (!grid.get(i).isEmpty()) input.getItem(i).shrink(1);
        input.setChanged();
        if (output != null) insertOrDrop(level, output, result, master.getBlockPos().above());
        else dropStack(level, master.getBlockPos().above(), result);
        return true;
    }

    private static boolean craftAtHellfireForge(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        BlockPos forgePos = firstRangePosition(master, "hellforgedRange");
        if (forgePos == null || !(level.getBlockEntity(forgePos) instanceof HellfireForgeBlockEntity forge)) return false;
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < 4; i++) inputs.add(forge.getItem(i).copy());
        var input = new MachineRecipeInput(inputs);
        var found = level.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.FORGE_TYPE.get(), input, level);
        if (found.isEmpty()) return false;
        HellfireForgeRecipe recipe = found.get().value();
        ItemStack result = recipe.assemble(input);
        if (result.isEmpty() || !forge.getItem(5).isEmpty() && !forge.getItem(5).is(result.getItem())) return false;
        double cost = recipe.drain();
        for (int i = 0; i < 5 && cost > 0; i++) {
            double available = ItemDemonWill.amount(forge.getItem(i));
            if (available <= 0) continue;
            double paid = Math.min(available, cost);
            ItemDemonWill.setAmount(forge.getItem(i), available - paid);
            cost -= paid;
        }
        if (cost > EPSILON) return false;
        for (int i = 0; i < 4; i++) forge.getItem(i).shrink(1);
        ItemStack existing = forge.getItem(5);
        if (existing.isEmpty()) forge.setItem(5, result);
        else existing.grow(result.getCount());
        forge.setChanged();
        return true;
    }

    private static boolean craftAtAlchemyTable(MasterRitualBlockEntity master, ServerLevel level, ServerPlayer actor) {
        BlockPos tablePos = firstRangePosition(master, "hellforgedRange");
        if (tablePos == null || !(level.getBlockEntity(tablePos) instanceof AlchemyTableBlockEntity table)) return false;
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < 6; i++) inputs.add(table.getItem(i).copy());
        var input = new MachineRecipeInput(inputs);
        var found = level.getServer().getRecipeManager().getRecipeFor(BloodMagicRecipes.TABLE_TYPE.get(), input, level);
        if (found.isEmpty()) return false;
        AlchemyTableRecipe recipe = found.get().value();
        ItemStack result = recipe.assemble(input);
        if (result.isEmpty() || !table.getItem(7).isEmpty() && !table.getItem(7).is(result.getItem())) return false;
        if (!master.payLp(recipe.syphon())) return false;
        for (int i = 0; i < 6; i++) if (!table.getItem(i).isEmpty()) table.getItem(i).shrink(1);
        ItemStack existing = table.getItem(7);
        if (existing.isEmpty()) table.setItem(7, result);
        else existing.grow(result.getCount());
        table.setChanged();
        return true;
    }

    private static void yawningVoid(MasterRitualBlockEntity master, ServerLevel level) {
        if (master.currentLp() < 10) return;
        RitualArea quarryArea = master.range("quarryRange");
        if (quarryArea == null) return;
        boolean move = master.configuredWill(EnumDemonWillType.STEADFAST) >= 0.05;
        boolean filter = master.configuredWill(EnumDemonWillType.CORROSIVE) >= 0.05;
        if (master.activeWillConfig().contains(EnumDemonWillType.STEADFAST) && !move) return;
        if (master.activeWillConfig().contains(EnumDemonWillType.CORROSIVE) && !filter) return;
        BlockPos replacement = null;
        RitualArea placementArea = master.range("placementRange");
        if (move && placementArea != null) {
            for (BlockPos pos : placementArea.positions(master.getBlockPos())) if (level.isEmptyBlock(pos)) { replacement = pos; break; }
        }
        Container filterContainer = filter ? container(master, level, "chest") : null;
        List<BlockPos> positions = quarryArea.positions(master.getBlockPos());
        positions.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed());
        BlockPos cursor = master.quarryCursor();
        int start = 0;
        if (cursor != null) for (int i = 0; i < positions.size(); i++) if (positions.get(i).equals(cursor)) { start = i + 1; break; }
        int checks = 0;
        for (int i = start; i < positions.size() && checks < 100; i++, checks++) {
            BlockPos pos = positions.get(i);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (!WorldAccess.mayBreak(level, pos, master.fakeUser())) continue;
            if (filter && !matchesFilter(filterContainer, state)) continue;
            if (move) {
                if (replacement == null) return;
                level.setBlockAndUpdate(replacement, state);
                level.removeBlock(pos, false);
            } else {
                level.removeBlock(pos, false);
            }
            master.setQuarryCursor(positions.get(Math.min(i + 1, positions.size() - 1)));
            if (!master.payLp(10)) return;
            if (move) master.drainWill(EnumDemonWillType.STEADFAST, 0.05);
            if (filter) master.drainWill(EnumDemonWillType.CORROSIVE, 0.05);
            if (master.configuredWill(EnumDemonWillType.DEFAULT) >= 0.05) master.drainWill(EnumDemonWillType.DEFAULT, 0.05);
            return;
        }
        master.setQuarryCursor(positions.isEmpty() ? null : positions.getFirst());
    }

    private static boolean matchesFilter(Container filter, BlockState state) {
        if (filter == null) return false;
        ItemStack block = new ItemStack(state.getBlock());
        for (int i = 0; i < filter.getContainerSize(); i++) {
            ItemStack candidate = filter.getItem(i);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, block)) return true;
        }
        return false;
    }

    private static int fillFluid(MasterRitualBlockEntity master, ServerLevel level, String rangeName, net.minecraft.world.level.material.Fluid fluid, int amount) {
        BlockPos pos = firstRangePosition(master, rangeName);
        if (pos == null) return 0;
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP);
        if (handler == null) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(FluidResource.of(fluid), amount, transaction);
            if (inserted > 0) transaction.commit();
            return inserted;
        }
    }

    private static Container container(MasterRitualBlockEntity master, ServerLevel level, String rangeName) {
        BlockPos pos = firstRangePosition(master, rangeName);
        return pos != null && level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    private static Container containerAbove(MasterRitualBlockEntity master, ServerLevel level) {
        return level.getBlockEntity(master.getBlockPos().above()) instanceof Container container ? container : null;
    }

    private static BlockPos firstRangePosition(MasterRitualBlockEntity master, String rangeName) {
        RitualArea area = master.range(rangeName);
        if (area == null) return null;
        List<BlockPos> positions = area.positions(master.getBlockPos());
        return positions.isEmpty() ? null : positions.getFirst();
    }

    private static boolean hasRoom(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
    }

    private static boolean canInsert(Container container, ItemStack stack) {
        if (container == null) return true;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existing = container.getItem(i);
            if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) return true;
        }
        return false;
    }

    private static void insertOrDrop(ServerLevel level, Container container, ItemStack stack, BlockPos fallback) {
        if (container != null) {
            int[] slots = container instanceof WorldlyContainer sided
                    ? sided.getSlotsForFace(Direction.DOWN)
                    : java.util.stream.IntStream.range(0, container.getContainerSize()).toArray();
            for (int slot : slots) {
                if (!container.canPlaceItem(slot, stack)) continue;
                ItemStack existing = container.getItem(slot);
                if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) continue;
                int room = Math.min(container.getMaxStackSize(stack), existing.getMaxStackSize()) - existing.getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved <= 0) continue;
                if (existing.isEmpty()) container.setItem(slot, stack.copyWithCount(moved));
                else existing.grow(moved);
                stack.shrink(moved);
                container.setChanged();
                if (stack.isEmpty()) return;
            }
        }
        dropStack(level, fallback, stack);
    }

    private static void dropStack(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) Block.popResource(level, pos, stack);
    }
    private static ItemEnchantments enchantments(ServerLevel level, ResourceKey<Enchantment> key, int levelValue) {
        var holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holder, levelValue);
        return mutable.toImmutable();
    }
}







