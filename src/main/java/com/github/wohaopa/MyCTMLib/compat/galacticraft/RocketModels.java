package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.resource.JsonModelResources;
import com.github.wohaopa.MyCTMLib.client.resource.ResourcePackFeatureGroup;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorTextureMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RocketModels {

    public static final RocketModels INSTANCE = new RocketModels();

    public static final List<String> NAMES = List
        .of("rocket_tier_1", "rocket_tier_2", "rocket_tier_3", "rocket_tier_4");

    public static final ResourcePackFeatureGroup FEATURE = new ResourcePackFeatureGroup(
        "enableGCRocketJsonModels",
        NAMES);

    private static final ResourceLocation MANIFEST = new ResourceLocation("myctmlib", "rockets/models.json");

    private static volatile Map<String, RocketModel> models = Map.of();

    private static int retries;

    public static RocketModel get(String tier) {
        return models.get(tier);
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || retries <= 0) return;
        retries--;
        GalacticraftIntegration.applyEntities();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void beforeStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        models = Map.of();
        retries = 600;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) {
            GalacticraftIntegration.apply();
            return;
        }
        IResourceManager manager = Minecraft.getMinecraft()
            .getResourceManager();
        FEATURE.onResourceManagerReload(manager);
        Set<String> enabled = FEATURE.getEnabled();
        if (!enabled.isEmpty()) {
            try {
                models = Map.copyOf(load(new JsonModelResources(manager), enabled));
            } catch (RuntimeException exception) {
                MyCTMLib.LOG.error("Cannot load Galacticraft rocket models; keeping the original models", exception);
            }
        }
        GalacticraftIntegration.apply();
    }

    private static Map<String, RocketModel> load(JsonModelResources resources, Set<String> enabled) {
        JsonObject manifest = resources.readObject(MANIFEST)
            .getAsJsonObject("models");
        if (manifest == null) throw new JsonParseException("Missing rocket model manifest");
        Map<String, RocketModel> loaded = new HashMap<>();
        for (String tier : NAMES) {
            if (!enabled.contains(tier)) continue;
            if (!manifest.has(tier)) {
                MyCTMLib.LOG.warn("Rocket model {} is missing from the manifest; keeping the original model", tier);
                continue;
            }
            String path = manifest.get(tier)
                .getAsString();
            loaded.put(tier, new RocketModel(tier, resources.readObject(new ResourceLocation(path + ".json"))));
        }
        if (!loaded.isEmpty()) MyCTMLib.LOG.info("Loaded {} Galacticraft rocket JSON models", loaded.size());
        return loaded;
    }
}
