package wayoftime.bloodmagic.altar;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.block.enums.BloodRuneType;

/** Coordinates and upgrade slots from upstream AltarTier/AltarUtil (35f0188). */
public final class AltarStructure {
    public enum Kind {
        RUNE, SOLID, GLOWSTONE, BLOODSTONE, BEACON, CRYSTAL;

        private final TagKey<Block> tag = TagKey.create(Registries.BLOCK,
                BloodMagic.rl("altar/" + name().toLowerCase(java.util.Locale.ROOT)));

        boolean matches(BlockState state) {
            return switch (this) {
                case RUNE -> state.getBlock() instanceof IBloodRune;
                case SOLID -> !state.isAir() && !(state.getBlock() instanceof LiquidBlock);
                default -> state.is(tag);
            };
        }
    }

    public record Component(BlockPos offset, Kind kind, boolean upgradeSlot) {}
    public record Scan(int tier, AltarUpgrades upgrades, boolean complete) {}

    private static final List<List<Component>> TIERS = buildTiers();

    private AltarStructure() {}

    public static List<Component> components(int tier) {
        return TIERS.get(tier - 1);
    }

    public static Scan scan(Level level, BlockPos altar) {
        int tier = 1;
        boolean complete = true;
        for (int candidate = 2; candidate <= 6; candidate++) {
            boolean unloaded = false;
            boolean invalid = false;
            for (Component component : components(candidate)) {
                BlockPos pos = altar.offset(component.offset());
                if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) {
                    unloaded = true;
                } else if (!component.kind().matches(level.getBlockState(pos))) {
                    invalid = true;
                    break;
                }
            }
            if (invalid || unloaded) {
                complete = invalid || !unloaded;
                break;
            }
            tier = candidate;
        }
        EnumMap<BloodRuneType, Integer> counts = new EnumMap<>(BloodRuneType.class);
        for (Component component : components(tier)) {
            if (!component.upgradeSlot()) continue;
            BlockPos pos = altar.offset(component.offset());
            if (level.getBlockState(pos).getBlock() instanceof IBloodRune rune) {
                BloodRuneType type = rune.getBloodRune(level, pos);
                if (type != null) {
                    int count = Math.max(0, rune.getRuneCount(level, pos));
                    counts.merge(type, count, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
                }
            }
        }
        return new Scan(tier, new AltarUpgrades(counts), complete);
    }

    private static List<List<Component>> buildTiers() {
        List<List<Component>> tiers = new ArrayList<>();
        tiers.add(List.of());
        List<Component> parts = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) add(parts, x, -1, z, Kind.RUNE, x == 0 || z == 0);
            }
        }
        tiers.add(List.copyOf(parts));
        parts.replaceAll(p -> new Component(p.offset(), p.kind(), true));
        pillars(parts, 3, -1, 0, 1, Kind.GLOWSTONE);
        ring(parts, 3, -2, 2);
        tiers.add(List.copyOf(parts));
        ring(parts, 5, -3, 3);
        pillars(parts, 5, -2, 1, 2, Kind.BLOODSTONE);
        tiers.add(List.copyOf(parts));
        corners(parts, 8, -4, Kind.BEACON);
        ring(parts, 8, -4, 6);
        tiers.add(List.copyOf(parts));
        pillars(parts, 11, -4, 2, 3, Kind.CRYSTAL);
        ring(parts, 11, -5, 9);
        tiers.add(List.copyOf(parts));
        return List.copyOf(tiers);
    }

    private static void ring(List<Component> parts, int radius, int y, int extent) {
        for (int i = -extent; i <= extent; i++) {
            add(parts, radius, y, i, Kind.RUNE, true);
            add(parts, -radius, y, i, Kind.RUNE, true);
            add(parts, i, y, radius, Kind.RUNE, true);
            add(parts, i, y, -radius, Kind.RUNE, true);
        }
    }

    private static void pillars(List<Component> parts, int radius, int bottom, int top, int capY, Kind cap) {
        for (int y = bottom; y <= top; y++) corners(parts, radius, y, Kind.SOLID);
        corners(parts, radius, capY, cap);
    }

    private static void corners(List<Component> parts, int radius, int y, Kind kind) {
        for (int x : new int[]{-radius, radius}) {
            for (int z : new int[]{-radius, radius}) add(parts, x, y, z, kind, false);
        }
    }

    private static void add(List<Component> parts, int x, int y, int z, Kind kind, boolean upgrade) {
        parts.add(new Component(new BlockPos(x, y, z), kind, upgrade));
    }
}
