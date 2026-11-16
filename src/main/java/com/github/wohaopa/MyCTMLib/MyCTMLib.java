package com.github.wohaopa.MyCTMLib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.wohaopa.MyCTMLib.client.ClientLifecycle;
import com.github.wohaopa.MyCTMLib.config.ModConfig;
import com.gtnewhorizon.gtnhlib.config.ConfigException;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = Tags.MODID,
    version = Tags.VERSION,
    name = Tags.MODNAME,
    dependencies = "required-after:gtnhlib;",
    guiFactory = "com.github.wohaopa.MyCTMLib.client.config.MyCTMLibGuiFactory",
    acceptedMinecraftVersions = "[1.7.10]")
public class MyCTMLib {

    public static boolean isInit = false;
    public static final String MODID = "MyCTMLib";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws ConfigException {
        ModConfig.registerConfig();

        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            ClientLifecycle.preInit();
        }
    }

    @Mod.EventHandler
    public void completeInit(FMLLoadCompleteEvent event) {
        isInit = true;
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            ClientLifecycle.loadComplete();
        }
    }

}
