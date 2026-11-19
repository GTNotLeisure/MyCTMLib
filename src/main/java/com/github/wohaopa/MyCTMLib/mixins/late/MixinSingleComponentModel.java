package com.github.wohaopa.MyCTMLib.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.github.wohaopa.MyCTMLib.compat.projectred.ProjectRedTorchModelRenderer;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState.IVertexOperation;

@Pseudo
@Mixin(targets = "mrtjp.projectred.integration.SingleComponentModel", remap = false)
public abstract class MixinSingleComponentModel {

    @Redirect(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/lib/render/CCModel;render([Lcodechicken/lib/render/CCRenderState$IVertexOperation;)V"),
        require = 1)
    private void myctmlib$renderTorch(CCModel model, IVertexOperation[] operations) {
        if (!ProjectRedTorchModelRenderer.render(this, operations)) {
            model.render(operations);
        }
    }
}
