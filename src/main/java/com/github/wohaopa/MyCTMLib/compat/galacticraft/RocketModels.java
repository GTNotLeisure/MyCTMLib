package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.resource.JsonModelResources;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RocketModels {

    public static final RocketModels INSTANCE = new RocketModels();

    private static final ResourceLocation MANIFEST = new ResourceLocation("myctmlib", "rockets/models.json");

    private static volatile Map<String, RocketModel> models = Map.of();

    private static int retries;

    public static RocketModel get(String tier) {
        return models.get(tier);
    }

    static void reset() {
        models = Map.of();
        retries = 600;
    }

    static void load(IResourceManager manager, Set<String> enabled) {
        if (enabled.isEmpty()) return;
        try {
            models = Map.copyOf(read(new JsonModelResources(manager), enabled));
        } catch (RuntimeException exception) {
            MyCTMLib.LOG.error("Cannot load Galacticraft rocket models; keeping the original models", exception);
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || retries <= 0) return;
        retries--;
        GalacticraftIntegration.applyEntities();
    }

    private static Map<String, RocketModel> read(JsonModelResources resources, Set<String> enabled) {
        JsonObject manifest = resources.readObject(MANIFEST)
            .getAsJsonObject("models");
        if (manifest == null) throw new JsonParseException("Missing rocket model manifest");
        Map<String, RocketModel> loaded = new HashMap<>();
        for (String tier : GalacticraftModels.ROCKETS) {
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
