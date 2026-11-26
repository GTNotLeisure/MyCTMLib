package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.model.QuadModel;
import com.github.wohaopa.MyCTMLib.client.resource.JsonModelResources;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class NasaWorkbenchModels {

    public static final NasaWorkbenchModels INSTANCE = new NasaWorkbenchModels();

    private static final String TILE_ENTITY = "micdoodle8.mods.galacticraft.core.tile.TileEntityNasaWorkbench";

    private static final String BLOCK = "micdoodle8.mods.galacticraft.core.blocks.BlockNasaWorkbench";

    private static final ResourceLocation MANIFEST = new ResourceLocation("myctmlib", "workbench/models.json");

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "myctmlib",
        "textures/workbench/nasa_workbench.png");

    private static final NasaWorkbenchRenderer RENDERER = new NasaWorkbenchRenderer();

    private static final NasaWorkbenchItemRenderer ITEM_RENDERER = new NasaWorkbenchItemRenderer();

    private static volatile QuadModel model;

    private static Object original;

    private static Item item;

    private static IItemRenderer previousItemRenderer;

    private static boolean itemRendererInstalled;

    private static int retries;

    public static QuadModel get() {
        return model;
    }

    static void reset() {
        model = null;
        retries = 600;
    }

    static void load(IResourceManager manager, Set<String> enabled) {
        if (!enabled.contains(GalacticraftModels.NASA_WORKBENCH)) return;
        try {
            model = read(new JsonModelResources(manager));
        } catch (RuntimeException exception) {
            MyCTMLib.LOG
                .error("Cannot load the Galacticraft NASA workbench model; keeping the original model", exception);
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || retries <= 0) return;
        retries--;
        install();
    }

    private static QuadModel read(JsonModelResources resources) {
        JsonObject manifest = resources.readObject(MANIFEST)
            .getAsJsonObject("models");
        if (manifest == null) throw new JsonParseException("Missing NASA workbench model manifest");
        if (!manifest.has(GalacticraftModels.NASA_WORKBENCH)) {
            throw new JsonParseException("Missing NASA workbench model entry");
        }
        String path = manifest.get(GalacticraftModels.NASA_WORKBENCH)
            .getAsString();
        QuadModel loaded = new QuadModel(
            TEXTURE,
            GalacticraftModels.NASA_WORKBENCH,
            resources.readObject(new ResourceLocation(path + ".json")));
        MyCTMLib.LOG.info("Loaded the Galacticraft NASA workbench JSON model");
        return loaded;
    }

    public static void install() {
        installRenderer();
        installItemRenderer();
    }

    private static void installRenderer() {
        if (TileEntityRendererDispatcher.instance == null) return;
        Map<?, ?> renderers = TileEntityRendererDispatcher.instance.mapSpecialRenderers;
        if (renderers == null) return;
        Class<?> type = tileEntityClass();
        if (type == null) return;
        synchronized (renderers) {
            Object current = renderers.get(type);
            Object wanted = model == null ? original : RENDERER;
            if (wanted == null || current == wanted) return;
            if (model != null && current != RENDERER) original = current;
            replace(renderers, type, current, wanted);
        }
    }

    private static void installItemRenderer() {
        Item blockItem = item();
        if (blockItem == null) return;
        if (model == null) {
            if (!itemRendererInstalled) return;
            MinecraftForgeClient.registerItemRenderer(blockItem, previousItemRenderer);
            itemRendererInstalled = false;
            return;
        }
        if (itemRendererInstalled) return;
        IItemRenderer current = MinecraftForgeClient
            .getItemRenderer(new ItemStack(blockItem), ItemRenderType.INVENTORY);
        previousItemRenderer = current == ITEM_RENDERER ? null : current;
        MinecraftForgeClient.registerItemRenderer(blockItem, ITEM_RENDERER);
        itemRendererInstalled = true;
    }

    private static void replace(Map<?, ?> renderers, Class<?> type, Object current, Object wanted) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) renderers;
        map.put(type, wanted);
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (entry.getKey() != type && entry.getValue() == current) entry.setValue(wanted);
        }
    }

    private static Class<?> tileEntityClass() {
        try {
            return Class.forName(TILE_ENTITY);
        } catch (ClassNotFoundException | LinkageError exception) {
            return null;
        }
    }

    private static Item item() {
        if (item != null) return item;
        for (Object candidate : Item.itemRegistry) {
            if (!(candidate instanceof ItemBlock)) continue;
            Block block = Block.getBlockFromItem((Item) candidate);
            if (block != null && BLOCK.equals(
                block.getClass()
                    .getName())) {
                item = (Item) candidate;
                return item;
            }
        }
        return null;
    }
}
