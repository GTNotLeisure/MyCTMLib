package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public final class RocketModel {

    private final ResourceLocation texture;
    private final int quads;
    private final float[] positions;
    private final float[] uvs;
    private final float[] normals;

    public RocketModel(String name, JsonObject json) {
        texture = new ResourceLocation("myctmlib", "textures/rockets/" + name + ".png");
        JsonArray array = json.getAsJsonArray("quads");
        if (array == null) throw new JsonParseException("Rocket model " + name + " has no quads");
        quads = array.size();
        positions = new float[quads * 12];
        uvs = new float[quads * 8];
        normals = new float[quads * 3];
        for (int index = 0; index < quads; index++) {
            JsonObject quad = array.get(index)
                .getAsJsonObject();
            read(quad.getAsJsonArray("v"), positions, index * 12, 3);
            read(quad.getAsJsonArray("uv"), uvs, index * 8, 2);
            normal(index);
        }
    }

    public void render() {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int index = 0; index < quads; index++) {
            int position = index * 12;
            int uv = index * 8;
            int normal = index * 3;
            tessellator.setNormal(normals[normal], normals[normal + 1], normals[normal + 2]);
            for (int vertex = 0; vertex < 4; vertex++) {
                tessellator.addVertexWithUV(
                    positions[position + vertex * 3],
                    positions[position + vertex * 3 + 1],
                    positions[position + vertex * 3 + 2],
                    uvs[uv + vertex * 2],
                    uvs[uv + vertex * 2 + 1]);
            }
        }
        tessellator.draw();
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

    private static void read(JsonArray array, float[] target, int offset, int stride) {
        if (array == null || array.size() != 4) throw new JsonParseException("Rocket quad needs four corners");
        for (int vertex = 0; vertex < 4; vertex++) {
            JsonArray corner = array.get(vertex)
                .getAsJsonArray();
            for (int axis = 0; axis < stride; axis++) {
                target[offset + vertex * stride + axis] = corner.get(axis)
                    .getAsFloat();
            }
        }
    }

    private void normal(int index) {
        int position = index * 12;
        float ax = positions[position + 3] - positions[position];
        float ay = positions[position + 4] - positions[position + 1];
        float az = positions[position + 5] - positions[position + 2];
        float bx = positions[position + 6] - positions[position + 3];
        float by = positions[position + 7] - positions[position + 4];
        float bz = positions[position + 8] - positions[position + 5];
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0E-6F) {
            length = 1;
        }
        int normal = index * 3;
        normals[normal] = nx / length;
        normals[normal + 1] = ny / length;
        normals[normal + 2] = nz / length;
    }
}
