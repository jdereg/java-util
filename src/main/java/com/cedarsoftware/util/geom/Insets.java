package com.cedarsoftware.util.geom;

/**
 * <b>Zero-dependency geometric primitive</b> - Immutable Insets class representing the borders of a container.
 * <p>
 * This class provides an API-compatible replacement for {@code java.awt.Insets} without requiring
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
public final class Insets {
    private final int top;
    private final int left;
    private final int bottom;
    private final int right;

    /**
     * Creates Insets with the specified top, left, bottom, and right values.
     * @param top the inset from the top
     * @param left the inset from the left
     * @param bottom the inset from the bottom
     * @param right the inset from the right
     */
    public Insets(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    /**
     * Returns the inset from the top.
     */
    public int getTop() {
        return top;
    }

    /**
     * Returns the inset from the left.
     */
    public int getLeft() {
        return left;
    }

    /**
     * Returns the inset from the bottom.
     */
    public int getBottom() {
        return bottom;
    }

    /**
     * Returns the inset from the right.
     */
    public int getRight() {
        return right;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Insets)) {
            return false;
        }
        Insets other = (Insets) obj;
        return top == other.top && left == other.left && bottom == other.bottom && right == other.right;
    }

    @Override
    public int hashCode() {
        int result = top;
        result = 31 * result + left;
        result = 31 * result + bottom;
        result = 31 * result + right;
        return result;
    }

    @Override
    public String toString() {
        return "Insets[top=" + top + ",left=" + left + ",bottom=" + bottom + ",right=" + right + "]";
    }
}
