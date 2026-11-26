package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import net.minecraftforge.client.model.IModelCustom;

public class RocketModelObj implements IModelCustom {

    private static final String PART = "Rocket";

    private final RocketModel model;

    /** Extra turn about the rocket's vertical axis, applied to placed rockets only. */
    private final float yawOffset;

    public RocketModelObj(RocketModel model) {
        this(model, 0.0F);
    }

    public RocketModelObj(RocketModel model, float yawOffset) {
        this.model = model;
        this.yawOffset = yawOffset;
    }

    @Override
    public String getType() {
        return "obj";
    }

    @Override
    public void renderAll() {
        model.render(yawOffset);
    }

    @Override
    public void renderOnly(String... groupNames) {
        if (groupNames == null) return;
        for (String group : groupNames) {
            if (PART.equalsIgnoreCase(group)) {
                model.render(yawOffset);
                return;
            }
        }
    }

    @Override
    public void renderPart(String partName) {
        if (PART.equalsIgnoreCase(partName)) model.render(yawOffset);
    }

    @Override
    public void renderAllExcept(String... excludedGroupNames) {
        if (excludedGroupNames != null) {
            for (String group : excludedGroupNames) {
                if (PART.equalsIgnoreCase(group)) return;
            }
        }
        model.render(yawOffset);
    }
}
