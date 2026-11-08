package wayoftime.bloodmagic.common.item.block;

import net.minecraft.world.item.BlockItem;
import wayoftime.bloodmagic.common.block.BlockBloodRune;
import wayoftime.bloodmagic.common.item.ItemBase;

/** Block tooltip hooks were removed; the rune item now carries its decoration hint. */
public final class ItemBlockBloodRune extends BlockItem {
    public ItemBlockBloodRune(BlockBloodRune block, Properties properties) {
        super(block, ItemBase.withDescription(properties, "decoration.safe"));
    }
}
