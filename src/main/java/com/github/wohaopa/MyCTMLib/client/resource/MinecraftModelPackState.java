package com.github.wohaopa.MyCTMLib.client.resource;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.TextureStitchEvent;

import com.github.wohaopa.MyCTMLib.mixins.early.AccessorTextureMap;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class MinecraftModelPackState {

    public static final MinecraftModelPackState INSTANCE = new MinecraftModelPackState();
    public static final ResourcePackFeatureGroup FEATURE = new ResourcePackFeatureGroup(
        "enableMinecraftJsonModels",
        Set.of("redstone_torch", "repeater", "comparator"));

    private MinecraftModelPackState() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        FEATURE.onResourceManagerReload(
            Minecraft.getMinecraft()
                .getResourceManager());
    }
}
