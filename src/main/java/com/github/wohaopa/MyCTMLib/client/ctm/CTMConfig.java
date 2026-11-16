package com.github.wohaopa.MyCTMLib.client.ctm;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class CTMConfig {

    public final String connectionTexture;
    public final String altTexture;
    public final List<String> randomTextures;
    public final List<String> equivalents;
    public final String method;
    public final String symmetry;
    public final int[] weights;
    public final int columns;
    public final int width;
    public final int height;

    public CTMConfig(JsonObject ctmObj) {
        this.connectionTexture = parseStringField(ctmObj, "connection");
        this.altTexture = parseStringField(ctmObj, "alt");
        this.randomTextures = parseStringArray(ctmObj, "random");
        this.equivalents = parseStringArray(ctmObj, "equivalents");
        this.method = parseRawStringField(ctmObj, "method");
        this.symmetry = parseRawStringField(ctmObj, "symmetry");
        this.weights = parseRawIntArray(ctmObj, "weights");
        this.columns = parseRawIntField(ctmObj, "columns", 0);
        this.width = parseRawIntField(ctmObj, "width", 0);
        this.height = parseRawIntField(ctmObj, "height", 0);
    }

    private int parseRawIntField(JsonObject ctmObj, String fieldName, int fallback) {
        return parseInt(ctmObj.get(fieldName), fallback);
    }

    private int parseInt(JsonElement element, int fallback) {
        if (element == null || !element.isJsonPrimitive()) return fallback;
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isNumber() && !primitive.isString()) return fallback;
        try {
            return primitive.getAsBigDecimal()
                .intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return fallback;
        }
    }

    private int[] parseRawIntArray(JsonObject ctmObj, String fieldName) {
        if (!ctmObj.has(fieldName)) {
            return new int[0];
        }

        JsonElement element = ctmObj.get(fieldName);
        if (!element.isJsonArray()) {
            return new int[0];
        }

        JsonArray array = element.getAsJsonArray();
        int[] result = new int[array.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = parseInt(array.get(i), 1);
        }
        return result;
    }

    private String parseRawStringField(JsonObject ctmObj, String fieldName) {
        JsonElement element = ctmObj.get(fieldName);
        if (element == null || !element.isJsonPrimitive()
            || !element.getAsJsonPrimitive()
                .isString())
            return null;
        String value = element.getAsString();
        return value.isEmpty() ? null : value;
    }

    private String parseStringField(JsonObject ctmObj, String fieldName) {
        String value = parseRawStringField(ctmObj, fieldName);
        return value == null ? null : processTextureName(value);
    }

    private List<String> parseStringArray(JsonObject ctmObj, String fieldName) {
        if (!ctmObj.has(fieldName)) {
            return new ArrayList<>();
        }

        JsonElement element = ctmObj.get(fieldName);
        if (!element.isJsonArray()) {
            return new ArrayList<>();
        }

        JsonArray array = element.getAsJsonArray();
        List<String> result = new ArrayList<>(array.size());

        for (JsonElement el : array) {
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive()
                .isString()) continue;
            String originalValue = el.getAsString();
            if (originalValue.isEmpty()) continue;
            String processedValue = processTextureName(originalValue);
            result.add(processedValue);
        }

        return result;
    }

    private static String processTextureName(String texture) {
        if (texture.startsWith("minecraft:")) texture = texture.substring("minecraft:".length());
        int pathStart = texture.indexOf(':') + 1;
        if (texture.startsWith("textures/blocks/", pathStart)) {
            texture = texture.substring(0, pathStart) + texture.substring(pathStart + "textures/blocks/".length());
        }
        return texture.endsWith(".png") ? texture.substring(0, texture.length() - 4) : texture;
    }
}
