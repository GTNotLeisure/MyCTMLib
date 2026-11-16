package com.github.wohaopa.MyCTMLib.client.texture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.github.wohaopa.MyCTMLib.mixins.early.AccessorTextureAtlasSprite;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CtmSheetSprite extends InterpolatedIcon {

    // Edge pixels replicated around every unit so filtered sampling stays inside the unit.
    public static final int DEFAULT_PADDING = 8;

    private static final int COMPACT_GRID = 2;

    private final ResourceLocation sheetLocation;
    private final BufferedImage sheet;
    private final int columns;
    private final int rows;
    private final int unitSize;
    private final int cellSize;
    private final boolean interpolate;
    private final IIcon[] units;

    /**
     * Creates a connection sheet sprite laid out as two by two compact variants.
     *
     * @param name          the atlas registration name; normally the sheet resource name, possibly a derived name
     * @param sheetLocation the sheet resource, used to read the animation metadata
     * @param sheet         the already loaded sheet image
     * @param padding       the number of replicated edge pixels added around each unit
     * @param interpolate   whether animation frames should be blended
     */
    public CtmSheetSprite(String name, ResourceLocation sheetLocation, BufferedImage sheet, int padding,
        boolean interpolate) {
        this(name, sheetLocation, sheet, COMPACT_GRID, COMPACT_GRID, padding, interpolate);
    }

    /**
     * Creates a connection sheet sprite.
     *
     * @param name          the atlas registration name; normally the sheet resource name, possibly a derived name
     * @param sheetLocation the sheet resource, used to read the animation metadata
     * @param sheet         the already loaded sheet image
     * @param columns       the number of unit columns in the sheet
     * @param rows          the number of unit rows in the sheet
     * @param padding       the number of replicated edge pixels added around each unit
     * @param interpolate   whether animation frames should be blended
     */
    public CtmSheetSprite(String name, ResourceLocation sheetLocation, BufferedImage sheet, int columns, int rows,
        int padding, boolean interpolate) {
        // The parent halves the grid to get one mipmap region per unit, so ask for twice the unit grid.
        super(name, columns * COMPACT_GRID, rows * COMPACT_GRID);
        if (padding < 0 || sheet.getWidth() < columns || sheet.getWidth() % columns != 0) {
            throw new IllegalArgumentException("Invalid connection sheet dimensions or padding");
        }
        this.sheetLocation = sheetLocation;
        this.sheet = sheet;
        this.columns = columns;
        this.rows = rows;
        this.unitSize = Math.max(1, sheet.getWidth() / columns);
        this.cellSize = Math.addExact(unitSize, Math.multiplyExact(padding, 2));
        this.interpolate = interpolate;
        this.units = new IIcon[Math.multiplyExact(columns, rows)];
        Math.multiplyExact(cellSize, columns);
        Math.multiplyExact(cellSize, rows);
        for (int unit = 0; unit < units.length; unit++) {
            units[unit] = new UnitIcon(unit);
        }
    }

    /**
     * Returns one icon per unit, each covering only that unit's content.
     *
     * @return the unit icons in sheet order
     */
    public IIcon[] getUnitIcons() {
        return units;
    }

    /** Checks compatibility with the default compact layout and padding. */
    public boolean isSameSource(BufferedImage image, boolean interpolate) {
        return isSameSource(image, COMPACT_GRID, COMPACT_GRID, DEFAULT_PADDING, interpolate);
    }

    /** Allows registrations to share a sprite only when their layout, padding, animation and pixels match. */
    public boolean isSameSource(BufferedImage image, int columns, int rows, int padding, boolean interpolate) {
        if (image == null || this.interpolate != interpolate) {
            return false;
        }
        if (this.columns != columns || this.rows != rows || cellSize - unitSize != (long) padding * 2) {
            return false;
        }
        if (image.getWidth() != sheet.getWidth() || image.getHeight() != sheet.getHeight()) {
            return false;
        }
        if (image == sheet) return true;
        int[] expected = new int[sheet.getWidth()];
        int[] actual = new int[sheet.getWidth()];
        for (int y = 0; y < sheet.getHeight(); y++) {
            sheet.getRGB(0, y, expected.length, 1, expected, 0, expected.length);
            image.getRGB(0, y, actual.length, 1, actual, 0, actual.length);
            if (!Arrays.equals(expected, actual)) return false;
        }
        return true;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        try {
            IResource resource = manager.getResource(sheetLocation);
            try (InputStream stream = resource.getInputStream()) {
                AnimationMetadataSection animation = (AnimationMetadataSection) resource.getMetadata("animation");
                int frameHeight = Math.multiplyExact(unitSize, rows);
                int frameCount = animation == null ? 1 : Math.max(1, sheet.getHeight() / frameHeight);
                List<int[][]> frames = new ArrayList<>(frameCount);
                for (int frame = 0; frame < frameCount; frame++) {
                    BufferedImage image = layout(frame * frameHeight);
                    int[][] data = { ((DataBufferInt) image.getRaster()
                        .getDataBuffer()).getData() };
                    fixTransparentPixels(data);
                    frames.add(data);
                }

                width = cellSize * columns;
                height = cellSize * rows;
                if (animation != null) {
                    if (animation.getFrameCount() == 0) {
                        List<AnimationFrame> sequence = new ArrayList<>(frameCount);
                        for (int frame = 0; frame < frameCount; frame++) sequence.add(new AnimationFrame(frame, -1));
                        animation = new AnimationMetadataSection(sequence, width, height, animation.getFrameTime());
                    } else {
                        for (int index : animation.getFrameIndexSet()) {
                            if (index < 0 || index >= frameCount) {
                                throw new IllegalArgumentException(
                                    "Invalid connection sheet animation frame: " + index);
                            }
                        }
                    }
                }
                // Vanilla loadSprite accepts mipmap images and assumes square animation frames.
                setFramesTextureData(frames);
                ((AccessorTextureAtlasSprite) this).setAnimationMetadata(animation);
                frameCounter = 0;
                tickCounter = 0;
                return false;
            }
        } catch (IOException | RuntimeException e) {
            return true;
        }
    }

    @Override
    public void updateAnimation() {
        if (interpolate) {
            super.updateAnimation();
            return;
        }

        // Reproduces the plain frame stepping, because the interpolation of the parent cannot be switched off.
        AnimationMetadataSection animation = ((AccessorTextureAtlasSprite) this).getAnimationMetadata();
        if (animation == null) {
            return;
        }

        tickCounter++;

        if (tickCounter >= animation.getFrameTimeSingle(frameCounter)) {
            int current = animation.getFrameIndex(frameCounter);
            int count = animation.getFrameCount() == 0 ? framesTextureData.size() : animation.getFrameCount();
            frameCounter = (frameCounter + 1) % count;
            tickCounter = 0;
            int next = animation.getFrameIndex(frameCounter);

            if (current != next && next >= 0 && next < framesTextureData.size()) {
                TextureUtil
                    .uploadTextureMipmap(framesTextureData.get(next), width, height, originX, originY, false, false);
            }
        }
    }

    private BufferedImage layout(int frameOffset) {
        int padding = (cellSize - unitSize) / 2;
        BufferedImage target = new BufferedImage(cellSize * columns, cellSize * rows, BufferedImage.TYPE_INT_ARGB);
        int targetWidth = target.getWidth();
        int[] pixels = ((DataBufferInt) target.getRaster()
            .getDataBuffer()).getData();
        int sheetHeight = sheet.getHeight();
        for (int unit = 0; unit < units.length; unit++) {
            int column = unit % columns;
            int row = unit / columns;
            int sourceX = column * unitSize;
            int sourceY = frameOffset + row * unitSize;
            int targetX = column * cellSize;
            int targetY = row * cellSize;
            for (int y = 0; y < cellSize; y++) {
                // A sheet that is shorter than a whole frame keeps its last row instead of reading past the image.
                int unitY = Math.min(sheetHeight - 1, sourceY + clamp(y - padding));
                int offset = (targetY + y) * targetWidth + targetX;
                sheet.getRGB(sourceX, unitY, unitSize, 1, pixels, offset + padding, targetWidth);
                Arrays.fill(pixels, offset, offset + padding, pixels[offset + padding]);
                Arrays.fill(
                    pixels,
                    offset + padding + unitSize,
                    offset + cellSize,
                    pixels[offset + padding + unitSize - 1]);
            }
        }
        return target;
    }

    private int clamp(int offset) {
        if (offset < 0) {
            return 0;
        }
        return Math.min(offset, unitSize - 1);
    }

    private float contentOffset(int index, boolean atUnitStart, int unitCount) {
        int padding = (cellSize - unitSize) / 2;
        int total = cellSize * unitCount;
        int offset = index * cellSize + (atUnitStart ? padding : padding + unitSize);
        return (float) offset / total;
    }

    private class UnitIcon implements IIcon {

        private final int unit;

        private UnitIcon(int unit) {
            this.unit = unit;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getMinU() {
            return CtmSheetSprite.this.getMinU() + (CtmSheetSprite.this.getMaxU() - CtmSheetSprite.this.getMinU())
                * contentOffset(unit % columns, true, columns);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getMaxU() {
            return CtmSheetSprite.this.getMinU() + (CtmSheetSprite.this.getMaxU() - CtmSheetSprite.this.getMinU())
                * contentOffset(unit % columns, false, columns);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getInterpolatedU(double value) {
            float min = getMinU();
            return (float) (min + (getMaxU() - min) * value / 16.0D);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getMinV() {
            return CtmSheetSprite.this.getMinV() + (CtmSheetSprite.this.getMaxV() - CtmSheetSprite.this.getMinV())
                * contentOffset(unit / columns, true, rows);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getMaxV() {
            return CtmSheetSprite.this.getMinV() + (CtmSheetSprite.this.getMaxV() - CtmSheetSprite.this.getMinV())
                * contentOffset(unit / columns, false, rows);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getInterpolatedV(double value) {
            float min = getMinV();
            return (float) (min + (getMaxV() - min) * value / 16.0D);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public String getIconName() {
            return CtmSheetSprite.super.getIconName();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getIconWidth() {
            return unitSize;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getIconHeight() {
            return unitSize;
        }
    }
}
