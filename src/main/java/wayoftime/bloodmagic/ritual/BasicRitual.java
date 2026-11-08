package wayoftime.bloodmagic.ritual;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.will.DemonWillHolder;

/** 32 upstream rituals. Costs and rune geometry from BloodMagic 35f0188. */
public enum BasicRitual implements StringRepresentable {
    WATER("water",500,25,1),    GREEN_GROVE("green_grove",5000,20,20),    CRUSHING("crushing",5000,7,40),    ANIMAL_GROWTH("animal_growth",10000,2,20),    ARMOUR_EVOLVE("armour_evolve",50000,0,1),    CONDOR("condor",1000000,5,10),    CRAFTING("crafting",15000,10,4),    CRYSTAL_HARVEST("crystal_harvest",40000,50,25),    CRYSTAL_SPLIT("crystal_split",20000,1000,20),    ELLIPSOID("ellipsoid",20000,10,1),    FEATHERED_KNIFE("feathered_knife",25000,20,20),    FELLING("felling",20000,10,1),    FORSAKEN_SOUL("forsaken_soul",40000,2,25),    FULL_STOMACH("full_stomach",100000,100,20),    GEODE("geode",50000,50,20),    GROUNDING("grounding",5000,1,1),    HARVEST("harvest",20000,20,5),    JUMPING("jumping",5000,5,1),    LAVA("lava",10000,500,1),    DOWNGRADE("downgrade",10000,10,1),    MAGNETISM("magnetism",5000,50,40),    METEOR("meteor",250000,0,20),    PLACER("placer",5000,50,5),    REGENERATION("regeneration",25000,100,50),    SIMPLE_DUNGEON("simple_dungeon",80000,0,1),    SPEED("speed",1000,5,1),    SPHERE("sphere",20000,10,1),    STANDARD_DUNGEON("standard_dungeon",150000,0,1),    UPGRADE_REMOVE("upgrade_remove",25000,0,1),    WELL_OF_SUFFERING("well_of_suffering",40000,2,25),    YAWNING_VOID("yawning_void",5000,10,10),    ZEPHYR("zephyr",1000,1,1);
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
        case WATER -> {
{
		corners(list, 1, 0, RitualRune.WATER);
	}
        }
        case GREEN_GROVE -> {
{
		corners(list, 1, 0, RitualRune.EARTH);
		parallel(list, 1, 0, RitualRune.WATER);
	}
        }
        case CRUSHING -> {
{
		parallel(list, 1, 0, RitualRune.EARTH);
		parallel(list, 2, 0, RitualRune.FIRE);
		corners(list, 2, 0, RitualRune.DUSK);
		parallel(list, 2, 1, RitualRune.AIR);
	}
        }
        case ANIMAL_GROWTH -> {
{

		parallel(list, 2, 0, RitualRune.DUSK);
		parallel(list, 1, 0, RitualRune.WATER);
		addRune(list, 1, 0, 2, RitualRune.EARTH);
		addRune(list, 1, 0, -2, RitualRune.EARTH);
		addRune(list, -1, 0, 2, RitualRune.EARTH);
		addRune(list, -1, 0, -2, RitualRune.EARTH);
		addRune(list, 2, 0, 1, RitualRune.AIR);
		addRune(list, 2, 0, -1, RitualRune.AIR);
		addRune(list, -2, 0, 1, RitualRune.AIR);
		addRune(list, -2, 0, -1, RitualRune.AIR);
	}
        }
        case ARMOUR_EVOLVE -> {
{
		corners(list, 1, 0, RitualRune.DUSK);
		corners(list, 2, 0, RitualRune.FIRE);
		addOffsetRunes(list, 1, 2, 0, RitualRune.FIRE);
		corners(list, 1, 1, RitualRune.DUSK);
		parallel(list, 4, 0, RitualRune.EARTH);
		corners(list, 1, 3, RitualRune.DUSK);
		parallel(list, 1, 4, RitualRune.EARTH);

		for (int i = 0; i < 4; i++)
		{
			corners(list, 3, i, RitualRune.EARTH);
		}
	}
        }
        case CONDOR -> {
{
		parallel(list, 1, 0, RitualRune.DUSK);
		corners(list, 2, 0, RitualRune.AIR);
		addOffsetRunes(list, 1, 3, 0, RitualRune.EARTH);
		parallel(list, 3, 0, RitualRune.EARTH);
		addOffsetRunes(list, 3, 4, 0, RitualRune.WATER);
		parallel(list, 1, 1, RitualRune.FIRE);
		parallel(list, 2, 1, RitualRune.BLANK);
		parallel(list, 4, 1, RitualRune.BLANK);
		parallel(list, 5, 1, RitualRune.AIR);
		parallel(list, 5, 0, RitualRune.DUSK);

		for (int i = 2; i <= 4; i++)
		{
			parallel(list, i, 2, RitualRune.EARTH);
		}

		addOffsetRunes(list, 2, 1, 4, RitualRune.FIRE);
		corners(list, 2, 4, RitualRune.AIR);
		corners(list, 4, 2, RitualRune.FIRE);

		for (int i = -1; i <= 1; i++)
		{
			addOffsetRunes(list, 3, i, 4, RitualRune.EARTH);
		}
	}
        }
        case CRAFTING -> {
{
		corners(list, 1, 1, RitualRune.EARTH);

		addOffsetRunes(list, 1, 2, -1, RitualRune.FIRE);
		corners(list, 1, -1, RitualRune.FIRE);
		addRune(list, -1, -1, 0, RitualRune.EARTH);
		addRune(list, 1, -1, 0, RitualRune.EARTH);
		addRune(list, 0, -1, -1, RitualRune.EARTH);
		addRune(list, 0, -1, 1, RitualRune.WATER);
		addRune(list, -1, 1, 0, RitualRune.EARTH);
		addRune(list, 1, 1, 0, RitualRune.EARTH);
		addRune(list, 0, 1, -1, RitualRune.EARTH);
		addRune(list, 0, 0, 1, RitualRune.EARTH);

		addRune(list, 0, 2, -1, RitualRune.DUSK);

	}
        }
        case CRYSTAL_HARVEST -> {
{
		corners(list, 1, 0, RitualRune.AIR);
		parallel(list, 1, 1, RitualRune.DUSK);
		parallel(list, 1, -1, RitualRune.FIRE);
		parallel(list, 2, -1, RitualRune.FIRE);
		parallel(list, 3, -1, RitualRune.FIRE);
		addOffsetRunes(list, 3, 1, -1, RitualRune.FIRE);
		corners(list, 3, -1, RitualRune.EARTH);
		corners(list, 3, 0, RitualRune.EARTH);
		addOffsetRunes(list, 3, 2, 0, RitualRune.DUSK);
	}
        }
        case CRYSTAL_SPLIT -> {
{
		addRune(list, 0, 0, -1, RitualRune.FIRE);
		addRune(list, 1, 0, 0, RitualRune.EARTH);
		addRune(list, 0, 0, 1, RitualRune.WATER);
		addRune(list, -1, 0, 0, RitualRune.AIR);

		addOffsetRunes(list, 1, 2, -1, RitualRune.DUSK);
		corners(list, 1, 0, RitualRune.BLANK);
		parallel(list, 2, 0, RitualRune.DUSK);
	}
        }
        case ELLIPSOID -> {
{
		corners(list, 1, 0, RitualRune.DUSK);

		addRune(list, 4, 0, 0, RitualRune.FIRE);
		addRune(list, 5, 0, 0, RitualRune.FIRE);
		addRune(list, 5, 0, -1, RitualRune.FIRE);
		addRune(list, 5, 0, -2, RitualRune.FIRE);
		addRune(list, -4, 0, 0, RitualRune.FIRE);
		addRune(list, -5, 0, 0, RitualRune.FIRE);
		addRune(list, -5, 0, 1, RitualRune.FIRE);
		addRune(list, -5, 0, 2, RitualRune.FIRE);

		addRune(list, 0, 0, 4, RitualRune.AIR);
		addRune(list, 0, 0, 5, RitualRune.AIR);
		addRune(list, 1, 0, 5, RitualRune.AIR);
		addRune(list, 2, 0, 5, RitualRune.AIR);
		addRune(list, 0, 0, -4, RitualRune.AIR);
		addRune(list, 0, 0, -5, RitualRune.AIR);
		addRune(list, -1, 0, -5, RitualRune.AIR);
		addRune(list, -2, 0, -5, RitualRune.AIR);

		addRune(list, 3, 0, 1, RitualRune.EARTH);
		addRune(list, 3, 0, 2, RitualRune.EARTH);
		addRune(list, 3, 0, 3, RitualRune.EARTH);
		addRune(list, 2, 0, 3, RitualRune.EARTH);
		addRune(list, -3, 0, -1, RitualRune.EARTH);
		addRune(list, -3, 0, -2, RitualRune.EARTH);
		addRune(list, -3, 0, -3, RitualRune.EARTH);
		addRune(list, -2, 0, -3, RitualRune.EARTH);

		addRune(list, 1, 0, -3, RitualRune.WATER);
		addRune(list, 2, 0, -3, RitualRune.WATER);
		addRune(list, 3, 0, -3, RitualRune.WATER);
		addRune(list, 3, 0, -2, RitualRune.WATER);
		addRune(list, -1, 0, 3, RitualRune.WATER);
		addRune(list, -2, 0, 3, RitualRune.WATER);
		addRune(list, -3, 0, 3, RitualRune.WATER);
		addRune(list, -3, 0, 2, RitualRune.WATER);
	}
        }
        case FEATHERED_KNIFE -> {
{
		parallel(list, 1, 0, RitualRune.DUSK);
		parallel(list, 2, -1, RitualRune.WATER);
		corners(list, 1, -1, RitualRune.AIR);
		addOffsetRunes(list, 2, 4, -1, RitualRune.FIRE);
		addOffsetRunes(list, 2, 4, 0, RitualRune.EARTH);
		addOffsetRunes(list, 4, 3, 0, RitualRune.EARTH);
		corners(list, 3, 0, RitualRune.AIR);
	}
        }
        case FELLING -> {
{
		corners(list, 1, 0, RitualRune.EARTH);
		corners(list, 1, 1, RitualRune.EARTH);
	}
        }
        case FORSAKEN_SOUL -> {
{
		corners(list, 1, 0, RitualRune.AIR);
		parallel(list, 1, -1, RitualRune.DUSK);
		parallel(list, 1, 1, RitualRune.FIRE);
		parallel(list, 2, 1, RitualRune.FIRE);
		parallel(list, 3, 1, RitualRune.FIRE);
		addOffsetRunes(list, 3, 1, 1, RitualRune.FIRE);
		corners(list, 3, 1, RitualRune.EARTH);
		corners(list, 3, 0, RitualRune.EARTH);
		addOffsetRunes(list, 3, 2, 0, RitualRune.EARTH);
	}
        }
        case FULL_STOMACH -> {
{
		parallel(list, 3, 0, RitualRune.FIRE);
		corners(list, 1, 0, RitualRune.AIR);
		addOffsetRunes(list, 1, 2, 0, RitualRune.AIR);
		corners(list, 4, 0, RitualRune.WATER);
		addOffsetRunes(list, 4, 3, 0, RitualRune.EARTH);
	}
        }
        case GEODE -> {
{
        corners(list, 1, 0, RitualRune.EARTH);
        parallel(list, 1, 0, RitualRune.AIR);
        parallel(list, 2, 0, RitualRune.FIRE);
    }
        }
        case GROUNDING -> {
{
		parallel(list, 1, 0, RitualRune.DUSK);
		corners(list, 2, 2, RitualRune.EARTH);
		corners(list, 3, 3, RitualRune.EARTH);
	}
        }
        case HARVEST -> {
{
		corners(list, 1, 0, RitualRune.DUSK);
		parallel(list, 2, 0, RitualRune.EARTH);
		addOffsetRunes(list, 3, 1, 0, RitualRune.EARTH);
		addOffsetRunes(list, 3, 2, 0, RitualRune.WATER);
	}
        }
        case JUMPING -> {
{
		for (int i = -1; i <= 1; i++) corners(list, 1, i, RitualRune.AIR);
	}
        }
        case LAVA -> {
{
		parallel(list, 1, 0, RitualRune.FIRE);
	}
        }
        case DOWNGRADE -> {
{
		addRune(list, 0, 0, -1, RitualRune.AIR);
		addRune(list, 0, 0, -2, RitualRune.DUSK);
		addRune(list, 0, 1, -3, RitualRune.DUSK);
		addRune(list, 0, 2, -3, RitualRune.BLANK);
		addRune(list, 0, 3, -3, RitualRune.BLANK);
		addRune(list, 0, 1, -4, RitualRune.FIRE);

		for (int i = 1; i <= 3; i++)
		{
			addRune(list, 0, 0, i, RitualRune.AIR);
		}

		for (int sgn = -1; sgn <= 1; sgn += 2)
		{
			addRune(list, sgn, 0, 4, RitualRune.AIR);
			addRune(list, sgn * 2, 0, 2, RitualRune.AIR);
			addRune(list, sgn * 3, 0, 2, RitualRune.AIR);
			addRune(list, sgn * 3, 0, 3, RitualRune.AIR);
			addRune(list, sgn, 0, 0, RitualRune.EARTH);
			addRune(list, sgn, 0, 1, RitualRune.EARTH);
			addRune(list, sgn * 2, 0, -1, RitualRune.FIRE);
			addRune(list, sgn * 2, 0, -2, RitualRune.FIRE);
			addRune(list, sgn * 3, 0, -2, RitualRune.FIRE);
			addRune(list, sgn * 3, 0, -3, RitualRune.FIRE);
			addRune(list, sgn * 3, 0, -4, RitualRune.FIRE);
			addRune(list, sgn, 1, -1, RitualRune.AIR);
			addRune(list, sgn, 1, -2, RitualRune.AIR);
			addRune(list, sgn, 1, -4, RitualRune.FIRE);
			addRune(list, sgn * 2, 1, -4, RitualRune.FIRE);
			addRune(list, sgn, 0, -3, RitualRune.EARTH);
			addRune(list, sgn, 0, -4, RitualRune.EARTH);
			addRune(list, sgn, 0, -5, RitualRune.EARTH);
			addRune(list, sgn, 1, -5, RitualRune.EARTH);
			addRune(list, sgn, 2, -5, RitualRune.EARTH);
			addRune(list, sgn, 3, -5, RitualRune.EARTH);
			addRune(list, sgn, 3, -4, RitualRune.EARTH);
		}
	}
        }
        case MAGNETISM -> {
{
		corners(list, 1, 0, RitualRune.EARTH);
		parallel(list, 2, 1, RitualRune.EARTH);
		corners(list, 2, 1, RitualRune.AIR);
		parallel(list, 2, 2, RitualRune.FIRE);
	}
        }
        case METEOR -> {
{
		addRune(list, 2, 0, 0, RitualRune.FIRE);
		addRune(list, -2, 0, 0, RitualRune.FIRE);
		addRune(list, 0, 0, 2, RitualRune.FIRE);
		addRune(list, 0, 0, -2, RitualRune.FIRE);
		addRune(list, 3, 0, 1, RitualRune.AIR);
		addRune(list, 3, 0, -1, RitualRune.AIR);
		addRune(list, -3, 0, 1, RitualRune.AIR);
		addRune(list, -3, 0, -1, RitualRune.AIR);
		addRune(list, 1, 0, 3, RitualRune.AIR);
		addRune(list, -1, 0, 3, RitualRune.AIR);
		addRune(list, 1, 0, -3, RitualRune.AIR);
		addRune(list, -1, 0, -3, RitualRune.AIR);
		addRune(list, 4, 0, 2, RitualRune.AIR);
		addRune(list, 4, 0, -2, RitualRune.AIR);
		addRune(list, -4, 0, 2, RitualRune.AIR);
		addRune(list, -4, 0, -2, RitualRune.AIR);
		addRune(list, 2, 0, 4, RitualRune.AIR);
		addRune(list, -2, 0, 4, RitualRune.AIR);
		addRune(list, 2, 0, -4, RitualRune.AIR);
		addRune(list, -2, 0, -4, RitualRune.AIR);
		addRune(list, 5, 0, 3, RitualRune.DUSK);
		addRune(list, 5, 0, -3, RitualRune.DUSK);
		addRune(list, -5, 0, 3, RitualRune.DUSK);
		addRune(list, -5, 0, -3, RitualRune.DUSK);
		addRune(list, 3, 0, 5, RitualRune.DUSK);
		addRune(list, -3, 0, 5, RitualRune.DUSK);
		addRune(list, 3, 0, -5, RitualRune.DUSK);
		addRune(list, -3, 0, -5, RitualRune.DUSK);
		addRune(list, -4, 0, -4, RitualRune.DUSK);
		addRune(list, -4, 0, 4, RitualRune.DUSK);
		addRune(list, 4, 0, 4, RitualRune.DUSK);
		addRune(list, 4, 0, -4, RitualRune.DUSK);

		for (int i = 4; i <= 6; i++)
		{
			addRune(list, i, 0, 0, RitualRune.EARTH);
			addRune(list, -i, 0, 0, RitualRune.EARTH);
			addRune(list, 0, 0, i, RitualRune.EARTH);
			addRune(list, 0, 0, -i, RitualRune.EARTH);
		}

		addRune(list, 8, 0, 0, RitualRune.EARTH);
		addRune(list, -8, 0, 0, RitualRune.EARTH);
		addRune(list, 0, 0, 8, RitualRune.EARTH);
		addRune(list, 0, 0, -8, RitualRune.EARTH);
		addRune(list, 8, 1, 0, RitualRune.EARTH);
		addRune(list, -8, 1, 0, RitualRune.EARTH);
		addRune(list, 0, 1, 8, RitualRune.EARTH);
		addRune(list, 0, 1, -8, RitualRune.EARTH);
		addRune(list, 7, 1, 0, RitualRune.EARTH);
		addRune(list, -7, 1, 0, RitualRune.EARTH);
		addRune(list, 0, 1, 7, RitualRune.EARTH);
		addRune(list, 0, 1, -7, RitualRune.EARTH);
		addRune(list, 7, 2, 0, RitualRune.FIRE);
		addRune(list, -7, 2, 0, RitualRune.FIRE);
		addRune(list, 0, 2, 7, RitualRune.FIRE);
		addRune(list, 0, 2, -7, RitualRune.FIRE);
		addRune(list, 6, 2, 0, RitualRune.FIRE);
		addRune(list, -6, 2, 0, RitualRune.FIRE);
		addRune(list, 0, 2, 6, RitualRune.FIRE);
		addRune(list, 0, 2, -6, RitualRune.FIRE);
		addRune(list, 6, 3, 0, RitualRune.WATER);
		addRune(list, -6, 3, 0, RitualRune.WATER);
		addRune(list, 0, 3, 6, RitualRune.WATER);
		addRune(list, 0, 3, -6, RitualRune.WATER);
		addRune(list, 5, 3, 0, RitualRune.WATER);
		addRune(list, -5, 3, 0, RitualRune.WATER);
		addRune(list, 0, 3, 5, RitualRune.WATER);
		addRune(list, 0, 3, -5, RitualRune.WATER);
		addRune(list, 5, 4, 0, RitualRune.AIR);
		addRune(list, -5, 4, 0, RitualRune.AIR);
		addRune(list, 0, 4, 5, RitualRune.AIR);
		addRune(list, 0, 4, -5, RitualRune.AIR);

		for (int i = -1; i <= 1; i++)
		{
			addRune(list, i, 4, 4, RitualRune.AIR);
			addRune(list, i, 4, -4, RitualRune.AIR);
			addRune(list, 4, 4, i, RitualRune.AIR);
			addRune(list, -4, 4, i, RitualRune.AIR);
		}

		addRune(list, 2, 4, 4, RitualRune.WATER);
		addRune(list, 4, 4, 2, RitualRune.WATER);
		addRune(list, 2, 4, -4, RitualRune.WATER);
		addRune(list, -4, 4, 2, RitualRune.WATER);
		addRune(list, -2, 4, 4, RitualRune.WATER);
		addRune(list, 4, 4, -2, RitualRune.WATER);
		addRune(list, -2, 4, -4, RitualRune.WATER);
		addRune(list, -4, 4, -2, RitualRune.WATER);
		addRune(list, 2, 4, 3, RitualRune.FIRE);
		addRune(list, 3, 4, 2, RitualRune.FIRE);
		addRune(list, 3, 4, 3, RitualRune.FIRE);
		addRune(list, -2, 4, 3, RitualRune.FIRE);
		addRune(list, 3, 4, -2, RitualRune.FIRE);
		addRune(list, 3, 4, -3, RitualRune.FIRE);
		addRune(list, 2, 4, -3, RitualRune.FIRE);
		addRune(list, -3, 4, 2, RitualRune.FIRE);
		addRune(list, -3, 4, 3, RitualRune.FIRE);
		addRune(list, -2, 4, -3, RitualRune.FIRE);
		addRune(list, -3, 4, -2, RitualRune.FIRE);
		addRune(list, -3, 4, -3, RitualRune.FIRE);
	}
        }
        case PLACER -> {
{
		addRune(list, 3, 0, 3, RitualRune.EARTH);
		addRune(list, 3, 0, -3, RitualRune.EARTH);
		addRune(list, -3, 0, 3, RitualRune.EARTH);
		addRune(list, -3, 0, -3, RitualRune.EARTH);

		addRune(list, 3, 0, 2, RitualRune.WATER);
		addRune(list, 3, 0, -2, RitualRune.WATER);
		addRune(list, 2, 0, 3, RitualRune.WATER);
		addRune(list, 2, 0, -3, RitualRune.WATER);
		addRune(list, -2, 0, 3, RitualRune.WATER);
		addRune(list, -2, 0, -3, RitualRune.WATER);
		addRune(list, -3, 0, 2, RitualRune.WATER);
		addRune(list, -3, 0, -2, RitualRune.WATER);
	}
        }
        case REGENERATION -> {
{
		addRune(list, 4, 0, 0, RitualRune.AIR);
		addRune(list, 5, 0, -1, RitualRune.AIR);
		addRune(list, 5, 0, 1, RitualRune.AIR);
		addRune(list, -4, 0, 0, RitualRune.AIR);
		addRune(list, -5, 0, -1, RitualRune.AIR);
		addRune(list, -5, 0, 1, RitualRune.AIR);
		addRune(list, 0, 0, 4, RitualRune.FIRE);
		addRune(list, 1, 0, 5, RitualRune.FIRE);
		addRune(list, -1, 0, 5, RitualRune.FIRE);
		addRune(list, 0, 0, -4, RitualRune.FIRE);
		addRune(list, 1, 0, -5, RitualRune.FIRE);
		addRune(list, -1, 0, -5, RitualRune.FIRE);
		addOffsetRunes(list, 3, 5, 0, RitualRune.WATER);
		corners(list, 3, 0, RitualRune.DUSK);
		addOffsetRunes(list, 4, 5, 0, RitualRune.EARTH);
		addOffsetRunes(list, 4, 5, -1, RitualRune.EARTH);
		corners(list, 5, 0, RitualRune.EARTH);
	}
        }
        case SIMPLE_DUNGEON -> {
{
		parallel(list, 1, 4, RitualRune.AIR);
		parallel(list, 2, 4, RitualRune.EARTH);
		corners(list, 1, 0, RitualRune.EARTH);
		parallel(list, 2, 0, RitualRune.EARTH);
		corners(list, 2, 0, RitualRune.FIRE);
		for (int i = -1; i <= 1; i++)
		{
			addRune(list, i, 0, 3, RitualRune.WATER);
			addRune(list, i, 0, -3, RitualRune.WATER);
			addRune(list, 3, 0, i, RitualRune.FIRE);
			addRune(list, -3, 0, i, RitualRune.FIRE);
		}

		for (int j = 1; j <= 4; j++)
		{
			addRune(list, 0, j, 0, RitualRune.EARTH);
		}
	}
        }
        case SPEED -> {
{
		addRune(list, 0, 0, -2, RitualRune.DUSK);
		addRune(list, 1, 0, -1, RitualRune.AIR);
		addRune(list, -1, 0, -1, RitualRune.AIR);
		for (int i = 0; i < 3; i++)
		{
			addRune(list, 2, 0, i, RitualRune.AIR);
			addRune(list, -2, 0, i, RitualRune.AIR);
		}
	}
        }
        case SPHERE -> {
{
		addRune(list, 1, 0, 1, RitualRune.EARTH);
		addRune(list, 1, 0, -1, RitualRune.EARTH);
		addRune(list, -1, 0, 1, RitualRune.EARTH);
		addRune(list, -1, 0, -1, RitualRune.EARTH);
		addRune(list, 2, 1, 0, RitualRune.EARTH);
		addRune(list, 0, 1, 2, RitualRune.EARTH);
		addRune(list, -2, 1, 0, RitualRune.EARTH);
		addRune(list, 0, 1, -2, RitualRune.EARTH);
		addRune(list, 2, 1, 2, RitualRune.AIR);
		addRune(list, 2, 1, -2, RitualRune.AIR);
		addRune(list, -2, 1, 2, RitualRune.AIR);
		addRune(list, -2, 1, -2, RitualRune.AIR);
		addRune(list, 2, 2, 0, RitualRune.FIRE);
		addRune(list, 0, 2, 2, RitualRune.FIRE);
		addRune(list, -2, 2, 0, RitualRune.FIRE);
		addRune(list, 0, 2, -2, RitualRune.DUSK);
	}
        }
        case STANDARD_DUNGEON -> {
{
//		parallel(list, 1, 4, RitualRune.AIR);
//		parallel(list, 2, 4, RitualRune.EARTH);
//		corners(list, 1, 0, RitualRune.EARTH);
//		parallel(list, 2, 0, RitualRune.EARTH);
//		corners(list, 2, 0, RitualRune.FIRE);
//		for (int i = -1; i <= 1; i++)
//		{
//			addRune(list, i, 0, 3, RitualRune.WATER);
//			addRune(list, i, 0, -3, RitualRune.WATER);
//			addRune(list, 3, 0, i, RitualRune.FIRE);
//			addRune(list, -3, 0, i, RitualRune.FIRE);
//		}
//
//		for (int j = 1; j <= 4; j++)
//		{
//			addRune(list, 0, j, 0, RitualRune.DUSK);
//		}

		addRune(list, 0, 0, 1, RitualRune.FIRE);
		addRune(list, -1, 0, 0, RitualRune.FIRE);
		addRune(list, 0, 0, -1, RitualRune.WATER);
		addRune(list, 1, 0, 0, RitualRune.WATER);
		addRune(list, -1, 0, 1, RitualRune.DUSK);
		addRune(list, 1, 0, -1, RitualRune.DUSK);

		addRune(list, 2, 0, 2, RitualRune.WATER);
		addRune(list, 2, 0, 1, RitualRune.WATER);
		addRune(list, 1, 0, 2, RitualRune.WATER);
		addRune(list, -2, 0, -2, RitualRune.FIRE);
		addRune(list, -2, 0, -1, RitualRune.FIRE);
		addRune(list, -1, 0, -2, RitualRune.FIRE);

		addRune(list, -3, 0, 3, RitualRune.AIR);
		addRune(list, -2, 0, 3, RitualRune.AIR);
		addRune(list, -1, 0, 3, RitualRune.AIR);
		addRune(list, -3, 0, 2, RitualRune.AIR);
		addRune(list, -3, 0, 1, RitualRune.AIR);
		addRune(list, 3, 0, -3, RitualRune.EARTH);
		addRune(list, 2, 0, -3, RitualRune.EARTH);
		addRune(list, 1, 0, -3, RitualRune.EARTH);
		addRune(list, 3, 0, -2, RitualRune.EARTH);
		addRune(list, 3, 0, -1, RitualRune.EARTH);

		addRune(list, 3, 0, 3, RitualRune.DUSK);
		addRune(list, 3, 0, 4, RitualRune.DUSK);
		addRune(list, 4, 0, 3, RitualRune.DUSK);
		addRune(list, 2, 0, 4, RitualRune.DUSK);
		addRune(list, 4, 0, 2, RitualRune.DUSK);
		addRune(list, -3, 0, -3, RitualRune.DUSK);
		addRune(list, -3, 0, -4, RitualRune.DUSK);
		addRune(list, -4, 0, -3, RitualRune.DUSK);
		addRune(list, -2, 0, -4, RitualRune.DUSK);
		addRune(list, -4, 0, -2, RitualRune.DUSK);

		corners(list, 3, 4, RitualRune.EARTH);
		addOffsetRunes(list, 2, 3, 4, RitualRune.DUSK);
		addOffsetRunes(list, 2, 4, 4, RitualRune.DUSK);
		addOffsetRunes(list, 1, 4, 4, RitualRune.AIR);
		parallel(list, 4, 4, RitualRune.AIR);

		addRune(list, 0, 2, 0, RitualRune.EARTH);
		addRune(list, 0, 3, 0, RitualRune.DUSK);
		addRune(list, 0, 4, 0, RitualRune.DUSK);
		addRune(list, 0, 5, 0, RitualRune.DUSK);
		addRune(list, 0, 6, 0, RitualRune.AIR);
	}
        }
        case UPGRADE_REMOVE -> {
{
		corners(list, 1, 0, RitualRune.DUSK);
		corners(list, 2, 0, RitualRune.FIRE);
		addOffsetRunes(list, 1, 2, 0, RitualRune.FIRE);
		corners(list, 1, 1, RitualRune.WATER);
		parallel(list, 4, 0, RitualRune.EARTH);
		corners(list, 1, 3, RitualRune.WATER);
		parallel(list, 1, 4, RitualRune.AIR);

		for (int i = 0; i < 4; i++)
		{
			corners(list, 3, i, RitualRune.EARTH);
		}
	}
        }
        case WELL_OF_SUFFERING -> {
{
		corners(list, 1, 0, RitualRune.FIRE);
		corners(list, 2, -1, RitualRune.FIRE);
		parallel(list, 2, -1, RitualRune.EARTH);
		corners(list, -3, -1, RitualRune.DUSK);
		addOffsetRunes(list, 2, 4, -1, RitualRune.WATER);
		addOffsetRunes(list, 1, 4, 0, RitualRune.WATER);
		parallel(list, 4, 1, RitualRune.AIR);
	}
        }
        case YAWNING_VOID -> {
{
		parallel(list, 1, 0, RitualRune.BLANK);
		parallel(list, 4, 0, RitualRune.EARTH);
		addOffsetRunes(list, 1, 3, 0, RitualRune.WATER);
		addOffsetRunes(list, 3, 1, 0, RitualRune.WATER);
		corners(list, 2, 0, RitualRune.DUSK);
		parallel(list, 3, 1, RitualRune.AIR);
		parallel(list, 2, 1, RitualRune.EARTH);
		parallel(list, 2, 2, RitualRune.EARTH);
		parallel(list, 3, 3, RitualRune.AIR);
		corners(list, 2, 3, RitualRune.FIRE);
		addOffsetRunes(list, 2, 1, 3, RitualRune.FIRE);
		addOffsetRunes(list, 1, 2, 3, RitualRune.FIRE);
	}
        }
        case ZEPHYR -> {
{
		parallel(list, 2, 0, RitualRune.AIR);
		corners(list, 1, 1, RitualRune.AIR);
		parallel(list, 1, -1, RitualRune.AIR);
	}
        }
        }
        var unique = new LinkedHashMap<BlockPos, Component>();
        for (var part : list) unique.putIfAbsent(part.offset(), part);
        return List.copyOf(unique.values());
    }
    private static void addRune(List<Component> list,int x,int y,int z,RitualRune rune) { list.add(new Component(new BlockPos(x,y,z),rune)); }
    private static void corners(List<Component> list,int r,int y,RitualRune rune) { for(int x:new int[]{-r,r})for(int z:new int[]{-r,r})list.add(new Component(new BlockPos(x,y,z),rune)); }
    private static void parallel(List<Component> list,int r,int y,RitualRune rune) { for(int v:new int[]{-r,r}) {list.add(new Component(new BlockPos(v,y,0),rune));list.add(new Component(new BlockPos(0,y,v),rune));} }
    private static void addOffsetRunes(List<Component> list,int o1,int o2,int y,RitualRune rune) { addRune(list,o1,y,o2,rune);addRune(list,o2,y,o1,rune);addRune(list,o1,y,-o2,rune);addRune(list,-o2,y,o1,rune);addRune(list,-o1,y,o2,rune);addRune(list,o2,y,-o1,rune);addRune(list,-o1,y,-o2,rune);addRune(list,-o2,y,-o1,rune); }
    public Scan scan(Level level,BlockPos master) {
        for(var part:components())if(!WorldAccess.loaded(level,master.offset(part.offset())))return Scan.UNLOADED;
        for(var part:components())if(!level.getBlockState(master.offset(part.offset())).is(part.rune().block()))return Scan.INVALID;
        return Scan.COMPLETE;
    }

    public boolean usesDemonWillConfig() {
        return switch (this) {
            case WATER, GREEN_GROVE, CRUSHING, ANIMAL_GROWTH, FEATHERED_KNIFE, GEODE, GROUNDING,
                    LAVA, REGENERATION, SPEED, CRAFTING, YAWNING_VOID -> true;
            default -> false;
        };
    }
    /** Configurable default regions and limits copied from upstream ritual constructors. */
    public List<RitualRangeSpec> rangeSpecs() {
        return switch (this) {
            case WATER -> List.of(
                    range("waterRange", new RitualArea(new BlockPos(0, 1, 0), 1), 9, 3, 3),
                    range("waterTank", new RitualArea(new BlockPos(0, 1, 0), 1), 1, 10, 10));
            case GREEN_GROVE -> List.of(
                    range("growing", new RitualArea(new BlockPos(-1, 2, -1), 3, 1, 3), 81, 4, 4),
                    range("leech", new RitualArea(new BlockPos(-1, 2, -1), 3, 1, 3), 0, 15, 15),
                    range("hydrate", new RitualArea(new BlockPos(-1, 1, -1), 3, 2, 3), 0, 15, 15));
            case CRUSHING -> List.of(
                    range("crushingRange", new RitualArea(new BlockPos(-1, -3, -1), 3), 50, 10, 10),
                    range("chest", new RitualArea(new BlockPos(0, 1, 0), 1), 1, 3, 3));
            case ANIMAL_GROWTH -> List.of(
                    range("growing", new RitualArea(new BlockPos(-2, 1, -2), 5, 2, 5), 0, 7, 7),
                    range("chest", new RitualArea(new BlockPos(0, 1, 0), 1), 1, 3, 3));
            case FEATHERED_KNIFE -> List.of(
                    range("altar", new RitualArea(new BlockPos(-5, -10, -5), 11, 21, 11), 0, 10, 15),
                    range("damage", new RitualArea(new BlockPos(-15, -20, -15), 31, 41, 31), 0, 25, 25));
            case GEODE -> List.of(
                    range("harvest", new RitualArea(new BlockPos(-1, 2, -1), 3, 3, 3), 81, 15, 15),
                    range("acceleration", new RitualArea(new BlockPos(0, 3, 0), 1), 7, 15, 15),
                    range("chest", new RitualArea(new BlockPos(0, 1, 0), 1), 1, 15, 15),
                    range("harm", new RitualArea(new BlockPos(-1, -3, -1), 3, 3, 3), 75, 15, 15));
            case GROUNDING -> List.of(
                    range("groundingRange", new RitualArea(new BlockPos(-10, 0, -10), 21, 30, 21), 0, 200, 200));
            case LAVA -> List.of(
                    range("lavaRange", new RitualArea(new BlockPos(0, 1, 0), 1), 9, 3, 3),
                    range("fireFuse", new RitualArea(new BlockPos(-2, -2, -2), 5), 0, 10, 10),
                    range("fireResist", new RitualArea(new BlockPos(0, 0, 0), 1), 0, 10, 10),
                    range("fireDamage", new RitualArea(new BlockPos(0, 0, 0), 1), 0, 10, 10),
                    range("lavaTank", new RitualArea(new BlockPos(0, 1, 0), 1), 1, 10, 10));
            case REGENERATION -> List.of(
                    range("heal", new RitualArea(new BlockPos(-15, -15, -15), 31), 0, 20, 20),
                    range("vampire", new RitualArea(new BlockPos(-15, -15, -15), 31), 0, 20, 20));
            case SPEED -> List.of(
                    range("sanicRange", new RitualArea(new BlockPos(-2, 1, -2), new BlockPos(3, 5, 3)), 0, 4, 5));
            case CRAFTING -> List.of(
                    range("outputRange", new RitualArea(new BlockPos(0, 1, 0), 1), 1, 7, 7),
                    range("inputRange", new RitualArea(new BlockPos(0, 1, 0), 1), 1, 7, 7),
                    range("recipeRange", new RitualArea(new BlockPos(0, 2, 0), 1), 1, 7, 7),
                    range("outputFilterRange", new RitualArea(new BlockPos(0, 0, 0), 1), 1, 7, 7),
                    range("hellforgedRange", new RitualArea(new BlockPos(0, 5, 0), 1), 1, 7, 7));
            case YAWNING_VOID -> List.of(
                    range("placementRange", new RitualArea(new BlockPos(-1, 1, -1), 3), 50, 4, 4),
                    range("quarryRange", new RitualArea(new BlockPos(-1, -3, -1), 3), 0, 64, 32),
                    range("chest", new RitualArea(new BlockPos(0, 1, 0), 1), 1, 3, 3));
            default -> List.of();
        };
    }

    private static RitualRangeSpec range(String name, RitualArea area, int maxVolume, int maxVertical, int maxHorizontal) {
        return new RitualRangeSpec(name, area, maxVolume, maxVertical, maxHorizontal);
    }

    public RitualRangeSpec rangeSpec(String name) {
        for (RitualRangeSpec spec : rangeSpecs()) if (spec.name().equals(name)) return spec;
        return null;
    }

    public int maxVolumeForRange(String range, List<EnumDemonWillType> activeTypes, DemonWillHolder holder) {
        RitualRangeSpec spec = rangeSpec(range);
        int defaultMax = spec == null ? 0 : spec.maxVolume();
        double will = will(holder, activeTypes, EnumDemonWillType.DESTRUCTIVE);
        if (this == GREEN_GROVE && "growing".equals(range) && will > 0) {
            return 81 + (int) Math.pow(will / 4.0, 1.5);
        }
        if (this == LAVA && "lavaRange".equals(range) && will > 0) {
            return 9 + (int) Math.pow(will / 10.0, 1.5);
        }
        return defaultMax;
    }

    public int maxVerticalForRange(String range, List<EnumDemonWillType> activeTypes, DemonWillHolder holder) {
        RitualRangeSpec spec = rangeSpec(range);
        int defaultMax = spec == null ? 0 : spec.maxVertical();
        double will = will(holder, activeTypes, EnumDemonWillType.DESTRUCTIVE);
        if (this == GREEN_GROVE && "growing".equals(range) && will > 0) return (int) (4 + will / 10.0);
        if (this == LAVA && "lavaRange".equals(range) && will > 0) return (int) (3 + will / 10.0);
        return defaultMax;
    }

    public int maxHorizontalForRange(String range, List<EnumDemonWillType> activeTypes, DemonWillHolder holder) {
        RitualRangeSpec spec = rangeSpec(range);
        int defaultMax = spec == null ? 0 : spec.maxHorizontal();
        double will = will(holder, activeTypes, EnumDemonWillType.DESTRUCTIVE);
        if (this == GREEN_GROVE && "growing".equals(range) && will > 0) return (int) (4 + will / 10.0);
        if (this == LAVA && "lavaRange".equals(range) && will > 0) return (int) (3 + will / 10.0);
        return defaultMax;
    }

    public int refreshInterval(List<EnumDemonWillType> activeTypes, DemonWillHolder holder) {
        double raw = will(holder, activeTypes, EnumDemonWillType.DEFAULT);
        return switch (this) {
            case WATER, GROUNDING, LAVA, SPEED -> 1;
            case CRAFTING -> 4;
            case REGENERATION -> 50;
            case GREEN_GROVE -> raw >= 0.05 ? 10 : 20;
            case CRUSHING -> raw >= 0.05 ? Math.max(1, (int) (40 - raw / 5)) : 40;
            case ANIMAL_GROWTH -> raw >= 0.05 ? Math.max(1, (int) (20 - raw / 10)) : 20;
            case FEATHERED_KNIFE -> raw >= 0.05 ? 10 : 20;
            case YAWNING_VOID -> raw >= 0.05 ? Math.max(1, (int) (10 - raw / 10)) : 10;
            default -> interval;
        };
    }

    private static double will(DemonWillHolder holder, List<EnumDemonWillType> activeTypes, EnumDemonWillType type) {
        if (holder == null || activeTypes == null || !activeTypes.contains(type)) return 0;
        return holder.getWill(type);
    }

}


