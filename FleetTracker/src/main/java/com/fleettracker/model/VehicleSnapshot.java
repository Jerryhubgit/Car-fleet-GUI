package com.fleettracker.model;

/**
 * An immutable, point-in-time copy of a {@link Vehicle}'s state.
 *
 * <p><b>Why this class exists:</b> {@code Vehicle} is mutable and is actively
 * written to by its own update thread. If the display thread (or any other
 * caller) were handed a live {@code Vehicle} reference, it would either need
 * to synchronize on it for every field read (verbose and easy to get wrong â€”
 * you'd need to hold the lock across reading id/type/status/speed/position
 * together to get a consistent picture), or risk reading a
 * partially-updated, inconsistent view.</p>
 *
 * <p>Instead, {@link Vehicle#snapshot()} takes a single {@code synchronized}
 * lock just long enough to copy every field at once into a brand-new,
 * fully-immutable {@code VehicleSnapshot}. Once created, this object can be
 * freely read by any thread, forever, with zero synchronization â€” because
 * nothing about it can ever change. This is the standard pattern for safely
 * publishing a consistent view of mutable state across threads: copy while
 * holding the lock, then hand out the immutable copy lock-free.</p>
 */
public final class VehicleSnapshot {

    private final String id;
    private final VehicleType type;
    private final String driverName;
    private final Position position;
    private final int speed;
    private final int direction;
    private final VehicleStatus status;
    private final int batteryPercent;
    private final int fuelPercent;

    public VehicleSnapshot(String id, VehicleType type, String driverName,
                            Position position, int speed, int direction,
                            VehicleStatus status, int batteryPercent, int fuelPercent) {
        this.id = id;
        this.type = type;
        this.driverName = driverName;
        this.position = position; // Position is immutable, safe to share directly
        this.speed = speed;
        this.direction = direction;
        this.status = status;
        this.batteryPercent = batteryPercent;
        this.fuelPercent = fuelPercent;
    }

    public String getId() {
        return id;
    }

    public VehicleType getType() {
        return type;
    }

    public String getDriverName() {
        return driverName;
    }

    public Position getPosition() {
        return position;
    }

    public int getSpeed() {
        return speed;
    }

    public int getDirection() {
        return direction;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public int getFuelPercent() {
        return fuelPercent;
    }

    @Override
    public String toString() {
        return String.format("%-6s %-10s %-8s %5d %s",
                id, type, status, speed, position);
    }
}
