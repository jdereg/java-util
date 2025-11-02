package com.cedarsoftware.util.geom;

/**
 * <b>Zero-dependency geometric primitive</b> - Immutable Point class representing (x, y) coordinates.
 * <p>
 * This class provides an API-compatible replacement for {@code java.awt.Point} without requiring
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
public final class Point {
    private final int x;
    private final int y;

    /**
     * Creates a Point at the specified (x, y) location.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the x coordinate of this Point.
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y coordinate of this Point.
     */
    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Point)) {
            return false;
        }
        Point other = (Point) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "Point[x=" + x + ",y=" + y + "]";
    }
}
