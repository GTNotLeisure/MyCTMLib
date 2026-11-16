package com.github.wohaopa.MyCTMLib.compat.ic2;

import static com.github.wohaopa.MyCTMLib.client.ctm.Textures.ctmAltMap;
import static com.github.wohaopa.MyCTMLib.client.ctm.Textures.ctmIconMap;
import static com.github.wohaopa.MyCTMLib.client.ctm.Textures.ctmReplaceMap;

import java.io.IOException;
import java.io.InputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.util.ResourceLocation;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.ctm.CTMConfig;
import com.github.wohaopa.MyCTMLib.client.ctm.CTMIconManager;
import com.github.wohaopa.MyCTMLib.client.texture.InterpolatedIcon;
import com.github.wohaopa.MyCTMLib.client.texture.NewTextureAtlasSprite;
import com.github.wohaopa.MyCTMLib.config.ModConfig;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorSimpleResource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class IC2TextureLoader {

    public static void handleTexture(IIconRegister iconRegister, String originName) {
        if (!(iconRegister instanceof TextureMap atlas)) return;
        for (int side = 0; side < 6; side++) {
            String textureName = originName + "&" + side + ".png";
            try {
                IResource resource = Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(new ResourceLocation(textureName));
                try (InputStream stream = resource.getInputStream()) {
                    if (!(resource instanceof SimpleResource simple) || simple.getMetadata("myctmlib") == null)
                        continue;
                    JsonObject json = ((AccessorSimpleResource) simple).getMcMetaJson()
                        .getAsJsonObject("myctmlib");
                    CTMConfig config = new CTMConfig(json);
                    TextureAtlasSprite base = registerSprite(atlas, textureName, 2, useInterpolation(simple));
                    TextureAtlasSprite connection = loadSprite(atlas, config.connectionTexture, json, "connection", 4);
                    TextureAtlasSprite alternate = loadSprite(atlas, config.altTexture, json, "alt", 2);
                    if (connection == null && alternate == null) continue;

                    CTMIconManager manager = CTMIconManager.builder()
                        .setIconSmall(base)
                        .setIconCTM(connection)
                        .setIconAlt(alternate)
                        .buildAndInit();
                    ctmIconMap.put(textureName, manager);
                    if (alternate != null) ctmAltMap.put(textureName, alternate.getIconName());
                    if (!config.equivalents.isEmpty()) {
                        ctmReplaceMap.put(textureName, config.equivalents.toArray(new String[0]));
                    }
                }
            } catch (IOException | RuntimeException exception) {
                if (ModConfig.debug)
                    MyCTMLib.LOG.warn("Cannot load IC2 texture metadata for {}", textureName, exception);
            }
        }
    }

    private static TextureAtlasSprite loadSprite(TextureMap atlas, String name, JsonObject json, String field,
        int grid) {
        if (name == null) return null;
        boolean interpolate = false;
        try {
            // IC2 metadata names resource files directly rather than using block-atlas paths.
            IResource resource = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(
                    new ResourceLocation(
                        json.get(field)
                            .getAsString()));
            try (InputStream stream = resource.getInputStream()) {
                if (resource instanceof SimpleResource simple) interpolate = useInterpolation(simple);
            }
        } catch (IOException ignored) {}
        return registerSprite(atlas, name, grid, interpolate);
    }

    private static TextureAtlasSprite registerSprite(TextureMap atlas, String name, int grid, boolean interpolate) {
        TextureAtlasSprite registered = atlas.getTextureExtry(name);
        if (registered != null) return registered;
        TextureAtlasSprite sprite = interpolate ? new InterpolatedIcon(name, grid, grid)
            : new NewTextureAtlasSprite(name, grid, grid);
        atlas.setTextureEntry(name, sprite);
        return sprite;
    }

    private static boolean useInterpolation(SimpleResource resource) {
        if (resource.getMetadata("animation") == null) return false;
        JsonObject animation = ((AccessorSimpleResource) resource).getMcMetaJson()
            .getAsJsonObject("animation");
        JsonElement value = animation == null ? null : animation.get("interpolate");
        return value != null && value.isJsonPrimitive()
            && value.getAsJsonPrimitive()
                .isBoolean()
            && value.getAsBoolean();
    }
}
