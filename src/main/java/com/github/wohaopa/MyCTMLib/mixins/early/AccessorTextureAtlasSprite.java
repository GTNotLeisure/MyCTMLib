package com.github.wohaopa.MyCTMLib.mixins.early;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TextureAtlasSprite.class, remap = true)
public interface AccessorTextureAtlasSprite {

    @Accessor(value = "animationMetadata", remap = true)
    AnimationMetadataSection getAnimationMetadata();

    @Accessor(value = "animationMetadata", remap = true)
    void setAnimationMetadata(AnimationMetadataSection metadata);

    @Invoker(value = "fixTransparentPixels", remap = true)
    void myctmlib$fixTransparentPixels(int[][] pixels);
}
