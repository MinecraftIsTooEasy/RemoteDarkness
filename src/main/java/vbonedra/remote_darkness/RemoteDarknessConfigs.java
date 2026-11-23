package vbonedra.remote_darkness;

import fi.dy.masa.malilib.config.ConfigTab;
import fi.dy.masa.malilib.config.SimpleConfigs;
import fi.dy.masa.malilib.config.options.*;
import net.minecraft.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static vbonedra.remote_darkness.RemoteDarknessMod.MOD_NAME;

public class RemoteDarknessConfigs extends SimpleConfigs {
    public static final ConfigBoolean EnableTrueDarkness = new ConfigBoolean("True Darkness", true, "Makes 0 light pitch black.");
    public static final ConfigBoolean OverrideNetherLightmap = new ConfigBoolean("Dark Nether", true, "No environment light in The Nether.");
    public static final ConfigBoolean RemoveEndMinValues = new ConfigBoolean("Dark End", true, "No environment light in The End.");
    public static final ConfigBoolean EnableDarkDimensions = new ConfigBoolean("Dark Dimension", true,
            "Overrides light tables, \"" + OverrideNetherLightmap.getName() + "\" and \"" + RemoveEndMinValues.getName() + "\" wont work without it.\n\""
                    + EnableTrueDarkness.getName() + "\" must be true for changes to be applied.");

    private static final RemoteDarknessConfigs Instance;
    public static final List<ConfigBase<?>> CosmeticOptions;
    public static final List<ConfigBase<?>> Total = new ArrayList<>();
    public static final List<ConfigTab> tabs = new ArrayList<>();

    public RemoteDarknessConfigs(String name, List<ConfigHotkey> hotkeys, List<ConfigBase<?>> values) {
        super(name, hotkeys, values);
    }

    @Override
    public List<ConfigTab> getConfigTabs() { return tabs; }
    public static RemoteDarknessConfigs getInstance() { return Instance; }

    static {
        CosmeticOptions = List.of(
                EnableTrueDarkness,
                EnableDarkDimensions,
                OverrideNetherLightmap,
                RemoveEndMinValues
        );

        Total.addAll(CosmeticOptions);

        tabs.add(new ConfigTab("General", CosmeticOptions));

        Instance = new RemoteDarknessConfigs(MOD_NAME, null, Total);
    }
}