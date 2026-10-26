package wayoftime.bloodmagic.ritual;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

/** Coordinates and unaugmented costs from RitualWater, RitualGreenGrove, RitualCrushing at 35f0188. */
public enum BasicRitual implements StringRepresentable {
    WATER("water",500,25,1), GREEN_GROVE("green_grove",5000,20,20), CRUSHING("crushing",5000,7,40);
    public static final Codec<BasicRitual> CODEC = StringRepresentable.fromEnum(BasicRitual::values);
    private final String id;
    public final int activationCost, cost, interval;
    BasicRitual(String id,int activationCost,int cost,int interval) { this.id=id;this.activationCost=activationCost;this.cost=cost;this.interval=interval; }
    @Override public String getSerializedName() { return id; }
    public record Component(BlockPos offset,RitualRune rune) {}
    public enum Scan { COMPLETE, INVALID, UNLOADED }
    public List<Component> components() {
        var list=new ArrayList<Component>();
        switch(this) {
            case WATER -> corners(list,1,0,RitualRune.WATER);
            case GREEN_GROVE -> { corners(list,1,0,RitualRune.EARTH); parallel(list,1,0,RitualRune.WATER); }
            case CRUSHING -> { parallel(list,1,0,RitualRune.EARTH); parallel(list,2,0,RitualRune.FIRE); corners(list,2,0,RitualRune.DUSK); parallel(list,2,1,RitualRune.AIR); }
        }
        return List.copyOf(list);
    }
    private static void corners(List<Component> list,int r,int y,RitualRune rune) { for(int x:new int[]{-r,r})for(int z:new int[]{-r,r})list.add(new Component(new BlockPos(x,y,z),rune)); }
    private static void parallel(List<Component> list,int r,int y,RitualRune rune) { for(int v:new int[]{-r,r}) {list.add(new Component(new BlockPos(v,y,0),rune));list.add(new Component(new BlockPos(0,y,v),rune));} }
    public Scan scan(Level level,BlockPos master) {
        for(var part:components())if(!WorldAccess.loaded(level,master.offset(part.offset)))return Scan.UNLOADED;
        for(var part:components())if(!level.getBlockState(master.offset(part.offset)).is(part.rune.block()))return Scan.INVALID;
        return Scan.COMPLETE;
    }
}
