package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.github.wohaopa.MyCTMLib.client.model.QuadModel;
import com.google.gson.JsonObject;

public final class RocketModel extends QuadModel {

    public RocketModel(String name, JsonObject json) {
        super(new ResourceLocation("myctmlib", "textures/rockets/" + name + ".png"), "Rocket model " + name, json);
    }

    /**
     * Renders the model turned by {@code yawOffset} degrees about its vertical axis.
     *
     * Every Galacticraft rocket renderer mirrors X and Y before drawing the model,
     * and a mirror conjugates rotations, so the angle is negated here to keep a
     * positive offset a counter clockwise turn on screen.
     */
    public void render(float yawOffset) {
        if (yawOffset == 0.0F) {
            render();
            return;
        }
        GL11.glPushMatrix();
        GL11.glRotatef(-yawOffset, 0.0F, 1.0F, 0.0F);
        render();
        GL11.glPopMatrix();
    }
}
