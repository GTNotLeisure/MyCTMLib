package com.github.wohaopa.MyCTMLib.compat.forestry.machine;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

import com.github.wohaopa.MyCTMLib.MyCTMLib;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.core.blocks.BlockCoreType;
import forestry.core.render.IBlockRenderer;
import forestry.core.render.RenderBlock;
import forestry.core.render.TileRendererIndex;
import forestry.factory.blocks.BlockFactoryTesrType;
import forestry.plugins.PluginCore;
import forestry.plugins.PluginFactory;

@SideOnly(Side.CLIENT)
public class ForestryMachineIntegration {

    public static void registerResources() {
        MinecraftForge.EVENT_BUS.register(ForestryMachineModels.INSTANCE);
    }

    @Optional.Method(modid = "Forestry")
    public static void registerRenderers() {
        int replaced = 0;
        if (PluginFactory.blocks != null) {
            Block block = PluginFactory.blocks.factoryTESR;
            for (BlockFactoryTesrType type : BlockFactoryTesrType.VALUES) {
                String name = type.name()
                    .toLowerCase(Locale.ENGLISH);
                if (!ForestryMachineModels.has(name)) continue;
                replace(block, type.getMeta(), type.getTeClass(), name);
                replaced++;
            }
        }
        if (PluginCore.blocks != null) {
            Block block = PluginCore.blocks.core;
            BlockCoreType type = BlockCoreType.ESCRITOIRE;
            replace(block, type.getMeta(), type.getTeClass(), "escritoire");
            replaced++;
        }
        MyCTMLib.LOG.info("Replaced {} Forestry machine renderers with JSON models", replaced);
    }

    @SuppressWarnings("unchecked")
    private static void replace(Block block, int meta, Class<?> tileClass, String name) {
        Class<? extends TileEntity> key = (Class<? extends TileEntity>) tileClass;
        TileEntitySpecialRenderer originalTile = TileEntityRendererDispatcher.instance.mapSpecialRenderers.get(key);
        TileEntityRendererDispatcher.instance.mapSpecialRenderers
            .put(key, new ForestryMachineRenderer(name, originalTile));
        TileRendererIndex index = new TileRendererIndex(block, meta);
        IBlockRenderer originalBlock = RenderBlock.byBlockRenderer.get(index);
        RenderBlock.byBlockRenderer.put(index, new ForestryBlockRenderer(name, originalBlock));
    }
}
