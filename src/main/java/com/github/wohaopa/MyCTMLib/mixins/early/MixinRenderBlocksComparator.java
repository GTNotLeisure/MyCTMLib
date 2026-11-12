package com.github.wohaopa.MyCTMLib.mixins.early;

import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.wohaopa.MyCTMLib.client.render.block.RedstoneComparatorRenderer;

@Mixin(value = RenderBlocks.class, priority = 1100, remap = true)
public abstract class MixinRenderBlocksComparator {

    @Shadow(remap = true)
    public IBlockAccess blockAccess;

    @Inject(method = "renderBlockRedstoneComparator", at = @At("HEAD"), cancellable = true, require = 1)
    private void myctmlib$renderRedstoneComparator(BlockRedstoneComparator block, int x, int y, int z,
        CallbackInfoReturnable<Boolean> cir) {
        if (blockAccess == null) return;
        if (RedstoneComparatorRenderer.render(block, blockAccess, x, y, z, (RenderBlocks) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
