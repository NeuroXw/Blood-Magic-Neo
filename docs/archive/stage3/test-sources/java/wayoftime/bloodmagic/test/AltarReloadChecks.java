package wayoftime.bloodmagic.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import wayoftime.bloodmagic.common.block.BloodMagicBlocks;
import wayoftime.bloodmagic.common.block.entity.BloodAltarBlockEntity;

/** Three real datapack reloads, with an unticked fixture so exact accounting is deterministic. */
final class AltarReloadChecks {
    private static CompletableFuture<Void> reload;
    private static BloodAltarBlockEntity altar;
    private static Path recipePath;
    private static int phase;
    private static int assertions;
    private static long deadline;

    static boolean afterTick(ServerLevel level) throws IOException {
        var server = level.getServer();
        if (phase == 0) {
            altar = new BloodAltarBlockEntity(new BlockPos(128, 180, 128), BloodMagicBlocks.BLOOD_ALTAR.get().defaultBlockState());
            altar.setLevel(level);
            altar.setStoredItem(new ItemStack(Items.DIAMOND));
            altar.fillMainTank(2_000);
            for (int i = 0; i < 10; i++) tick();
            check(altar.getAltarState().progress() == 50 && altar.getAltarState().blood() == 1_950, "reload fixture starts with paid progress");
            Path pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("stage3_reload_probe");
            recipePath = pack.resolve("data/bloodmagic/recipe/altar/weakbloodorb.json");
            Files.createDirectories(recipePath.getParent());
            var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
            String version = "[" + format.major() + "," + format.minor() + "]";
            Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Stage 3 isolated reload check\",\"min_format\":"
                    + version + ",\"max_format\":" + version + "}}");
            writeRecipe("minecraft:diamond");
            server.getPackRepository().reload();
            var packs = new ArrayList<>(server.getPackRepository().getSelectedIds());
            if (!packs.contains("file/stage3_reload_probe")) packs.add("file/stage3_reload_probe");
            reload = server.reloadResources(packs);
            deadline = System.nanoTime() + 60_000_000_000L;
            phase = 1;
            return false;
        }
        if (System.nanoTime() > deadline && phase < 4) throw new IllegalStateException("STAGE3_SMOKE FAIL: datapack reload timed out");
        if (!reload.isDone()) return false;
        reload.join();
        if (phase == 1) {
            tick();
            check(altar.getAltarState().requiredBlood() == 1_500 && altar.getAltarState().progress() == 25
                    && altar.getAltarState().blood() == 1_925, "changed recipe definition resets old progress after real reload");
            writeRecipe("minecraft:emerald");
            reload = server.reloadResources(server.getPackRepository().getSelectedIds());
            phase = 2;
            return false;
        }
        if (phase == 2) {
            tick();
            check(altar.findRecipe().isEmpty() && !altar.getAltarState().active() && altar.getAltarState().progress() == 0
                    && altar.getAltarState().blood() == 1_925, "removed matching recipe stops consuming LP after real reload");
            Files.delete(recipePath);
            reload = server.reloadResources(server.getPackRepository().getSelectedIds());
            phase = 3;
            return false;
        }
        if (phase == 3) {
            tick();
            check(altar.getAltarState().requiredBlood() == 2_000 && altar.getAltarState().progress() == 5
                    && altar.getAltarState().blood() == 1_920, "restored original datapack recipe starts cleanly");
            phase = 4;
        }
        return true;
    }

    static int assertions() { return assertions; }

    private static void writeRecipe(String ingredient) throws IOException {
        Files.writeString(recipePath, "{\"type\":\"bloodmagic:altar\",\"input\":\"" + ingredient
                + "\",\"output\":{\"id\":\"minecraft:gold_ingot\"},\"upgradeLevel\":0,\"altarSyphon\":1500,\"consumptionRate\":25,\"drainRate\":1}");
    }

    private static void tick() {
        BloodAltarBlockEntity.serverTick(altar.getLevel(), altar.getBlockPos(), altar.getBlockState(), altar);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("STAGE3_SMOKE FAIL: " + message);
        assertions++;
    }
}
