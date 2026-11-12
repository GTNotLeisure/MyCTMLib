package com.github.wohaopa.MyCTMLib.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.BakedBlock;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.github.wohaopa.MyCTMLib.client.resource.JsonModelResources;
import com.github.wohaopa.MyCTMLib.config.ModConfig;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorTextureMap;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.ModelLoc;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Loads the four redstone comparator JSON models (compare/subtract x unpowered/powered) and caches one geometry
 * for each metadata direction.
 */
public final class RedstoneComparatorModel {

    public static final RedstoneComparatorModel INSTANCE = new RedstoneComparatorModel();
    public static final String MODEL_ROOT = "myctmlib:block/mc/";
    public static final String TEXTURE_ROOT = "myctmlib:blocks/mc/";

    private static final String[] TEXTURES = { TEXTURE_ROOT + "smooth_stone", TEXTURE_ROOT + "comparator",
        TEXTURE_ROOT + "comparator_on", TEXTURE_ROOT + "redstone_torch", TEXTURE_ROOT + "redstone_torch_off" };

    private volatile Snapshot snapshot;

    private RedstoneComparatorModel() {}

    public Snapshot getSnapshot() {
        return snapshot;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void beforeStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        snapshot = null;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        if (!ModConfig.redstoneComparatorJsonModel) return;
        for (String texture : TEXTURES) event.map.registerIcon(texture);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterStitch(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0 || !ModConfig.redstoneComparatorJsonModel) return;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        try {
            for (String texture : TEXTURES) {
                IIcon sprite = event.map.getAtlasSprite(texture);
                if ("missingno".equals(sprite.getIconName())) {
                    throw new JsonParseException("Missing redstone comparator model texture: " + texture);
                }
            }
            Snapshot loaded = Snapshot.load(
                new JsonModelResources(
                    Minecraft.getMinecraft()
                        .getResourceManager()));
            snapshot = loaded;
            MyCTMLib.LOG.info("Loaded 4 redstone comparator JSON models");
        } catch (RuntimeException exception) {
            snapshot = null;
            MyCTMLib.LOG
                .error("Cannot load the redstone comparator JSON models; keeping the vanilla renderer", exception);
        }
    }

    public record Snapshot(Geometry[] offCompare, Geometry[] onCompare, Geometry[] offSubtract,
        Geometry[] onSubtract) {

        public Snapshot {
            offCompare = offCompare.clone();
            onCompare = onCompare.clone();
            offSubtract = offSubtract.clone();
            onSubtract = onSubtract.clone();
        }

        public static Snapshot load(JsonModelResources resources) {
            Geometry[] offCompare = new Geometry[4];
            Geometry[] onCompare = new Geometry[4];
            Geometry[] offSubtract = new Geometry[4];
            Geometry[] onSubtract = new Geometry[4];

            for (int power = 0; power < 2; power++) {
                for (int subtract = 0; subtract < 2; subtract++) {
                    String name = "comparator" + (power == 1 ? "_on" : "")
                        + (subtract == 1 ? "_subtract" : "");
                    BakedBlock baked = new JsonBlockModel(resources.load(ModelLoc.fromStr(MODEL_ROOT + name)))
                        .bakeBlock();
                    Geometry[] target = power == 1 ? (subtract == 1 ? onSubtract : onCompare)
                        : (subtract == 1 ? offSubtract : offCompare);
                    for (int direction = 0; direction < 4; direction++) {
                        Geometry geometry = baked.orientations()[orientation(direction)];
                        if (geometry == null) {
                            throw new JsonParseException("Missing comparator orientation: " + name);
                        }
                        target[direction] = geometry;
                    }
                }
            }
            return new Snapshot(offCompare, onCompare, offSubtract, onSubtract);
        }

        public Geometry geometry(boolean powered, boolean subtract, int metadata) {
            Geometry[] set = powered ? (subtract ? onSubtract : onCompare)
                : (subtract ? offSubtract : offCompare);
            return set[metadata & 3];
        }

        /**
         * The unrotated model has its two front torches on the south side, matching metadata 0. Rotate so the
         * model's front (+z) follows the metadata output direction.
         */
        private static int orientation(int direction) {
            ForgeDirection output = switch (direction & 3) {
                case 0 -> ForgeDirection.SOUTH;
                case 1 -> ForgeDirection.WEST;
                case 2 -> ForgeDirection.NORTH;
                default -> ForgeDirection.EAST;
            };
            return ModelOrientation.index(output.getOpposite(), ForgeDirection.UP);
        }
    }
}
