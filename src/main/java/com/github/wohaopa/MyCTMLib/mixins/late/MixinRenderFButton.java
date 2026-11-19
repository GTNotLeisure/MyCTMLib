package com.github.wohaopa.MyCTMLib.mixins.late;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.github.wohaopa.MyCTMLib.compat.projectred.ProjectRedTorchFeature;

@Pseudo
@Mixin(targets = "mrtjp.projectred.illumination.RenderFButton$", remap = false)
public abstract class MixinRenderFButton {

    @Redirect(
        method = "drawExtras(Lcodechicken/lib/vec/Transformation;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getIcon(II)Lnet/minecraft/util/IIcon;",
            remap = true),
        require = 1)
    private IIcon myctmlib$torchIcon(Block block, int side, int metadata) {
        IIcon icon = ProjectRedTorchFeature.getOnIcon();
        return icon == null ? block.getIcon(side, metadata) : icon;
    }
}
