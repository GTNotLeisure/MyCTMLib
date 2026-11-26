package com.github.wohaopa.MyCTMLib.compat.galacticraft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import com.github.wohaopa.MyCTMLib.MyCTMLib;
import com.github.wohaopa.MyCTMLib.core.Mods;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GalacticraftIntegration {

    private record Target(String name, String tier, String field) {}

    private static final List<Target> ENTITIES = List.of(
        new Target("micdoodle8.mods.galacticraft.core.entities.EntityTier1Rocket", "rocket_tier_1", "modelSpaceship"),
        new Target(
            "micdoodle8.mods.galacticraft.planets.mars.entities.EntityTier2Rocket",
            "rocket_tier_2",
            "modelSpaceship"),
        new Target(
            "micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityTier3Rocket",
            "rocket_tier_3",
            "rocketModelObj"),
        new Target("galaxyspace.core.entity.rocket.EntityTier4Rocket", "rocket_tier_4", "model"));

    private static final List<Target> ITEMS = List.of(
        new Target("micdoodle8.mods.galacticraft.core.items.ItemTier1Rocket", "rocket_tier_1", "modelSpaceship"),
        new Target(
            "micdoodle8.mods.galacticraft.planets.mars.items.ItemTier2Rocket",
            "rocket_tier_2",
            "modelSpaceship"),
        new Target(
            "micdoodle8.mods.galacticraft.planets.asteroids.items.ItemTier3Rocket",
            "rocket_tier_3",
            "modelSpaceship"));

    private static final Target TIER4_ITEM = new Target("galaxyspace tier4", "rocket_tier_4", "model");

    private static final Map<Object, Slot> installed = new IdentityHashMap<>();

    private static final Map<String, Float> ENTITY_YAW = Map.of("rocket_tier_3", 45.0F, "rocket_tier_4", 45.0F);

    private GalacticraftIntegration() {}

    public static synchronized void reset() {
        installed.clear();
    }

    public static synchronized void apply() {
        if (RenderManager.instance == null || Item.itemRegistry == null) return;
        List<Slot> found = new ArrayList<>();
        entityRenderers(found);
        itemRenderers(found);
        install(found);
    }

    public static synchronized void applyEntities() {
        if (RenderManager.instance == null) return;
        List<Slot> found = new ArrayList<>();
        entityRenderers(found);
        install(found);
    }

    private static void install(List<Slot> found) {
        if (found.isEmpty()) return;
        for (Slot slot : found) {
            Slot known = installed.get(slot.owner);
            if (known == null) {
                installed.put(slot.owner, slot);
                known = slot;
            }
            known.install();
        }
    }

    private static void entityRenderers(List<Slot> found) {
        for (Map.Entry<?, Render> entry : RenderManager.instance.entityRenderMap.entrySet()) {
            if (!(entry.getKey() instanceof Class<?>type)) continue;
            String tier = tierOf(type);
            if (tier != null) appendEntity(found, entry.getValue(), tier);
        }
    }

    private static String tierOf(Class<?> type) {
        String name = type.getName();
        for (Target target : ENTITIES) {
            if (target.name()
                .equals(name)) return target.tier();
        }
        String simple = type.getSimpleName();
        if (simple.endsWith("Tier4Rocket")) return "rocket_tier_4";
        if (simple.endsWith("Tier3Rocket")) return "rocket_tier_3";
        if (simple.endsWith("Tier2Rocket")) return "rocket_tier_2";
        if (simple.endsWith("Tier1Rocket")) return "rocket_tier_1";
        return null;
    }

    private static void appendEntity(List<Slot> found, Object renderer, String tier) {
        if (renderer == null) return;
        List<String> names;
        String simple = renderer.getClass()
            .getSimpleName();
        if (simple.endsWith("RenderTier3Rocket")) {
            names = List.of("rocketModelObj", "model");
        } else if (simple.endsWith("RenderTier1Rocket")) {
            names = List.of("modelSpaceship", "modelSpaceshipObj");
        } else {
            names = List.of("model", "rocketModelObj", "modelSpaceship");
        }
        for (String name : names) {
            Field field = field(renderer.getClass(), name);
            if (field == null) continue;
            try {
                Object value = field.get(renderer);
                if (value == null) continue;
                found.add(new Slot(renderer, field, tier, value, true));
                return;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                MyCTMLib.LOG.warn("Cannot read {} of {}", name, renderer.getClass(), exception);
                return;
            }
        }
        MyCTMLib.LOG.warn("{} has no rocket model field; its model stays unchanged", renderer.getClass());
    }

    private static void itemRenderers(List<Slot> found) {
        for (Object value : Item.itemRegistry) {
            if (!(value instanceof Item item)) continue;
            Target target = find(
                ITEMS,
                item.getClass()
                    .getName());
            if (target != null) append(found, renderer(item), target);
        }
        if (!Mods.GalaxySpace.isModLoaded()) return;
        Item tier4 = tier4Item();
        if (tier4 != null) append(found, renderer(tier4), TIER4_ITEM);
    }

    private static IItemRenderer renderer(Item item) {
        return MinecraftForgeClient.getItemRenderer(new ItemStack(item), ItemRenderType.INVENTORY);
    }

    private static Item tier4Item() {
        try {
            Object item = Class.forName("galaxyspace.core.register.GSItems")
                .getField("Tier4Rocket")
                .get(null);
            return item instanceof Item ? (Item) item : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static void append(List<Slot> found, Object owner, Target target) {
        if (owner == null) return;
        Field field = field(owner.getClass(), target.field());
        if (field == null) {
            MyCTMLib.LOG.warn("{} has no field {}; its rocket model stays unchanged", owner.getClass(), target.field());
            return;
        }
        try {
            found.add(new Slot(owner, field, target.tier(), field.get(owner), false));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            MyCTMLib.LOG.warn("Cannot read {} of {}", target.field(), owner.getClass(), exception);
        }
    }

    private static Field field(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static Target find(List<Target> targets, String name) {
        for (Target target : targets) {
            if (target.name()
                .equals(name)) return target;
        }
        return null;
    }

    private static Object wrap(Class<?> type, RocketModel model, String tier, boolean entity) {
        float yaw = entity ? ENTITY_YAW.getOrDefault(tier, 0.0F) : 0.0F;
        if (type.isAssignableFrom(RocketModelBase.class)) return new RocketModelBase(model, yaw);
        if (type.isAssignableFrom(RocketModelObj.class)) return new RocketModelObj(model, yaw);
        return null;
    }

    private static final class Slot {

        private final Object owner;
        private final Field field;
        private final String tier;
        private final Object original;
        private final boolean entity;
        private boolean failed;

        private Slot(Object owner, Field field, String tier, Object original, boolean entity) {
            this.owner = owner;
            this.field = field;
            this.tier = tier;
            this.original = original;
            this.entity = entity;
        }

        private void install() {
            RocketModel model = RocketModels.get(tier);
            Object value = model == null ? original : wrap(field.getType(), model, tier, entity);
            if (value == null || failed) return;
            try {
                field.set(owner, value);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                failed = true;
                MyCTMLib.LOG.warn("Cannot replace {} of {}", field.getName(), owner.getClass(), exception);
            }
        }
    }
}
