package com.github.wohaopa.MyCTMLib.mixins.early;

import static com.github.wohaopa.MyCTMLib.client.ctm.Textures.ctmAltMap;
import static com.github.wohaopa.MyCTMLib.client.ctm.Textures.ctmIconMap;
import static com.github.wohaopa.MyCTMLib.client.ctm.Textures.ctmRandomMap;
import static com.github.wohaopa.MyCTMLib.client.ctm.Textures.ctmReplaceMap;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.ITickableTextureObject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.ctm.CTMConfig;
import com.github.wohaopa.MyCTMLib.client.ctm.CTMIconManager;
import com.github.wohaopa.MyCTMLib.client.ctm.CtmMethod;
import com.github.wohaopa.MyCTMLib.client.ctm.Textures;
import com.github.wohaopa.MyCTMLib.client.resource.MyCTMLibMetadataSectionSerializer.MyCTMLibMetadataSection;
import com.github.wohaopa.MyCTMLib.client.texture.CtmSheetSprite;
import com.github.wohaopa.MyCTMLib.client.texture.InterpolatedIcon;
import com.github.wohaopa.MyCTMLib.client.texture.NewTextureAtlasSprite;
import com.github.wohaopa.MyCTMLib.config.ModConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Mixin(value = TextureMap.class, remap = true)
public abstract class MixinTextureMap extends AbstractTexture implements ITickableTextureObject, IIconRegister {

    @Shadow(remap = true)
    @Final
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @Shadow(remap = true)
    protected abstract ResourceLocation completeResourceLocation(ResourceLocation location, int type);

    @Shadow(remap = true)
    @Final
    private String basePath;

    @Inject(method = "loadTextureAtlas", at = @At("HEAD"))
    private void clearMyCtmRegistrations(IResourceManager resourceManager, CallbackInfo ci) {
        if (!isItemAtlas()) {
            Textures.clearTextureRegistrations();
        }
    }

