package com.fleettracker.simulation;

/**
 * Immutable description of the rectangular map boundary vehicles must stay
 * within (e.g. 0 &le; x &le; 100, 0 &le; y &le; 100).
 *
 * <p>Like {@link model.Position}, this is a plain immutable value object -
 * safe to share across every update thread with no synchronization,
 * since it is configured once at startup and never changes.</p>
 */
public final class MapBounds {

    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;

    public MapBounds(int minX, int minY, int maxX, int maxY) {
        if (minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("Invalid map bounds");
        }
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }
}
