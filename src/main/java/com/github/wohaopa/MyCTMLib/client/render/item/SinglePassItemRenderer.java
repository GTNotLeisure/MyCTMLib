package com.github.wohaopa.MyCTMLib.client.render.item;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public interface SinglePassItemRenderer extends IItemRenderer {

    boolean rendersAllPasses(ItemStack stack, ItemRenderType type);
}
