package com.github.wohaopa.MyCTMLib.client.render.block;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;

import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.github.wohaopa.MyCTMLib.client.model.RedstoneTorchModel;

public class RedstoneTorchRenderer {

    private static final int[] NO_TINTS = new int[0];
    private static final ThreadLocal<JsonBlockModelRenderer> RENDERERS = ThreadLocal
        .withInitial(JsonBlockModelRenderer::new);

    private RedstoneTorchRenderer() {}

    public static boolean render(Block block, IBlockAccess world, int x, int y, int z, RenderBlocks renderer) {
        if (block != Blocks.redstone_torch || world.getBlock(x, y, z) != block) return false;
        RedstoneTorchModel.Snapshot snapshot = RedstoneTorchModel.INSTANCE.getSnapshot();
        if (snapshot == null) return false;

        Geometry geometry = snapshot.geometry(world.getBlockMetadata(x, y, z) & 7);
        if (geometry == null) return false;
        JsonBlockModelRenderer context = RENDERERS.get();
        context.prepare(block, world, x, y, z, renderer);
        context.render(geometry, x, y, z, 0, 0, 0, 0xFFFFFF, NO_TINTS, false, renderer.overrideBlockTexture);
        return true;
    }
}
