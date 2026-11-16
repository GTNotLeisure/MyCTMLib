package com.github.wohaopa.MyCTMLib.mixins.early;

import net.minecraft.client.resources.SimpleReloadableResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.wohaopa.MyCTMLib.client.ctm.Textures;

@Mixin(value = SimpleReloadableResourceManager.class, remap = true)
public class MixinSimpleReloadableResourceManager {

    @Inject(method = "clearResources", at = @At("HEAD"))
    private void onClearResources(CallbackInfo ci) {
        Textures.clearTextureRegistrations();
    }
}
