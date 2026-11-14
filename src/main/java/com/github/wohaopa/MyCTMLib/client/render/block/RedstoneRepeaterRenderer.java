package com.github.wohaopa.MyCTMLib.client.render.block;

import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;

import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.github.wohaopa.MyCTMLib.client.model.RedstoneRepeaterModel;

/** Renders redstone repeaters with the JSON models, falling back to the vanilla renderer when they are unavailable. */
public final class RedstoneRepeaterRenderer {

    private static final int[] NO_TINTS = new int[0];
    private static final ThreadLocal<JsonBlockModelRenderer> RENDERERS = ThreadLocal
        .withInitial(JsonBlockModelRenderer::new);

    private RedstoneRepeaterRenderer() {}

    public static boolean render(BlockRedstoneRepeater block, IBlockAccess world, int x, int y, int z,
        RenderBlocks renderer) {
        if (block != Blocks.powered_repeater && block != Blocks.unpowered_repeater) return false;
        // The repeater model uses several textures, so a single override icon cannot represent it correctly.
        if (world.getBlock(x, y, z) != block || renderer.overrideBlockTexture != null) return false;

        RedstoneRepeaterModel.Snapshot snapshot = RedstoneRepeaterModel.INSTANCE.getSnapshot();
        if (snapshot == null) return false;

        int metadata = world.getBlockMetadata(x, y, z);
        boolean powered = block == Blocks.powered_repeater;
        boolean locked = block.func_149910_g(world, x, y, z, metadata);
        Geometry geometry = snapshot.geometry(powered, locked, metadata);
        if (geometry == null) return false;

        JsonBlockModelRenderer context = RENDERERS.get();
        context.prepare(block, world, x, y, z, renderer);
        context.render(geometry, x, y, z, 0, 0, 0, 0xFFFFFF, NO_TINTS, false, null);
        return true;
    }
}
