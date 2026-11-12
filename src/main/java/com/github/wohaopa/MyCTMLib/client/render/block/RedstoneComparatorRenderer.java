package com.github.wohaopa.MyCTMLib.client.render.block;

import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;

import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.github.wohaopa.MyCTMLib.client.model.RedstoneComparatorModel;

/** Renders redstone comparators with the JSON models, falling back to the vanilla renderer when unavailable. */
public final class RedstoneComparatorRenderer {

    private static final int[] NO_TINTS = new int[0];
    private static final ThreadLocal<JsonBlockModelRenderer> RENDERERS = ThreadLocal
        .withInitial(JsonBlockModelRenderer::new);

    private RedstoneComparatorRenderer() {}

    public static boolean render(BlockRedstoneComparator block, IBlockAccess world, int x, int y, int z,
        RenderBlocks renderer) {
        if (block != Blocks.powered_comparator && block != Blocks.unpowered_comparator) return false;
        // The comparator model uses several textures, so a single override icon cannot represent it correctly.
        if (world.getBlock(x, y, z) != block || renderer.overrideBlockTexture != null) return false;

        RedstoneComparatorModel.Snapshot snapshot = RedstoneComparatorModel.INSTANCE.getSnapshot();
        if (snapshot == null) return false;

        int metadata = world.getBlockMetadata(x, y, z);
        // Vanilla treats both the powered block instance and metadata bit 3 as powered.
        boolean powered = block == Blocks.powered_comparator || (metadata & 8) != 0;
        boolean subtract = (metadata & 4) != 0;
        Geometry geometry = snapshot.geometry(powered, subtract, metadata);
        if (geometry == null) return false;

        JsonBlockModelRenderer context = RENDERERS.get();
        context.prepare(block, world, x, y, z, renderer);
        context.render(geometry, x, y, z, 0, 0, 0, 0xFFFFFF, NO_TINTS, false, null);
        return true;
    }
}
