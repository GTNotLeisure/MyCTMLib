package com.github.wohaopa.MyCTMLib.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.BakedBlock;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.github.wohaopa.MyCTMLib.client.resource.JsonModelResources;
import com.github.wohaopa.MyCTMLib.client.resource.MinecraftModelPackState;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorTextureMap;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.ModelLoc;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RedstoneTorchModel {

    public static final RedstoneTorchModel INSTANCE = new RedstoneTorchModel();
    public static final String MODEL = "myctmlib:block/minecraft/redstone_torch_on";
    public static final String TEXTURE = "myctmlib:blocks/minecraft/redstone_torch";

    private static final int UNROTATED = ModelOrientation.index(ForgeDirection.NORTH, ForgeDirection.UP);
    private static final float WALL_LIFT = 0.2F;
    private static final float WALL_HEIGHT = 0.625F;
    private static final float WALL_ANGLE = (float) Math.asin(0.4D);
    private static final float WALL_COS = (float) Math.cos(WALL_ANGLE);
    private static final float WALL_BASE_Y = WALL_LIFT + WALL_HEIGHT * (1.0F - WALL_COS);

    private volatile Snapshot snapshot;

    private RedstoneTorchModel() {}

    public Snapshot getSnapshot() {
        return snapshot;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void beforeStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        snapshot = null;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        if (MinecraftModelPackState.FEATURE.isEnabled("redstone_torch")) event.map.registerIcon(TEXTURE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterStitch(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0 || !MinecraftModelPackState.FEATURE.isEnabled("redstone_torch")) return;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        try {
            IIcon sprite = event.map.getAtlasSprite(TEXTURE);
            if ("missingno".equals(sprite.getIconName())) {
                throw new JsonParseException("Missing redstone torch model texture: " + TEXTURE);
            }
            JsonModelResources resources = new JsonModelResources(
                Minecraft.getMinecraft()
                    .getResourceManager());
            BakedBlock model = new JsonBlockModel(resources.load(ModelLoc.fromStr(MODEL))).bakeBlock(UNROTATED);
            snapshot = Snapshot.of(model);
            MyCTMLib.LOG.info("Loaded the redstone torch JSON model");
        } catch (RuntimeException exception) {
            snapshot = null;
            MyCTMLib.LOG.error("Cannot load the redstone torch JSON model; keeping the vanilla renderer", exception);
        }
    }

    public record Snapshot(Geometry standing, Geometry[] wall) {

        public Snapshot {
            wall = wall.clone();
        }

        public static Snapshot of(BakedBlock model) {
            Geometry standing = model.orientations()[UNROTATED];
            Geometry[] wall = new Geometry[5];
            wall[1] = rotate(standing, -0.5F, 0.0F, false, -WALL_ANGLE);
            wall[2] = rotate(standing, 0.5F, 0.0F, false, WALL_ANGLE);
            wall[3] = rotate(standing, 0.0F, -0.5F, true, WALL_ANGLE);
            wall[4] = rotate(standing, 0.0F, 0.5F, true, -WALL_ANGLE);
            return new Snapshot(standing, wall);
        }

        public Geometry geometry(int metadata) {
            return metadata >= 1 && metadata <= 4 ? wall[metadata] : standing;
        }

        private static Geometry rotate(Geometry base, float dx, float dz, boolean aroundX, float angle) {
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            ModelQuadView[] quads = new ModelQuadView[base.quads().length];
            float[] shades = new float[quads.length];
            ForgeDirection[] cullFaces = new ForgeDirection[quads.length];
            for (int index = 0; index < quads.length; index++) {
                ModelQuadView source = base.quads()[index];
                ModelQuad quad = new ModelQuad(source);
                quad.setDirectionalShading(source.hasDirectionalShading());
                quad.setEmissiveness(source.getEmissiveness());
                for (int vertex = 0; vertex < 4; vertex++) {
                    float x = source.getX(vertex) - 0.5F;
                    float y = source.getY(vertex);
                    float z = source.getZ(vertex) - 0.5F;
                    float nx;
                    float ny;
                    float nz;
                    if (aroundX) {
                        nx = x;
                        ny = y * cos - z * sin;
                        nz = y * sin + z * cos;
                    } else {
                        nx = x * cos - y * sin;
                        ny = x * sin + y * cos;
                        nz = z;
                    }
                    quad.setX(vertex, nx + 0.5F + dx);
                    quad.setY(vertex, ny + WALL_BASE_Y);
                    quad.setZ(vertex, nz + 0.5F + dz);
                }
                quads[index] = quad;
                cullFaces[index] = ForgeDirection.UNKNOWN;
                shades[index] = quad.hasDirectionalShading() ? ModelISBRH.diffuseLight(quad.getComputedFaceNormal())
                    : 1.0F;
            }
            return new Geometry(quads, shades, cullFaces);
        }
    }
}
