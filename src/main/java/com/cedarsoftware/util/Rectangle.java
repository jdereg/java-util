package com.cedarsoftware.util;

/**
 * Immutable Rectangle class representing a rectangular region defined by location (x, y) and size (width, height).
 * This is a lightweight replacement for java.awt.Rectangle to eliminate java.desktop dependency.
 * API-compatible with java.awt.Rectangle for common operations.
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
public final class Rectangle {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    /**
     * Creates a Rectangle at location (x, y) with the specified width and height.
     * @param x the x coordinate of the upper-left corner
     * @param y the y coordinate of the upper-left corner
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     */
    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the x coordinate of the upper-left corner.
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y coordinate of the upper-left corner.
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the width of this Rectangle.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this Rectangle.
     */
    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Rectangle)) {
            return false;
        }
        Rectangle other = (Rectangle) obj;
        return x == other.x && y == other.y && width == other.width && height == other.height;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return "Rectangle[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
