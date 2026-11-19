package com.github.wohaopa.MyCTMLib.compat.projectred;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.IIcon;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.client.model.JsonBlockModel.Geometry;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

public final class ProjectRedTorchModelRenderer {

    private static final String TORCH_INTERFACE = "mrtjp.projectred.integration.TRedstoneTorchModel";
    private static final Map<TorchKey, Object> MODELS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> ON_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> LIGHT_METHODS = new ConcurrentHashMap<>();

    private static volatile Geometry geometry;
    private static Method quadModelMethod;
    private static Method computeNormalsMethod;
    private static Method renderMethod;
    private static Field vertsField;
    private static Constructor<?> vertexConstructor;

    private ProjectRedTorchModelRenderer() {}

    public static void initialize(Geometry litGeometry) throws ReflectiveOperationException {
        geometry = litGeometry;
        MODELS.clear();
        Class<?> modelClass = Class.forName("codechicken.lib.render.CCModel");
        Class<?> vertexClass = Class.forName("codechicken.lib.render.Vertex5");
        Class<?> operationClass = Class.forName("codechicken.lib.render.CCRenderState$IVertexOperation");
        Class<?> operationArrayClass = java.lang.reflect.Array.newInstance(operationClass, 0)
            .getClass();
        quadModelMethod = modelClass.getMethod("quadModel", int.class);
        computeNormalsMethod = modelClass.getMethod("computeNormals");
        renderMethod = modelClass.getMethod("render", operationArrayClass);
        vertsField = modelClass.getField("verts");
        vertexConstructor = vertexClass
            .getConstructor(double.class, double.class, double.class, double.class, double.class);
    }

    public static void reset() {
        geometry = null;
        MODELS.clear();
        ON_METHODS.clear();
        LIGHT_METHODS.clear();
        quadModelMethod = null;
        computeNormalsMethod = null;
        renderMethod = null;
        vertsField = null;
        vertexConstructor = null;
    }

    public static boolean render(Object subject, Object operations) {
        if (geometry == null || quadModelMethod == null || !isLitTorch(subject)) return false;
        Object model = modelFor(subject);
        if (model == null) return false;
        try {
            renderMethod.invoke(model, operations);
            return true;
        } catch (ReflectiveOperationException exception) {
            MyCTMLib.LOG.error("Cannot render the ProjectRed lit torch JSON model", exception);
            return false;
        }
    }

