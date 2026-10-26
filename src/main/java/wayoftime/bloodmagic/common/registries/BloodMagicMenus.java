package wayoftime.bloodmagic.common.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.menu.AltarMenu;
import wayoftime.bloodmagic.common.menu.MachineMenu;

public final class BloodMagicMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BloodMagic.MODID);
    // The client uses a dummy inventory and vanilla menu sync, so no extra opening buffer is needed.
    public static final DeferredHolder<MenuType<?>, MenuType<AltarMenu>> ALTAR = MENUS.register(
            "altar", () -> new MenuType<>(AltarMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private BloodMagicMenus() {}
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> ALCHEMY_TABLE = MENUS.register(
            "alchemytable", () -> new MenuType<>(MachineMenu::table, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> HELLFIRE_FORGE = MENUS.register("soulforge", () -> new MenuType<>(MachineMenu::forge, FeatureFlags.DEFAULT_FLAGS));
}
