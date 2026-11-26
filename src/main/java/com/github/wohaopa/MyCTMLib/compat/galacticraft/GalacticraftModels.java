package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.client.event.TextureStitchEvent;

import com.github.wohaopa.MyCTMLib.client.resource.ResourcePackFeatureGroup;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorTextureMap;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GalacticraftModels {

    public static final GalacticraftModels INSTANCE = new GalacticraftModels();

    public static final String NASA_WORKBENCH = "nasa_workbench";

    public static final List<String> ROCKETS = List
        .of("rocket_tier_1", "rocket_tier_2", "rocket_tier_3", "rocket_tier_4");

    public static final List<String> KEYS = List.of(
        "rocket_tier_1",
        "rocket_tier_2",
        "rocket_tier_3",
        "rocket_tier_4",
        NASA_WORKBENCH);

    public static final ResourcePackFeatureGroup FEATURE = new ResourcePackFeatureGroup("enableGCJsonModels", KEYS);

    private GalacticraftModels() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void beforeStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        RocketModels.reset();
        NasaWorkbenchModels.reset();
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) {
            GalacticraftIntegration.apply();
            NasaWorkbenchModels.install();
            return;
        }
        IResourceManager manager = Minecraft.getMinecraft()
            .getResourceManager();
        FEATURE.onResourceManagerReload(manager);
        RocketModels.load(manager, FEATURE.getEnabled());
        GalacticraftIntegration.apply();
        NasaWorkbenchModels.load(manager, FEATURE.getEnabled());
        NasaWorkbenchModels.install();
    }
}
