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
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.ModelLoc;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Loads the JSON model used by the placed redstone torch and prepares one geometry per torch metadata value.
 * The model is baked after the block atlas has been stitched, so it always uses the sprite that was actually uploaded.
 */
public final class RedstoneTorchModel {

    public static final RedstoneTorchModel INSTANCE = new RedstoneTorchModel();
    public static final String MODEL = "myctmlib:block/redstone_torch_on";
    public static final String TEXTURE = "myctmlib:blocks/redstone_torch";

    private static final int UNROTATED = ModelOrientation.index(ForgeDirection.NORTH, ForgeDirection.UP);
    /** Vanilla wall torches are lifted by 0.2 blocks and lean 0.25 blocks over their 0.625 block height. */
    private static final float WALL_LIFT = 0.2F;
    private static final float WALL_LEAN = 0.4F;

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
        if (ModConfig.redstoneTorchJsonModel) event.map.registerIcon(TEXTURE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterStitch(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0 || !ModConfig.redstoneTorchJsonModel) return;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        try {
            IIcon sprite = event.map.getAtlasSprite(TEXTURE);
            if ("missingno".equals(sprite.getIconName())) {
                throw new JsonParseException("Missing redstone torch model texture: " + TEXTURE);
            }
            JsonModelResources resources = new JsonModelResources(
                Minecraft.getMinecraft()
                    .getResourceManager());
            BakedBlock model = new JsonBlockModel(resources.load(ModelLoc.fromStr(MODEL))).bakeBlock();
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
            // Metadata 1..4 attach the torch to the west, east, north and south neighbour respectively.
            wall[1] = shear(standing, -0.5F, 0.0F, WALL_LEAN, 0.0F);
            wall[2] = shear(standing, 0.5F, 0.0F, -WALL_LEAN, 0.0F);
            wall[3] = shear(standing, 0.0F, -0.5F, 0.0F, WALL_LEAN);
            wall[4] = shear(standing, 0.0F, 0.5F, 0.0F, -WALL_LEAN);
            return new Snapshot(standing, wall);
        }

        public Geometry geometry(int metadata) {
            return metadata >= 1 && metadata <= 4 ? wall[metadata] : standing;
        }

        private static Geometry shear(Geometry base, float dx, float dz, float leanX, float leanZ) {
            ModelQuadView[] quads = new ModelQuadView[base.quads().length];
            float[] shades = new float[quads.length];
            for (int index = 0; index < quads.length; index++) {
                ModelQuadView source = base.quads()[index];
                ModelQuad quad = new ModelQuad(source);
                // The copy constructor does not carry these lighting properties.
                quad.setDirectionalShading(source.hasDirectionalShading());
                quad.setEmissiveness(source.getEmissiveness());
                for (int vertex = 0; vertex < 4; vertex++) {
                    float y = source.getY(vertex);
                    quad.setX(vertex, source.getX(vertex) + dx + leanX * y);
                    quad.setY(vertex, y + WALL_LIFT);
                    quad.setZ(vertex, source.getZ(vertex) + dz + leanZ * y);
                }
                quads[index] = quad;
                shades[index] = quad.hasDirectionalShading() ? ModelISBRH.diffuseLight(quad.getComputedFaceNormal())
                    : 1.0F;
            }
            return new Geometry(quads, shades, base.cullFaces());
        }
    }
}
