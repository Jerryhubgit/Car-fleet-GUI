package com.fleettracker.model;

/**
 * Represents an immutable (x, y) coordinate on the fleet map.
 *
 * <p><b>Why immutable?</b> A {@code Position} is read by the display thread
 * while it may be "replaced" by an update thread at almost the same instant.
 * If {@code x} and {@code y} were mutable fields on this class, a reader could
 * observe a <i>torn</i> state: the new {@code x} paired with the old {@code y},
 * because the writer updated one field and got interrupted (by the scheduler,
 * not necessarily an exception) before updating the other. That is a classic
 * concurrency bug called a "torn read."</p>
 *
 * <p>By making {@code Position} immutable, this problem disappears entirely.
 * A vehicle never "mutates its position" â€” it computes a brand-new
 * {@code Position} object and atomically swaps its reference to point at
 * that new object (a single reference assignment, which the JVM guarantees
 * is atomic). Any thread holding an old {@code Position} reference is holding
 * a perfectly valid, self-consistent snapshot forever â€” it simply won't see
 * future updates, which is exactly the semantics we want for a
 * point-in-time read.</p>
 *
 * <p>This class needs no {@code synchronized} keyword anywhere: immutable
 * objects with all-final fields are inherently thread-safe by construction.
 * This is one of the cheapest and most powerful concurrency techniques in
 * Java â€” prefer immutability before reaching for locks.</p>
 */
public final class Position {

    private final int x;
    private final int y;

    /**
     * Creates a new immutable position.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * Returns a <b>new</b> {@code Position} offset from this one by the given
     * deltas, clamped to stay within the given map boundaries.
     *
     * <p>This method never mutates {@code this} â€” it returns a fresh object,
     * which is the defining trait of an immutable value type. Callers
     * (typically a movement strategy) use this to compute "where should the
     * vehicle be next" without ever touching shared mutable state directly.</p>
     *
     * @param dx      change in x
     * @param dy      change in y
     * @param minX    minimum allowed x (inclusive)
     * @param minY    minimum allowed y (inclusive)
     * @param maxX    maximum allowed x (inclusive)
     * @param maxY    maximum allowed y (inclusive)
     * @return a new, boundary-clamped {@code Position}
     */
    public Position move(int dx, int dy, int minX, int minY, int maxX, int maxY) {
        int newX = clamp(this.x + dx, minX, maxX);
        int newY = clamp(this.y + dy, minY, maxY);
        return new Position(newX, newY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position other = (Position) o;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }
}
