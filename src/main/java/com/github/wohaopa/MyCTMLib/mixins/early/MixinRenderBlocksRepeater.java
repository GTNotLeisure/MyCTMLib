package com.github.wohaopa.MyCTMLib.mixins.early;

import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.wohaopa.MyCTMLib.client.render.block.RedstoneRepeaterRenderer;

@Mixin(value = RenderBlocks.class, priority = 1100, remap = true)
public abstract class MixinRenderBlocksRepeater {

    @Shadow(remap = true)
    public IBlockAccess blockAccess;

    @Inject(method = "renderBlockRepeater", at = @At("HEAD"), cancellable = true, require = 1)
    private void myctmlib$renderRedstoneRepeater(BlockRedstoneRepeater block, int x, int y, int z,
        CallbackInfoReturnable<Boolean> cir) {
        if (blockAccess == null) return;
        if (RedstoneRepeaterRenderer.render(block, blockAccess, x, y, z, (RenderBlocks) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
