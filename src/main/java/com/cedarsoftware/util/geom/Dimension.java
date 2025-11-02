package com.cedarsoftware.util.geom;

/**
 * <b>Zero-dependency geometric primitive</b> - Immutable Dimension class representing width and height.
 * <p>
 * This class provides an API-compatible replacement for {@code java.awt.Dimension} without requiring
 * the {@code java.desktop} module (~8MB), making it ideal for headless servers, microservices,
 * and modular applications.
 * <p>
 * <b>No AWT dependency</b> - This class is completely independent and does not require {@code java.awt}.
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
public final class Dimension {
    private final int width;
    private final int height;

    /**
     * Creates a Dimension with the specified width and height.
     * @param width the width dimension
     * @param height the height dimension
     */
    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the width of this Dimension.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this Dimension.
     */
    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Dimension)) {
            return false;
        }
        Dimension other = (Dimension) obj;
        return width == other.width && height == other.height;
    }

    @Override
    public int hashCode() {
        return 31 * width + height;
    }

    @Override
    public String toString() {
        return "Dimension[width=" + width + ",height=" + height + "]";
    }
}
