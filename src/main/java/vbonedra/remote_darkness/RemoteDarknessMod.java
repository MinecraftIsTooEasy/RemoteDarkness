package vbonedra.remote_darkness;

import fi.dy.masa.malilib.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RemoteDarknessMod implements ClientModInitializer {
    public static final String MOD_ID = "remote_darkness";
    public static final String MOD_NAME = "RemoteDarkness";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Override
    public void onInitializeClient() {
        RemoteDarknessConfigs.getInstance().load();
        ConfigManager.getInstance().registerConfig(RemoteDarknessConfigs.getInstance());
    }
}