    @Inject(
        method = "registerIcon",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;<init>(Ljava/lang/String;)V"),
        cancellable = true)
    private void onRegisterIcon(String textureName, CallbackInfoReturnable<IIcon> cir) {
        if (isItemAtlas()) return;
        try {
            IResource resource = getResourceFromTextureName(textureName);
            try (InputStream baseStream = resource.getInputStream()) {
                TextureAtlasSprite currentBase;
                TextureAtlasSprite currentCTM;
                TextureAtlasSprite currentAlt = null;
                List<CTMIconManager> randomManagers = new ArrayList<>();

                if (!(resource instanceof SimpleResource simple)) {
                    return;
                }

                MyCTMLibMetadataSection section = (MyCTMLibMetadataSection) resource.getMetadata("myctmlib");
                if (section == null) {
                    return;
                }

                JsonObject ctmObj = section.getJson();

                if (ctmObj == null) {
                    return;
                }

                CTMIconManager.Builder builder = CTMIconManager.builder();
                CTMConfig config = new CTMConfig(ctmObj);

                CtmMethod method = CtmMethod.fromName(config.method);
                if (method != null && !method.isImplemented()) return;
                if (method == CtmMethod.COMPACT) method = null;

                currentBase = useInterpolation(simple) ? new InterpolatedIcon(textureName, 2, 2)
                    : new NewTextureAtlasSprite(textureName, 2, 2);
                builder.setIconSmall(currentBase);
                mapRegisteredSprites.put(textureName, currentBase);

                if (config.connectionTexture != null && method == CtmMethod.FIXED) {

                    try {
                        IResource resourceTile = getResourceFromTextureName(config.connectionTexture);

                        try (InputStream stream = resourceTile.getInputStream()) {
                            if (resourceTile instanceof SimpleResource simpleTile) {
                                TextureAtlasSprite faceTile = useInterpolation(simpleTile)
                                    ? new InterpolatedIcon(config.connectionTexture, 1, 1)
                                    : new NewTextureAtlasSprite(config.connectionTexture, 1, 1);
                                faceTile = registerSprite(faceTile);
                                builder.setFaceTile(faceTile);
                            }
                        }
                    } catch (IOException ignored) {}

                } else if (config.connectionTexture != null && method == CtmMethod.REPEAT
                    && config.width > 0
                    && config.height > 0) {

                        try {
                            IResource resourceRepeat = getResourceFromTextureName(config.connectionTexture);

                            try (InputStream stream = resourceRepeat.getInputStream()) {
                                if (resourceRepeat instanceof SimpleResource simpleRepeat) {
                                    BufferedImage repeatImage = ImageIO.read(stream);
                                    if (repeatImage != null && repeatImage.getWidth() % config.width == 0
                                        && repeatImage.getHeight() % config.height == 0
                                        && repeatImage.getWidth() / config.width
                                            == repeatImage.getHeight() / config.height) {
                                        CtmSheetSprite repeatSprite = registerSheet(
                                            textureName,
                                            config.connectionTexture,
                                            repeatImage,
                                            config.width,
                                            config.height,
                                            useInterpolation(simpleRepeat));
                                        builder.setFaceTiles(CtmMethod.REPEAT, repeatSprite.getUnitIcons());
                                        builder.setRepeatOptions(config.width, config.height, config.symmetry);
                                    }
                                }
                            }
                        } catch (IOException ignored) {}

                    } else if (config.connectionTexture != null && method == CtmMethod.RANDOM) {

                        try {
                            IResource resourceRandom = getResourceFromTextureName(config.connectionTexture);

                            try (InputStream stream = resourceRandom.getInputStream()) {
                                if (resourceRandom instanceof SimpleResource simpleRandom) {
                                    BufferedImage randomImage = ImageIO.read(stream);
                                    if (randomImage != null && randomImage.getHeight() > 0) {
                                        int columns = config.columns > 0 ? config.columns
                                            : randomImage.getWidth() / randomImage.getHeight();
                                        int unitSize = columns > 0 ? randomImage.getWidth() / columns : 0;
                                        int rows = unitSize > 0 ? randomImage.getHeight() / unitSize : 0;
                                        if (columns > 0 && unitSize > 0
                                            && rows > 0
                                            && columns * unitSize == randomImage.getWidth()
                                            && rows * unitSize == randomImage.getHeight()) {
                                            CtmSheetSprite randomSprite = registerSheet(
                                                textureName,
                                                config.connectionTexture,
                                                randomImage,
                                                columns,
                                                rows,
                                                useInterpolation(simpleRandom));
                                            builder.setFaceTiles(CtmMethod.RANDOM, randomSprite.getUnitIcons());
                                            builder.setRandomOptions(config.symmetry, config.weights);
                                        }
                                    }
                                }
                            }
                        } catch (IOException ignored) {}

                    } else if (config.connectionTexture != null && method != null) {

                        try {
                            IResource resourceLayout = getResourceFromTextureName(config.connectionTexture);

                            try (InputStream stream = resourceLayout.getInputStream()) {
                                if (resourceLayout instanceof SimpleResource simpleLayout) {
                                    BufferedImage layoutImage = ImageIO.read(stream);
                                    // A sheet that cannot hold one pixel per cell would slice to nothing, so it is
                                    // ignored.
                                    if (layoutImage != null && layoutImage.getWidth() >= method.getColumns()
                                        && layoutImage.getHeight() >= method.getRows()) {
                                        CtmSheetSprite layoutSprite = registerSheet(
                                            textureName,
                                            config.connectionTexture,
                                            layoutImage,
                                            method.getColumns(),
                                            method.getRows(),
                                            useInterpolation(simpleLayout));
                                        IIcon[] units = layoutSprite.getUnitIcons();
                                        IIcon[] tiles = new IIcon[units.length];
                                        for (int tile = 0; tile < tiles.length; tile++) {
                                            tiles[tile] = units[method.getCellIndex(tile)];
                                        }
                                        builder.setFaceTiles(method, tiles);
                                    }
                                }
                            }
                        } catch (IOException ignored) {}

                    } else if (config.connectionTexture != null) {

                        try {
                            IResource resourceCTM = getResourceFromTextureName(config.connectionTexture);

                            try (InputStream stream = resourceCTM.getInputStream()) {
                                if (resourceCTM instanceof SimpleResource simpleCTM) {
                                    BufferedImage sheet = ImageIO.read(stream);
                                    if (sheet != null && sheet.getWidth() == sheet.getHeight()
                                        && sheet.getWidth() >= 4
                                        && (sheet.getWidth() & 1) == 0) {
                                        CtmSheetSprite sheetSprite = registerSheet(
                                            textureName,
                                            config.connectionTexture,
                                            sheet,
                                            2,
                                            2,
                                            useInterpolation(simpleCTM));
                                        builder.setIconVariants(sheetSprite.getUnitIcons());
                                    } else {
                                        currentCTM = useInterpolation(simpleCTM)
                                            ? new InterpolatedIcon(config.connectionTexture, 4, 4)
                                            : new NewTextureAtlasSprite(config.connectionTexture, 4, 4);
                                        currentCTM = registerSprite(currentCTM);
                                        builder.setIconCTM(currentCTM);
                                    }
                                }

                            }
                        } catch (IOException ignored) {}
                    }

                if (!config.randomTextures.isEmpty()) {

                    List<String> processedTextures = config.randomTextures;

                    // Pair random connection sheets with their fallback sheets.
                    if (config.connectionTexture != null) {
                        for (String processedTexture : processedTextures) {
                            if (!processedTexture.contains("_ctm")) {
                                continue;
                            }

                            String baseTextureName = processedTexture.replace("_ctm", "");

                            if (!processedTextures.contains(baseTextureName)) {
                                continue;
                            }

                            TextureAtlasSprite baseSprite = new NewTextureAtlasSprite(baseTextureName, 2, 2);
                            TextureAtlasSprite randomSprite = new NewTextureAtlasSprite(processedTexture, 4, 4);
                            baseSprite = registerSprite(baseSprite);
                            randomSprite = registerSprite(randomSprite);

                            randomManagers.add(
                                CTMIconManager.builder()
                                    .setIconSmall(baseSprite)
                                    .setIconCTM(randomSprite)
                                    .buildAndInit());
                        }

                    }

                    // Build fallback-only random variants when no connection sheet exists.
                    if (config.connectionTexture == null) {

                        for (String processedTexture : processedTextures) {
                            TextureAtlasSprite randomSprite = new NewTextureAtlasSprite(processedTexture, 2, 2);
                            randomSprite = registerSprite(randomSprite);

                            randomManagers.add(
                                CTMIconManager.builder()
                                    .setIconSmall(randomSprite)
                                    .buildAndInit());
                        }
                    }

                }

                if (config.altTexture != null) {
                    try {
                        IResource resourceAlt = getResourceFromTextureName(config.altTexture);

                        try (InputStream stream = resourceAlt.getInputStream()) {
                            if (resourceAlt instanceof SimpleResource simpleAlt) {
                                currentAlt = useInterpolation(simpleAlt) ? new InterpolatedIcon(config.altTexture, 2, 2)
                                    : new NewTextureAtlasSprite(config.altTexture, 2, 2);

                                currentAlt = registerSprite(currentAlt);
                                builder.setIconAlt(currentAlt);
                            }

                        }
                    } catch (IOException ignored) {}

                }

                CTMIconManager ctmManager = builder.buildAndInit();
                if (!ctmManager.hasConnectionTexture()) {
                    cir.setReturnValue(currentBase);
                    return;
                }

                if (!randomManagers.isEmpty()) {
                    ctmRandomMap.put(textureName, randomManagers);
                }

                if (currentAlt != null) {
                    ctmAltMap.put(textureName, currentAlt.getIconName());
                }

                if (!config.equivalents.isEmpty()) {
                    ctmReplaceMap.put(textureName, config.equivalents.toArray(new String[0]));
                }

                ctmIconMap.put(textureName, ctmManager);

                cir.setReturnValue(currentBase);
            }
        } catch (FileNotFoundException e) {
            // The base texture is not shipped by any resource pack, which is common for icon names that only exist as
            // metadata, so there is nothing to report.
        } catch (Exception e) {
            // A metadata section that cannot be read leaves the texture untouched, which is reported only in debug mode
            // because a broken resource pack would otherwise flood the log.
            if (ModConfig.debug) {
                MyCTMLib.LOG.warn("[MyCTMLib] Ignoring resource metadata for texture {}.", textureName, e);
            }
        }
    }

