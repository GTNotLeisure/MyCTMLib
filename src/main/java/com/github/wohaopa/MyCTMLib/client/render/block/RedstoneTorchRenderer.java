package com.github.wohaopa.MyCTMLib.client.render.block;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.github.wohaopa.MyCTMLib.client.model.RedstoneTorchModel;

public class RedstoneTorchRenderer {

    private static final int[] NO_TINTS = new int[0];
    private static final ThreadLocal<JsonBlockModelRenderer> RENDERERS = ThreadLocal
        .withInitial(JsonBlockModelRenderer::new);

    private RedstoneTorchRenderer() {}

    public static boolean render(Block block, IBlockAccess world, int x, int y, int z, RenderBlocks renderer) {
        boolean lit = block == Blocks.redstone_torch;
        boolean unlit = block == Blocks.unlit_redstone_torch;
        if (!lit && !unlit) return false;
        if (world.getBlock(x, y, z) != block) return false;
        int metadata = world.getBlockMetadata(x, y, z) & 7;
        if (!lit && (metadata < 1 || metadata > 4)) return false;
        RedstoneTorchModel.Snapshot snapshot = RedstoneTorchModel.INSTANCE.getSnapshot();
        if (snapshot == null) return false;

        Geometry geometry = snapshot.geometry(metadata);
        if (geometry == null) return false;
        IIcon override = renderer.overrideBlockTexture;
        if (override == null && unlit) override = Blocks.unlit_redstone_torch.getBlockTextureFromSide(0);
        JsonBlockModelRenderer context = RENDERERS.get();
        context.prepare(block, world, x, y, z, renderer);
        context.render(geometry, x, y, z, 0, 0, 0, 0xFFFFFF, NO_TINTS, false, override);
        return true;
    }
}
