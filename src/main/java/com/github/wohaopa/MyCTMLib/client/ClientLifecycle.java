package com.github.wohaopa.MyCTMLib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;

import com.github.wohaopa.MyCTMLib.client.model.RedstoneComparatorModel;
import com.github.wohaopa.MyCTMLib.client.model.RedstoneRepeaterModel;
import com.github.wohaopa.MyCTMLib.client.model.RedstoneTorchModel;
import com.github.wohaopa.MyCTMLib.client.resource.BeeJsonModelPackState;
import com.github.wohaopa.MyCTMLib.client.resource.MyCTMLibMetadataSectionSerializer;
import com.github.wohaopa.MyCTMLib.client.resource.MyCTMLibMetadataSectionSerializer.MyCTMLibMetadataSection;
import com.github.wohaopa.MyCTMLib.compat.ae2.Ae2Integration;
import com.github.wohaopa.MyCTMLib.compat.forestry.ForestryIntegration;
import com.github.wohaopa.MyCTMLib.compat.opencomputers.OpenComputersIntegration;
import com.github.wohaopa.MyCTMLib.core.Mods;
import com.github.wohaopa.MyCTMLib.mixins.early.AccessorMinecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientLifecycle {

    public static void preInit() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ((AccessorMinecraft) minecraft).getMetadataSerializer()
            .registerMetadataSectionType(new MyCTMLibMetadataSectionSerializer(), MyCTMLibMetadataSection.class);

        // Loads the replacement redstone torch, repeater and comparator models during every block-atlas stitch.
        MinecraftForge.EVENT_BUS.register(RedstoneTorchModel.INSTANCE);
        MinecraftForge.EVENT_BUS.register(RedstoneRepeaterModel.INSTANCE);
        MinecraftForge.EVENT_BUS.register(RedstoneComparatorModel.INSTANCE);
        if (Mods.AppliedEnergistics2.isModLoaded()) {
            Ae2Integration.registerResources();
        }
        if (minecraft.getResourceManager() instanceof IReloadableResourceManager manager) {
            manager.registerReloadListener(BeeJsonModelPackState.INSTANCE);
            if (Mods.Forestry.isModLoaded()) {
                ForestryIntegration.registerResources(manager);
            }
        }
    }

    public static void loadComplete() {
        if (Mods.AppliedEnergistics2.isModLoaded()) {
            Ae2Integration.registerItemRenderers();
        }
        if (Mods.Forestry.isModLoaded()) {
            ForestryIntegration.registerItemRenderers();
            if (Mods.OpenComputers.isModLoaded()) {
                OpenComputersIntegration.registerItemRenderer();
            }
        }
    }
}
