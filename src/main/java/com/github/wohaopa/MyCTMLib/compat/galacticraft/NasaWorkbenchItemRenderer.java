package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import com.github.wohaopa.MyCTMLib.client.model.QuadModel;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class NasaWorkbenchItemRenderer implements IItemRenderer {

    private static final float INVENTORY_SCALE = 0.75F;
    private static final float HELD_SCALE = 1.0F;
    private static final float CENTER_Y = 1.292F;
    private static final double BASE_HEIGHT = 0.92D;

    private static final RenderBlocks BASE_RENDERER = new RenderBlocks();

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        switch (type) {
            case INVENTORY:
            case EQUIPPED:
            case EQUIPPED_FIRST_PERSON:
            case ENTITY:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (type == ItemRenderType.ENTITY) {
            return helper == ItemRendererHelper.BLOCK_3D || helper == ItemRendererHelper.ENTITY_ROTATION;
        }
        if (type == ItemRenderType.INVENTORY) {
            return helper == ItemRendererHelper.INVENTORY_BLOCK;
        }
        return helper == ItemRendererHelper.EQUIPPED_BLOCK;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        QuadModel model = NasaWorkbenchModels.get();
        Block block = Block.getBlockFromItem(stack.getItem());
        if (model == null || block == null) return;
        float scale = type == ItemRenderType.INVENTORY ? INVENTORY_SCALE : HELD_SCALE;
        boolean culling = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(-0.5F, -CENTER_Y, -0.5F);
        if (culling) GL11.glDisable(GL11.GL_CULL_FACE);
        renderBase(block, stack.getItemDamage());
        model.render();
        if (culling) GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    private static void renderBase(Block block, int metadata) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        BASE_RENDERER.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, BASE_HEIGHT, 1.0D);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();

        IIcon icon = block.getIcon(0, metadata);
        if (icon != null) {
            tessellator.setNormal(0.0F, -1.0F, 0.0F);
            BASE_RENDERER.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, icon);
        }
        icon = block.getIcon(1, metadata);
        if (icon != null) {
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            BASE_RENDERER.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, icon);
        }
        icon = block.getIcon(2, metadata);
        if (icon != null) {
            tessellator.setNormal(0.0F, 0.0F, -1.0F);
            BASE_RENDERER.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, icon);
        }
        icon = block.getIcon(3, metadata);
        if (icon != null) {
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            BASE_RENDERER.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, icon);
        }
        icon = block.getIcon(4, metadata);
        if (icon != null) {
            tessellator.setNormal(-1.0F, 0.0F, 0.0F);
            BASE_RENDERER.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, icon);
        }
        icon = block.getIcon(5, metadata);
        if (icon != null) {
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            BASE_RENDERER.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, icon);
        }

        tessellator.draw();
    }
}
