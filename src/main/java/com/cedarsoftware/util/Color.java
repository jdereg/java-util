package com.cedarsoftware.util;

/**
 * Immutable Color class representing RGB or RGBA color values.
 * This is a lightweight replacement for java.awt.Color to eliminate java.desktop dependency.
 * API-compatible with java.awt.Color for common operations.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class Color {
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    // Standard color constants
    public static final Color BLACK = new Color(0, 0, 0);
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color CYAN = new Color(0, 255, 255);
    public static final Color DARK_GRAY = new Color(64, 64, 64);
    public static final Color GRAY = new Color(128, 128, 128);
    public static final Color GREEN = new Color(0, 255, 0);
    public static final Color LIGHT_GRAY = new Color(192, 192, 192);
    public static final Color MAGENTA = new Color(255, 0, 255);
    public static final Color ORANGE = new Color(255, 200, 0);
    public static final Color PINK = new Color(255, 175, 175);
    public static final Color RED = new Color(255, 0, 0);
    public static final Color WHITE = new Color(255, 255, 255);
    public static final Color YELLOW = new Color(255, 255, 0);

    /**
     * Creates an opaque RGB color with the specified red, green, and blue values.
     * @param red the red component (0-255)
     * @param green the green component (0-255)
     * @param blue the blue component (0-255)
     * @throws IllegalArgumentException if any value is out of range 0-255
     */
    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    /**
     * Creates an RGBA color with the specified red, green, blue, and alpha values.
     * @param red the red component (0-255)
     * @param green the green component (0-255)
     * @param blue the blue component (0-255)
     * @param alpha the alpha component (0-255)
     * @throws IllegalArgumentException if any value is out of range 0-255
     */
    public Color(int red, int green, int blue, int alpha) {
        if (red < 0 || red > 255) {
            throw new IllegalArgumentException("Red must be 0-255, got: " + red);
        }
        if (green < 0 || green > 255) {
            throw new IllegalArgumentException("Green must be 0-255, got: " + green);
        }
        if (blue < 0 || blue > 255) {
            throw new IllegalArgumentException("Blue must be 0-255, got: " + blue);
        }
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("Alpha must be 0-255, got: " + alpha);
        }
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    /**
     * Creates a color from a packed RGB integer value (0xRRGGBB).
     * @param rgb the packed RGB value (alpha is set to 255)
     */
    public Color(int rgb) {
        this((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
    }

    /**
     * Creates a color from a packed integer value.
     * @param rgb the packed RGB or ARGB value
     * @param hasAlpha if true, treats rgb as ARGB (0xAARRGGBB); if false, treats as RGB (0xRRGGBB) with alpha=255
     */
    public Color(int rgb, boolean hasAlpha) {
        this((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hasAlpha ? (rgb >> 24) & 0xFF : 255);
    }

    /**
     * Returns the red component (0-255).
     */
    public int getRed() {
        return red;
    }

    /**
     * Returns the green component (0-255).
     */
    public int getGreen() {
        return green;
    }

    /**
     * Returns the blue component (0-255).
     */
    public int getBlue() {
        return blue;
    }

    /**
     * Returns the alpha component (0-255).
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Returns the RGB value representing the color in the default sRGB ColorModel.
     * The alpha value is in bits 24-31, red in bits 16-23, green in bits 8-15, and blue in bits 0-7.
     */
    public int getRGB() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Color)) {
            return false;
        }
        Color other = (Color) obj;
        return red == other.red && green == other.green && blue == other.blue && alpha == other.alpha;
    }

    @Override
    public int hashCode() {
        return getRGB();
    }

    @Override
    public String toString() {
        return "Color[r=" + red + ",g=" + green + ",b=" + blue + ",a=" + alpha + "]";
    }
}
