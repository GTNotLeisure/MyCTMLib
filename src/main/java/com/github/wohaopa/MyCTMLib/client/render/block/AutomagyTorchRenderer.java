package com.github.wohaopa.MyCTMLib.client.render.block;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import com.github.wohaopa.MyCTMLib.client.model.AutomagyTorchModel;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;

public final class AutomagyTorchRenderer {

    private static final String TORCH_CLASS = "tuhljin.automagy.blocks.BlockTorchInversion";
    private static final int[] NO_TINTS = new int[0];
    private static final ThreadLocal<JsonBlockModelRenderer> RENDERERS = ThreadLocal
        .withInitial(JsonBlockModelRenderer::new);
    private static volatile Field litField;

    private AutomagyTorchRenderer() {}

    public static boolean render(Block block, IBlockAccess world, int x, int y, int z, RenderBlocks renderer) {
        if (!TORCH_CLASS.equals(
            block.getClass()
                .getName()))
            return false;
        if (!isLit(block)) return false;
        if (world.getBlock(x, y, z) != block || renderer.overrideBlockTexture != null) return false;
        if (!AutomagyTorchModel.FEATURE.isEnabled("redstone_inversion_torch")) return false;
        AutomagyTorchModel.Snapshot snapshot = AutomagyTorchModel.INSTANCE.getSnapshot();
        if (snapshot == null) return false;

        Geometry geometry = snapshot.geometry(world.getBlockMetadata(x, y, z) & 7);
        if (geometry == null) return false;
        JsonBlockModelRenderer context = RENDERERS.get();
        context.prepare(block, world, x, y, z, renderer);
        context.render(geometry, x, y, z, 0, 0, 0, 0xFFFFFF, NO_TINTS, false, null);
        return true;
    }

    private static boolean isLit(Block block) {
        try {
            Field field = litField;
            if (field == null) {
                field = block.getClass()
                    .getDeclaredField("isLit");
                field.setAccessible(true);
                litField = field;
            }
            return field.getBoolean(block);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }
}