    @Unique
    private CtmSheetSprite registerSheet(String textureName, String connectionTexture, BufferedImage image, int columns,
        int rows, boolean interpolate) {
        String key = connectionSheetKey(connectionTexture, textureName, image, columns, rows, interpolate);
        TextureAtlasSprite registered = mapRegisteredSprites.get(key);
        if (registered instanceof CtmSheetSprite sprite) return sprite;
        CtmSheetSprite sprite = new CtmSheetSprite(
            key,
            completeResourceLocation(new ResourceLocation(connectionTexture), 0),
            image,
            columns,
            rows,
            CtmSheetSprite.DEFAULT_PADDING,
            interpolate);
        mapRegisteredSprites.put(key, sprite);
        return sprite;
    }

    @Unique
    private TextureAtlasSprite registerSprite(TextureAtlasSprite sprite) {
        TextureAtlasSprite registered = mapRegisteredSprites.putIfAbsent(sprite.getIconName(), sprite);
        return registered == null ? sprite : registered;
    }

    @Unique
    private boolean isCompatibleSheet(Object candidate, BufferedImage sheet, int columns, int rows,
        boolean interpolate) {
        return candidate instanceof CtmSheetSprite sprite
            && sprite.isSameSource(sheet, columns, rows, CtmSheetSprite.DEFAULT_PADDING, interpolate);
    }

