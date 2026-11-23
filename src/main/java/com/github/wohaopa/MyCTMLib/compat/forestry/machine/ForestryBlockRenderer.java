package com.github.wohaopa.MyCTMLib.compat.forestry.machine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;

import org.lwjgl.opengl.GL11;

import com.github.wohaopa.MyCTMLib.client.render.block.JsonBlockModelRenderer;
import com.github.wohaopa.MyCTMLib.compat.forestry.machine.ForestryMachineModels.Machine;
import com.github.wohaopa.MyCTMLib.compat.forestry.machine.ForestryMachineModels.Snapshot;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.core.render.IBlockRenderer;

@SideOnly(Side.CLIENT)
public class ForestryBlockRenderer implements IBlockRenderer {

    private static final int UNTINTED = 0xFFFFFF;

    private final String model;
    private final IBlockRenderer fallback;
    private final JsonBlockModelRenderer renderer = new JsonBlockModelRenderer();
    private final int[] tints = { UNTINTED, UNTINTED };

    public ForestryBlockRenderer(String model, IBlockRenderer fallback) {
        this.model = model;
        this.fallback = fallback;
    }

    @Override
    public void inventoryRender(double x, double y, double z) {
        Snapshot snapshot = ForestryMachineModels.INSTANCE.getSnapshot();
        Machine machine = snapshot == null ? null
            : snapshot.models()
                .get(model);
        if (machine == null) {
            if (fallback != null) fallback.inventoryRender(x, y, z);
            return;
        }
        renderer.prepareInventory();

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glTranslated(x, y, z);
            Tessellator tessellator = TessellatorManager.get();
            tessellator.startDrawingQuads();
            renderer.render(
                machine.model()
                    .orientations()[ForestryMachineRenderer.UNROTATED],
                0,
                0,
                0,
                0,
                0,
                0,
                UNTINTED,
                tints,
                false,
                null);
            tessellator.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