    private static boolean isLitTorch(Object subject) {
        boolean torch = false;
        for (Class<?> type = subject.getClass(); type != null && !torch; type = type.getSuperclass()) {
            for (Class<?> iface : type.getInterfaces()) {
                if (TORCH_INTERFACE.equals(iface.getName())) {
                    torch = true;
                    break;
                }
            }
        }
        if (!torch) return false;
        try {
            Method on = ON_METHODS.computeIfAbsent(subject.getClass(), type -> {
                try {
                    return type.getMethod("on");
                } catch (NoSuchMethodException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            return Boolean.TRUE.equals(on.invoke(subject));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static Object modelFor(Object subject) {
        try {
            Method getLightPos = LIGHT_METHODS.computeIfAbsent(subject.getClass(), type -> {
                try {
                    return type.getMethod("getLightPos");
                } catch (NoSuchMethodException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            Object light = getLightPos.invoke(subject);
            Class<?> lightClass = light.getClass();
            double x = lightClass.getField("x")
                .getDouble(light);
            double y = lightClass.getField("y")
                .getDouble(light);
            double z = lightClass.getField("z")
                .getDouble(light);
            boolean flipped = subject.getClass()
                .getName()
                .contains("FlippedRSTorchModel");
            int heightPixels = flipped ? 5 : (int) Math.round(y * 16.0D) + 2;
            TorchKey key = new TorchKey(x, z, heightPixels, flipped);
            Object cached = MODELS.get(key);
            if (cached != null) return cached;
            Object model = buildModel(x, z, heightPixels, flipped);
            if (model == null) return null;
            MODELS.put(key, model);
            return model;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            MyCTMLib.LOG.error("Cannot build the ProjectRed lit torch JSON model", exception);
            return null;
        }
    }

    private static Object buildModel(double lightX, double lightZ, int heightPixels, boolean flipped)
        throws ReflectiveOperationException {
        ModelQuadView[] quads = geometry.quads();
        if (quads.length == 0) return null;
        double dx = lightX - 0.5D;
        double dz = lightZ - 0.5D;
        double cutY = Math.max(0, 11 - heightPixels) / 16.0D;
        double minY = flipped ? cutY : 0.0D;
        List<Vertex> vertices = new ArrayList<>();
        for (ModelQuadView quad : quads) {
            IIcon sprite = (IIcon) quad.celeritas$getSprite();
            if (sprite == null || sprite.getMaxU() == sprite.getMinU() || sprite.getMaxV() == sprite.getMinV()) {
                return null;
            }
            Vertex[] input = new Vertex[4];
            for (int vertex = 0; vertex < 4; vertex++) {
                double x = quad.getX(vertex) + dx;
                double y = quad.getY(vertex);
                double z = quad.getZ(vertex) + dz;
                if (flipped) {
                    x = 1.0D - x;
                    y = 1.0D - y - 6.0D / 16.0D;
                } else {
                    y -= cutY;
                }
                double u = (quad.getTexU(vertex) - sprite.getMinU()) / (sprite.getMaxU() - sprite.getMinU());
                double v = (quad.getTexV(vertex) - sprite.getMinV()) / (sprite.getMaxV() - sprite.getMinV());
                input[vertex] = new Vertex(x, y, z, u, v);
            }
            appendPolygon(vertices, clip(input, minY));
        }
        if (vertices.isEmpty() || vertices.size() % 4 != 0) return null;
        Object model = quadModelMethod.invoke(null, vertices.size());
        Object[] verts = (Object[]) vertsField.get(model);
        for (int index = 0; index < vertices.size(); index++) {
            Vertex vertex = vertices.get(index);
            verts[index] = vertexConstructor.newInstance(vertex.x, vertex.y, vertex.z, vertex.u, vertex.v);
        }
        computeNormalsMethod.invoke(model);
        return model;
    }

    private static List<Vertex> clip(Vertex[] input, double minY) {
        List<Vertex> output = new ArrayList<>(5);
        for (int index = 0; index < input.length; index++) {
            Vertex current = input[index];
            Vertex next = input[(index + 1) % input.length];
            boolean currentInside = current.y >= minY;
            boolean nextInside = next.y >= minY;
            if (currentInside) output.add(current);
            if (currentInside != nextInside) {
                double t = (minY - current.y) / (next.y - current.y);
                output.add(
                    new Vertex(
                        current.x + (next.x - current.x) * t,
                        minY,
                        current.z + (next.z - current.z) * t,
                        current.u + (next.u - current.u) * t,
                        current.v + (next.v - current.v) * t));
            }
        }
        return output;
    }

    private static void appendPolygon(List<Vertex> output, List<Vertex> polygon) {
        if (polygon.size() == 3) {
            output.add(polygon.get(0));
            output.add(polygon.get(1));
            output.add(polygon.get(2));
            output.add(polygon.get(2));
        } else if (polygon.size() == 4) {
            output.addAll(polygon);
        } else if (polygon.size() == 5) {
            output.add(polygon.get(0));
            output.add(polygon.get(1));
            output.add(polygon.get(2));
            output.add(polygon.get(3));
            output.add(polygon.get(0));
            output.add(polygon.get(3));
            output.add(polygon.get(4));
            output.add(polygon.get(4));
        }
    }

    private record Vertex(double x, double y, double z, double u, double v) {}

    private record TorchKey(double x, double z, int heightPixels, boolean flipped) {}
}