    /** Reuses compatible sheets and gives incompatible registrations a separate atlas name. */
    @Unique
    private String connectionSheetKey(String connectionTexture, String textureName, BufferedImage sheet, int columns,
        int rows, boolean interpolate) {
        Object owner = this.mapRegisteredSprites.get(connectionTexture);
        if (owner == null || isCompatibleSheet(owner, sheet, columns, rows, interpolate)) {
            return connectionTexture;
        }

        // concat and String.valueOf keep this class free of string concatenation bytecode, which the current JDK
        // would otherwise compile to an invokedynamic call and ship as an extra versions:17 class entry.
        String base = connectionTexture.concat("_")
            .concat(
                textureName.replace(':', '_')
                    .replace('/', '_'));
        String candidate = base;
        for (int suffix = 2;; suffix++) {
            Object conflict = this.mapRegisteredSprites.get(candidate);
            if (conflict == null || isCompatibleSheet(conflict, sheet, columns, rows, interpolate)) {
                return candidate;
            }
            candidate = base.concat("_")
                .concat(String.valueOf(suffix));
        }
    }

    @Unique
    private boolean useInterpolation(SimpleResource simple) {
        if (simple.getMetadata("animation") == null) {
            return false;
        }

        JsonObject animationObj = ((AccessorSimpleResource) simple).getMcMetaJson()
            .getAsJsonObject("animation");
        JsonElement value = animationObj == null ? null : animationObj.get("interpolate");
        return value != null && value.isJsonPrimitive()
            && value.getAsJsonPrimitive()
                .isBoolean()
            && value.getAsBoolean();
    }

    @Unique
    private boolean isItemAtlas() {
        return basePath.contains("textures\\items") || basePath.contains("textures/items");
    }

    @Unique
    private IResource getResourceFromTextureName(String textureName) throws IOException {
        ResourceLocation res = completeResourceLocation(new ResourceLocation(textureName), 0);
        return Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(res);
    }
}
