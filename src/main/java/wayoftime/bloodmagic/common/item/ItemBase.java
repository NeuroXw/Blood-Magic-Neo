package wayoftime.bloodmagic.common.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

/** Basic material item. Properties must come from an ID-aware registration factory. */
public class ItemBase extends Item {
    public ItemBase(Properties properties) {
        this(properties, "");
    }

    public ItemBase(Properties properties, String desc) {
        super(withDescription(properties, desc));
    }

    public ItemBase(Properties properties, int stackSize) {
        this(properties, stackSize, "");
    }

    public ItemBase(Properties properties, int stackSize, String desc) {
        this(properties.stacksTo(stackSize), desc);
    }

    /** Static descriptions use a data component; appendHoverText is deprecated in 26.1.2. */
    public static Properties withDescription(Properties properties, String desc) {
        if (desc.isEmpty()) {
            return properties;
        }
        return properties.component(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("tooltip.bloodmagic." + desc)
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)))));
    }
}
