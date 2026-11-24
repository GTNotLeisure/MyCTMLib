package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

public class RocketModelBase extends ModelBase {

    private final RocketModel model;

    /** Extra turn about the rocket's vertical axis, applied to placed rockets only. */
    private final float yawOffset;

    public RocketModelBase(RocketModel model) {
        this(model, 0.0F);
    }

    public RocketModelBase(RocketModel model, float yawOffset) {
        this.model = model;
        this.yawOffset = yawOffset;
    }

    @Override
    public void render(Entity entity, float x, float y, float z, float yaw, float pitch, float scale) {
        GL11.glPushMatrix();
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        model.render(yawOffset);
        GL11.glPopMatrix();
    }
}
