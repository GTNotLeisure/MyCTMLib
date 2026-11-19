package com.github.wohaopa.MyCTMLib.compat.projectred;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.BakedBlock;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.github.wohaopa.MyCTMLib.client.model.ModelOrientation;
import com.github.wohaopa.MyCTMLib.client.resource.JsonModelResources;
import com.github.wohaopa.MyCTMLib.client.resource.MyCTMLibMetadataSectionSerializer.MyCTMLibMetadataSection;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorMinecraft;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorTextureMap;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.ModelLoc;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class ProjectRedTorchFeature {

    public static final ProjectRedTorchFeature INSTANCE = new ProjectRedTorchFeature();
    private static final String SETTING = "enableProjectRedJsonModels";
    private static final String TEXTURE_ON = "myctmlib:blocks/minecraft/redstone_torch";
    private static final String TEXTURE_OFF = "myctmlib:blocks/minecraft/redstone_torch_off";
    private static final String MODEL = "myctmlib:block/minecraft/redstone_torch_on";
    private static final int UNROTATED = ModelOrientation.index(ForgeDirection.NORTH, ForgeDirection.UP);

    private volatile boolean enabled;
    private volatile IIcon onIcon;

    private ProjectRedTorchFeature() {}

    public static IIcon getOnIcon() {
        ProjectRedTorchFeature feature = INSTANCE;
        return feature.enabled ? feature.onIcon : null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return;
        enabled = false;
        onIcon = null;
        ProjectRedTorchModelRenderer.reset();
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        if (!isProjectRedLoaded() || !isSettingEnabled()) return;
        enabled = true;
        event.map.registerIcon(TEXTURE_ON);
        event.map.registerIcon(TEXTURE_OFF);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterStitch(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0 || !enabled) return;
        if (((AccessorTextureMap) event.map).myctmlib$isSkippingFirstLoad()) return;
        IIcon on = event.map.getAtlasSprite(TEXTURE_ON);
        IIcon off = event.map.getAtlasSprite(TEXTURE_OFF);
        if ("missingno".equals(on.getIconName()) || "missingno".equals(off.getIconName())) {
            MyCTMLib.LOG.error("Missing ProjectRed torch replacement texture");
            enabled = false;
            return;
        }
        onIcon = on;
        applyIcons("mrtjp.projectred.integration.ComponentStore$", "redstoneTorchIcons_$eq", new IIcon[] { off, on });
        applyIcon("mrtjp.projectred.fabrication.ICComponentStore$", "torchOffIcon_$eq", off);
        applyIcon("mrtjp.projectred.fabrication.ICComponentStore$", "torchOnIcon_$eq", on);
        loadLitTorchModel();
    }

    private static void loadLitTorchModel() {
        if (!Loader.isModLoaded("ProjRed|Integration")) return;
        try {
            JsonModelResources resources = new JsonModelResources(
                Minecraft.getMinecraft()
                    .getResourceManager());
            BakedBlock baked = new JsonBlockModel(resources.load(ModelLoc.fromStr(MODEL))).bakeBlock(UNROTATED);
            Geometry geometry = baked.orientations()[UNROTATED];
            if (geometry == null) throw new IllegalStateException("Missing redstone torch orientation");
            ProjectRedTorchModelRenderer.initialize(geometry);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            ProjectRedTorchModelRenderer.reset();
            MyCTMLib.LOG.error("Cannot load the ProjectRed lit torch JSON model", exception);
        }
    }

    private static boolean isProjectRedLoaded() {
        return Loader.isModLoaded("ProjRed|Integration") || Loader.isModLoaded("ProjRed|Fabrication")
            || Loader.isModLoaded("ProjRed|Illumination");
    }

    private static boolean isSettingEnabled() {
        ResourcePackRepository repository = Minecraft.getMinecraft()
            .getResourcePackRepository();
        List<IResourcePack> packs = new ArrayList<>();
        packs.add(repository.func_148530_e());
        List<ResourcePackRepository.Entry> entries = repository.getRepositoryEntries();
        for (int index = entries.size() - 1; index >= 0; index--) {
            packs.add(
                entries.get(index)
                    .getResourcePack());
        }
        for (IResourcePack pack : packs) {
            if (pack == null) continue;
            try {
                MyCTMLibMetadataSection metadata = (MyCTMLibMetadataSection) pack.getPackMetadata(
                    ((AccessorMinecraft) Minecraft.getMinecraft()).getMetadataSerializer(),
                    "myctmlib");
                if (metadata == null) continue;
                Boolean value = metadata.getBooleanSetting(SETTING);
                if (value != null) return value;
            } catch (IOException | RuntimeException exception) {
                MyCTMLib.LOG.warn("Cannot read {} from resource pack {}", SETTING, pack.getPackName(), exception);
            }
        }
        return false;
    }

    private static void applyIcons(String className, String setterName, IIcon[] icons) {
        try {
            Class<?> type = Class.forName(className);
            Object module = type.getField("MODULE$")
                .get(null);
            Method setter = type.getMethod(setterName, IIcon[].class);
            setter.invoke(module, (Object) icons);
        } catch (ClassNotFoundException | LinkageError ignored) {} catch (ReflectiveOperationException exception) {
            MyCTMLib.LOG.error("Cannot replace ProjectRed torch icons in {}", className, exception);
        }
    }

    private static void applyIcon(String className, String setterName, IIcon icon) {
        try {
            Class<?> type = Class.forName(className);
            Object module = type.getField("MODULE$")
                .get(null);
            Method setter = type.getMethod(setterName, IIcon.class);
            setter.invoke(module, icon);
        } catch (ClassNotFoundException | LinkageError ignored) {} catch (ReflectiveOperationException exception) {
            MyCTMLib.LOG.error("Cannot replace ProjectRed torch icon in {}", className, exception);
        }
    }
}
