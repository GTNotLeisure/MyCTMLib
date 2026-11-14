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
 * Loads all redstone repeater JSON variants after the block atlas stitch and caches one geometry for every
 * metadata/direction, power and lock state combination.
 */
public final class RedstoneRepeaterModel {

    public static final RedstoneRepeaterModel INSTANCE = new RedstoneRepeaterModel();
    public static final String MODEL_ROOT = "myctmlib:block/mc/";
    public static final String TEXTURE_ROOT = "myctmlib:blocks/mc/";

    private static final String[] TEXTURES = { TEXTURE_ROOT + "smooth_stone", TEXTURE_ROOT + "repeater",
        TEXTURE_ROOT + "repeater_on", TEXTURE_ROOT + "redstone_torch", TEXTURE_ROOT + "redstone_torch_off",
        TEXTURE_ROOT + "bedrock" };

    private volatile Snapshot snapshot;

    private RedstoneRepeaterModel() {}

    public Snapshot getSnapshot() {
        return snapshot;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void beforeStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        snapshot = null;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        if (!ModConfig.redstoneRepeaterJsonModel) return;
        for (String texture : TEXTURES) event.map.registerIcon(texture);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterStitch(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0 || !ModConfig.redstoneRepeaterJsonModel) return;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        try {
            for (String texture : TEXTURES) {
                IIcon sprite = event.map.getAtlasSprite(texture);
                if ("missingno".equals(sprite.getIconName())) {
                    throw new JsonParseException("Missing redstone repeater model texture: " + texture);
                }
            }
            Snapshot loaded = Snapshot.load(
                new JsonModelResources(
                    Minecraft.getMinecraft()
                        .getResourceManager()));
            snapshot = loaded;
            MyCTMLib.LOG.info("Loaded 16 redstone repeater JSON models");
        } catch (RuntimeException exception) {
            snapshot = null;
            MyCTMLib.LOG
                .error("Cannot load the redstone repeater JSON models; keeping the vanilla renderer", exception);
        }
    }

    public record Snapshot(Geometry[] unpowered, Geometry[] unpoweredLocked, Geometry[] powered,
        Geometry[] poweredLocked) {

        public Snapshot {
            unpowered = unpowered.clone();
            unpoweredLocked = unpoweredLocked.clone();
            powered = powered.clone();
            poweredLocked = poweredLocked.clone();
        }

        public static Snapshot load(JsonModelResources resources) {
            Geometry[] unpowered = new Geometry[16];
            Geometry[] unpoweredLocked = new Geometry[16];
            Geometry[] powered = new Geometry[16];
            Geometry[] poweredLocked = new Geometry[16];

            for (int delay = 0; delay < 4; delay++) {
                for (int power = 0; power < 2; power++) {
                    for (int lock = 0; lock < 2; lock++) {
                        String name = "repeater_" + (delay + 1) + "tick"
                            + (power == 1 ? "_on" : "")
                            + (lock == 1 ? "_locked" : "");
                        BakedBlock baked = new JsonBlockModel(resources.load(ModelLoc.fromStr(MODEL_ROOT + name)))
                            .bakeBlock();
                        Geometry[] target = power == 0 ? (lock == 0 ? unpowered : unpoweredLocked)
                            : (lock == 0 ? powered : poweredLocked);
                        for (int direction = 0; direction < 4; direction++) {
                            Geometry geometry = baked.orientations()[orientation(direction)];
                            if (geometry == null) {
                                throw new JsonParseException("Missing repeater orientation: " + name);
                            }
                            target[(delay << 2) | direction] = geometry;
                        }
                    }
                }
            }
            return new Snapshot(unpowered, unpoweredLocked, powered, poweredLocked);
        }

        public Geometry geometry(boolean isPowered, boolean isLocked, int metadata) {
            Geometry[] set = isPowered ? (isLocked ? poweredLocked : powered)
                : (isLocked ? unpoweredLocked : unpowered);
            return set[metadata & 15];
        }

        /**
         * Metadata 0 is the vanilla repeater with its input on the south side; the unrotated model in {@code mc/}
         * uses the same north/south layout, so metadata 0 keeps the identity orientation.
         */
        private static int orientation(int direction) {
            ForgeDirection input = switch (direction & 3) {
                case 0 -> ForgeDirection.SOUTH;
                case 1 -> ForgeDirection.WEST;
                case 2 -> ForgeDirection.NORTH;
                default -> ForgeDirection.EAST;
            };
            return ModelOrientation.index(input.getOpposite(), ForgeDirection.UP);
        }
    }
}
