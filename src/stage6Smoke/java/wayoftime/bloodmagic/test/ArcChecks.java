package wayoftime.bloodmagic.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.DungeonDecorativeBlocks;
import wayoftime.bloodmagic.common.block.entity.AlchemicalReactionChamberBlockEntity;
import wayoftime.bloodmagic.common.item.BloodMagicItems;
import wayoftime.bloodmagic.common.registries.BloodMagicSounds;
import wayoftime.bloodmagic.common.registries.BloodMagicRecipes;
import wayoftime.bloodmagic.recipe.MachineRecipeInput;
import wayoftime.bloodmagic.recipe.PotionRecipe;

/** Stage 4 ARC/flask and stage 6A resource + gameplay checks. */
final class ArcChecks {
    private static final BlockPos POS = new BlockPos(48, 160, 48);
    private static int assertions;
    static int begin(ServerLevel level) {
        int start = assertions;
        check(level.getServer().getRecipeManager().byKey(recipeKey("alchemical_reaction_chamber")).isPresent(), "ARC crafting recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("arc/clay_from_sand")).isPresent(), "ARC processing recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("dungeon_stone")).isPresent(), "dungeon crafting recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("dungeon_brick_stairs")).isPresent(), "dungeon stairs crafting recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("dungeon_brick1_from_dungeon_raw_stonecutting")).isPresent(), "dungeon stonecutting recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("hellforged_block")).isPresent(), "hellforged storage block recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("hellforged_block_to_ingot")).isPresent(), "hellforged block-to-ingot recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("smelting/ingot_from_demonite")).isPresent(), "dungeon ore smelting recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("smelting/blasting_ingot_from_demonite")).isPresent(), "dungeon ore blasting recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("dungeon_metal_vengeful_from_storage_blocks/hellforged_stonecutting")).isPresent(), "dungeon metal variant stonecutting recipe loaded");
        check(resource("/data/c/tags/item/ingots/hellforged.json") && resource("/data/c/tags/item/storage_blocks/hellforged.json"), "hellforged common item tags packaged");
        long dungeonRecipeCount = level.getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().identifier().getNamespace().equals(BloodMagic.MODID))
                .filter(holder -> holder.id().identifier().getPath().startsWith("dungeon_")).count();
        check(dungeonRecipeCount == 216, "all 220 stage 6A-associated recipes load with hellforged material and ore processing recipes: " + dungeonRecipeCount);
        check(level.getServer().getRecipeManager().byKey(recipeKey("flask/speed_boost")).isPresent(), "potion effect recipe loaded");
        check(level.getServer().getRecipeManager().byKey(recipeKey("altar/alchemy_flask")).isPresent(), "altar flask recipe loaded");
        check(BloodMagicSounds.BLEEDING_EDGE.get().location().equals(BloodMagic.rl("bleedingedge")), "complete upstream sound registered");

        check(DungeonDecorativeBlocks.totalRegisteredBlocks() == 135 && DungeonDecorativeBlocks.BLOCK_BY_ID.size() == 134, "stage 6A owns all 135 non-machinery dungeon blocks including pre-existing dungeon metal");
        check(resource("/assets/bloodmagic/blockstates/dungeon_metal.json") && resource("/data/bloodmagic/loot_table/blocks/dungeon_metal.json"), "pre-existing dungeon metal resources remain complete");
        for (var entry : DungeonDecorativeBlocks.BLOCK_BY_ID.entrySet()) {
            check(entry.getValue().get().getDescriptionId().equals("block.bloodmagic." + entry.getKey()), "dungeon lang id " + entry.getKey());
            check(DungeonDecorativeBlocks.ITEM_BY_ID.get(entry.getKey()) != null
                    && DungeonDecorativeBlocks.ITEM_BY_ID.get(entry.getKey()).get().asItem() == entry.getValue().get().asItem(), "dungeon item " + entry.getKey());
            check(resource("/assets/bloodmagic/blockstates/" + entry.getKey() + ".json"), "dungeon blockstate " + entry.getKey());
            check(resource("/assets/bloodmagic/items/" + entry.getKey() + ".json"), "dungeon 26.1 item definition " + entry.getKey());
            check(resource("/data/bloodmagic/loot_table/blocks/" + entry.getKey() + ".json"), "dungeon loot " + entry.getKey());
        }
        check(resource("/assets/bloodmagic/sounds.json") && resource("/assets/bloodmagic/sounds/bleedingedge.ogg")
                && resource("/assets/bloodmagic/sounds/bleedingedge_old.ogg"), "all sound assets packaged");

        clear(level, POS); level.setBlockAndUpdate(POS, BloodMagicBlocks.ALCHEMICAL_REACTION_CHAMBER.get().defaultBlockState());
        AlchemicalReactionChamberBlockEntity arc = (AlchemicalReactionChamberBlockEntity) level.getBlockEntity(POS);
        check(arc != null && arc.getType() == wayoftime.bloodmagic.common.block.entity.BloodMagicBlockEntities.ALCHEMICAL_REACTION_CHAMBER.get(), "ARC block entity");
        arc.setItem(AlchemicalReactionChamberBlockEntity.INPUT_SLOT, new ItemStack(Items.SAND));
        arc.setItem(AlchemicalReactionChamberBlockEntity.TOOL_SLOT, new ItemStack(BloodMagicItems.SIMPLE_CATALYST.get()));
        arc.setItem(AlchemicalReactionChamberBlockEntity.INPUT_BUCKET_SLOT, new ItemStack(Items.WATER_BUCKET));
        AlchemicalReactionChamberBlockEntity.serverTick(level, POS, arc.getBlockState(), arc);
        check(arc.inputFluidAmount() == 1000 && arc.getItem(AlchemicalReactionChamberBlockEntity.OUTPUT_BUCKET_SLOT).is(Items.BUCKET), "ARC water tank accepts bucket without item loss");
        check(arc.progress() == 1 && arc.status() == 0, "ARC recipe and fluid gate start");
        tick(arc, 198); check(arc.progress() == 199 && arc.getItem(1).isEmpty(), "ARC commits only at completion");
        tick(arc, 1); check(arc.progress() == 0 && arc.getItem(1).is(Items.CLAY_BALL) && arc.inputFluidAmount() == 800,
                "ARC atomic item and fluid processing");
        check(arc.getItem(AlchemicalReactionChamberBlockEntity.TOOL_SLOT).isEmpty()
                && arc.getItem(AlchemicalReactionChamberBlockEntity.INPUT_SLOT).is(Items.SAND), "ARC honours consumeingredient=false and consumes the non-damageable catalyst");

        PotionRecipe recipe = level.getServer().getRecipeManager().byKey(recipeKey("flask/speed_boost")).orElseThrow().value()
                instanceof PotionRecipe p ? p : null;
        check(recipe != null && recipe.operation() == PotionRecipe.Operation.EFFECT, "typed potion recipe");
        ItemStack flask = new ItemStack(BloodMagicItems.ALCHEMY_FLASK.get());
        MachineRecipeInput input = new MachineRecipeInput(List.of(new ItemStack(BloodMagicItems.SIMPLE_CATALYST.get()), new ItemStack(Items.SUGAR), flask, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        check(recipe.matches(input, level), "potion recipe unordered match");
        ItemStack potion = recipe.assemble(input);
        var contents = potion.get(DataComponents.POTION_CONTENTS);
        check(potion.is(BloodMagicItems.ALCHEMY_FLASK.get()) && contents != null && contents.hasEffects(), "potion payload survives recipe");
        check(contents.getAllEffects().iterator().next().getEffect().is(MobEffects.SPEED), "potion effect component applied");
        check(potion.getMaxDamage() == 8 && potion.getDamageValue() == 0, "flask eight-use durability");
        ItemStack splash = new ItemStack(BloodMagicItems.ALCHEMY_FLASK_THROWABLE.get());
        splash.set(DataComponents.POTION_CONTENTS, contents);
        check(PotionRecipe.isFlask(splash) && PotionRecipe.isFlask(new ItemStack(Items.SPLASH_POTION)), "flask variants accepted by recipes");
        clear(level, POS);
        return assertions - start;
    }
    private static ResourceKey<Recipe<?>> recipeKey(String path) { return ResourceKey.create(Registries.RECIPE, BloodMagic.rl(path)); }
    private static boolean resource(String path) { return ArcChecks.class.getResource(path) != null; }
    private static void tick(AlchemicalReactionChamberBlockEntity arc, int count) { for (int i = 0; i < count; i++) AlchemicalReactionChamberBlockEntity.serverTick(arc.getLevel(), arc.getBlockPos(), arc.getBlockState(), arc); }
    private static void clear(ServerLevel level, BlockPos pos) { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException("STAGE6_SMOKE FAIL: " + message); assertions++; }
}













