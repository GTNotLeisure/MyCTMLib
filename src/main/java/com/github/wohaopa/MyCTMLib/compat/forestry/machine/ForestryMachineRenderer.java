package com.github.wohaopa.MyCTMLib.compat.forestry.machine;

import java.awt.Color;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.github.wohaopa.MyCTMLib.client.model.ModelOrientation;
import com.github.wohaopa.MyCTMLib.client.render.block.JsonBlockModelRenderer;
import com.github.wohaopa.MyCTMLib.compat.forestry.machine.ForestryMachineModels.Machine;
import com.github.wohaopa.MyCTMLib.compat.forestry.machine.ForestryMachineModels.Snapshot;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.core.render.EnumTankLevel;
import forestry.core.render.TankRenderInfo;
import forestry.core.tiles.IRenderableTile;

@SideOnly(Side.CLIENT)
public class ForestryMachineRenderer extends TileEntitySpecialRenderer {

    private static final int UNTINTED = 0xFFFFFF;
    private static final int[] NO_TINTS = new int[0];
    public static final int UNROTATED = ModelOrientation.index(ForgeDirection.NORTH, ForgeDirection.UP);

    private static final ClassValue<Method> ORIENTATION = new ClassValue<>() {

        @Override
        protected Method computeValue(Class<?> type) {
            try {
                return type.getMethod("getOrientation");
            } catch (NoSuchMethodException exception) {
                return null;
            }
        }
    };

    private final String model;
    private final TileEntitySpecialRenderer fallback;
    private final JsonBlockModelRenderer renderer = new JsonBlockModelRenderer();

    public ForestryMachineRenderer(String model, TileEntitySpecialRenderer fallback) {
        this.model = model;
        this.fallback = fallback;
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        Snapshot snapshot = ForestryMachineModels.INSTANCE.getSnapshot();
        Machine machine = snapshot == null ? null
            : snapshot.models()
                .get(model);
        if (machine == null || !tile.hasWorldObj()) {
            if (fallback != null) fallback.renderTileEntityAt(tile, x, y, z, partialTicks);
            return;
        }
        ForgeDirection front = ForgeDirection.NORTH;
        TankRenderInfo resourceTank = TankRenderInfo.EMPTY;
        TankRenderInfo productTank = TankRenderInfo.EMPTY;
        if (tile instanceof IRenderableTile renderable) {
            if (renderable.getOrientation() != null) front = renderable.getOrientation();
            if (renderable.getResourceTankInfo() != null) resourceTank = renderable.getResourceTankInfo();
            if (renderable.getProductTankInfo() != null) productTank = renderable.getProductTankInfo();
        } else {
            ForgeDirection orientation = orientation(tile);
            if (orientation != null) front = orientation;
        }
        int orientation = ModelOrientation.index(front, ForgeDirection.UP);
        Geometry geometry = ModelOrientation.ORIENTATIONS[orientation] == null ? null
            : machine.model()
                .orientations()[orientation];
        if (geometry == null) {
            if (fallback != null) fallback.renderTileEntityAt(tile, x, y, z, partialTicks);
            return;
        }

        renderer.prepare(tile.getBlockType(), tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord, false);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glTranslated(x, y, z);
            Tessellator tessellator = TessellatorManager.get();
            tessellator.startDrawingQuads();
            renderer.render(geometry, 0, 0, 0, 0, 0, 0, UNTINTED, NO_TINTS, false, null);
            tessellator.draw();
            drawTank(geometry, 0, machine.resourceTank(), resourceTank, ForestryMachineModels.RESOURCE_TANK);
            drawTank(geometry, 1, machine.productTank(), productTank, ForestryMachineModels.PRODUCT_TANK);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            GL11.glColor4f(1, 1, 1, 1);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            RenderHelper.enableStandardItemLighting();
        }
    }

    private void drawTank(Geometry geometry, int layer, boolean present, TankRenderInfo tank, String kind) {
        if (!present) return;
        int level = clamp(tank.getLevel());
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(ForestryMachineModels.tankTexture(model, kind, level));
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawingQuads();
        renderer.renderTankLayer(geometry, layer, color(tank.getFluidColor()));
        tessellator.draw();
    }

    private static ForgeDirection orientation(TileEntity tile) {
        Method method = ORIENTATION.get(tile.getClass());
        if (method == null) return null;
        try {
            return method.invoke(tile) instanceof ForgeDirection direction ? direction : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static int clamp(EnumTankLevel level) {
        int index = level == null ? 0 : level.ordinal();
        return Math.max(0, Math.min(ForestryMachineModels.TANK_LEVELS - 1, index));
    }

    private static int color(Color color) {
        return color == null ? UNTINTED : color.getRGB() & 0xFFFFFF;
    }
}
