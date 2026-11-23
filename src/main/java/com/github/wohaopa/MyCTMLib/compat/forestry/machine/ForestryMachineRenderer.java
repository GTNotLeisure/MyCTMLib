package com.github.wohaopa.MyCTMLib.compat.forestry.machine;

import java.awt.Color;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

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
    private final int[] tints = new int[2];
    private final IIcon[] sprites = new IIcon[2];

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
        if (ModelOrientation.ORIENTATIONS[orientation] == null) {
            if (fallback != null) fallback.renderTileEntityAt(tile, x, y, z, partialTicks);
            return;
        }

        prepareTank(0, machine.resource(), resourceTank);
        prepareTank(1, machine.product(), productTank);
        renderer.prepare(tile.getBlockType(), tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord, false);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glTranslated(x, y, z);
            Tessellator tessellator = TessellatorManager.get();
            tessellator.startDrawingQuads();
            renderer.render(
                machine.model()
                    .orientations()[orientation],
                0,
                0,
                0,
                0,
                0,
                0,
                UNTINTED,
                tints,
                false,
                null,
                sprites);
            tessellator.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            RenderHelper.enableStandardItemLighting();
        }
    }

    private void prepareTank(int index, IIcon[] levels, TankRenderInfo tank) {
        if (levels == null) {
            sprites[index] = null;
            tints[index] = UNTINTED;
            return;
        }
        sprites[index] = levels[clamp(tank.getLevel())];
        tints[index] = color(tank.getFluidColor());
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
