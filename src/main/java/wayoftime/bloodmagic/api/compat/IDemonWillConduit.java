package wayoftime.bloodmagic.api.compat;

/** Upstream conduit contract used by machines that accept or supply Demon Will. */
public interface IDemonWillConduit {
    int getWeight();
    double fillDemonWill(EnumDemonWillType type, double amount, boolean doFill);
    double drainDemonWill(EnumDemonWillType type, double amount, boolean doDrain);
    boolean canFill(EnumDemonWillType type);
    boolean canDrain(EnumDemonWillType type);
    double getCurrentWill(EnumDemonWillType type);
}
