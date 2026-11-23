package com.github.wohaopa.MyCTMLib.compat.forestry.machine;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.BakedBlock;
import com.github.wohaopa.MyCTMLib.client.resource.JsonModelResources;
import com.github.wohaopa.MyCTMLib.client.resource.ResourcePackFeatureGroup;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorTextureMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.ModelLoc;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ForestryMachineModels {

    public static final ForestryMachineModels INSTANCE = new ForestryMachineModels();

    public static final List<String> NAMES = List
        .of("bottler", "carpenter", "centrifuge", "fermenter", "moistener", "squeezer", "still", "escritoire");

    public static final String RESOURCE_TANK = "resource";
    public static final String PRODUCT_TANK = "product";
    public static final int TANK_LEVELS = 5;

    public static final ResourcePackFeatureGroup FEATURE = new ResourcePackFeatureGroup(
        "enableForestryJsonModels",
        NAMES);

    private static final ResourceLocation MANIFEST = new ResourceLocation("myctmlib", "forestry/models.json");
    private static final String TANK_TEXTURE = "textures/blocks/machines/";

    private volatile Snapshot snapshot;
    private final Map<String, JsonBlockModel> pending = new LinkedHashMap<>();
    private final Map<String, Set<String>> pendingTanks = new HashMap<>();

    public static boolean has(String name) {
        return NAMES.contains(name);
    }

    public static ResourceLocation tankTexture(String name, String tank, int level) {
        return new ResourceLocation(
            "myctmlib",
            TANK_TEXTURE + name + "/tank_" + (RESOURCE_TANK.equals(tank) ? "res" : "prod") + "_" + level + ".png");
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void beforeStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        snapshot = null;
        pending.clear();
        pendingTanks.clear();
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        IResourceManager manager = Minecraft.getMinecraft()
            .getResourceManager();
        FEATURE.onResourceManagerReload(manager);
        Set<String> enabled = FEATURE.getEnabled();
        if (enabled.isEmpty()) return;
        JsonModelResources resources = new JsonModelResources(manager);
        try {
            JsonObject models = resources.readObject(MANIFEST)
                .getAsJsonObject("models");
            if (models == null) throw new JsonParseException("Missing Forestry machine models");
            for (String name : NAMES) {
                if (!enabled.contains(name)) continue;
                JsonObject entry = models.getAsJsonObject(name);
                if (entry == null) {
                    MyCTMLib.LOG
                        .warn("Forestry model {} is missing from the manifest; keeping its original renderer", name);
                    continue;
                }
                JsonBlockModel model = new JsonBlockModel(
                    resources.load(
                        ModelLoc.fromStr(
                            entry.get("model")
                                .getAsString())));
                pending.put(name, model);
                for (String texture : model.getTextures()
                    .values()) {
                    if (!texture.startsWith("#")) event.map.registerIcon(texture);
                }
                JsonArray declared = entry.getAsJsonArray("tanks");
                Set<String> tanks = new LinkedHashSet<>();
                if (declared != null) {
                    for (JsonElement tank : declared) tanks.add(tank.getAsString());
                }
                Set<String> present = new LinkedHashSet<>();
                for (String tank : tanks) {
                    if (!RESOURCE_TANK.equals(tank) && !PRODUCT_TANK.equals(tank)) {
                        throw new JsonParseException("Unknown Forestry tank " + tank + " on " + name);
                    }
                    if (hasTankTextures(manager, name, tank)) present.add(tank);
                    else MyCTMLib.LOG.warn("Missing {} tank textures of {}; the tank stays empty", tank, name);
                }
                pendingTanks.put(name, present);
            }
        } catch (RuntimeException exception) {
            pending.clear();
            pendingTanks.clear();
            MyCTMLib.LOG.error("Cannot load Forestry machine models; keeping the original renderers", exception);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterStitch(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0 || pending.isEmpty()) return;
        try {
            Map<String, Machine> models = new HashMap<>();
            for (Map.Entry<String, JsonBlockModel> entry : pending.entrySet()) {
                for (String texture : entry.getValue()
                    .getTextures()
                    .values()) {
                    if (!texture.startsWith("#") && "missingno".equals(
                        event.map.getAtlasSprite(texture)
                            .getIconName())) {
                        throw new JsonParseException("Missing Forestry model texture: " + texture);
                    }
                }
                String name = entry.getKey();
                Set<String> tanks = pendingTanks.getOrDefault(name, Set.of());
                models.put(
                    name,
                    new Machine(
                        entry.getValue()
                            .bakeBlock(),
                        tanks.contains(RESOURCE_TANK),
                        tanks.contains(PRODUCT_TANK)));
            }
            snapshot = new Snapshot(Map.copyOf(models));
            MyCTMLib.LOG.info("Loaded {} Forestry machine JSON models", models.size());
        } catch (RuntimeException exception) {
            MyCTMLib.LOG.error("Cannot bake Forestry machine models; keeping the original renderers", exception);
        } finally {
            pending.clear();
            pendingTanks.clear();
        }
    }

    private static boolean hasTankTextures(IResourceManager manager, String name, String tank) {
        for (int level = 0; level < TANK_LEVELS; level++) {
            try {
                manager.getResource(tankTexture(name, tank, level))
                    .getInputStream()
                    .close();
            } catch (IOException exception) {
                return false;
            }
        }
        return true;
    }

    public record Machine(BakedBlock model, boolean resourceTank, boolean productTank) {}

    public record Snapshot(Map<String, Machine> models) {}
}
