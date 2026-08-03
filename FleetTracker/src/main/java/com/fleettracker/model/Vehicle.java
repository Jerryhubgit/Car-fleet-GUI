package com.fleettracker.model;

/**
 * Represents a single tracked vehicle in the fleet.
 *
 * <p>This class is a <b>monitor</b> in the classic Java Monitor Pattern
 * sense: every field that can change after construction is private, and the
 * <em>only</em> way to read or write that state is through this object's
 * {@code synchronized} methods, using {@code this} as the intrinsic lock.
 * No other class ever reaches into a {@code Vehicle}'s fields directly.</p>
 *
 * <p><b>Identity vs. state.</b> {@code id}, {@code type}, and
 * {@code driverName} are {@code final} â€” they describe <i>what this vehicle
 * is</i> and never change after construction, so they need no
 * synchronization to read safely from any thread (final fields assigned in
 * the constructor are safely published to all threads once the constructor
 * completes). {@code position}, {@code speed}, {@code direction}, and
 * {@code status} describe <i>what this vehicle is currently doing</i>, and
 * they change continuously while the simulation runs â€” those are the fields
 * this monitor protects.</p>
 *
 * <p><b>Why lock on {@code this} instead of a shared/global lock?</b> Each
 * vehicle moves independently of every other vehicle. If all vehicles shared
 * one lock object, updating vehicle A would block the thread updating
 * vehicle B even though they touch completely unrelated data â€” unnecessary
 * contention that would only get worse as the fleet grows. By synchronizing
 * on each {@code Vehicle} instance itself, every vehicle has its own
 * independent monitor, so N vehicles can be updated by N threads truly in
 * parallel, and only concurrent operations on the <em>same</em> vehicle ever
 * wait on each other.</p>
 *
 * <p><b>Why not use {@code volatile} fields instead of synchronized
 * methods?</b> {@code volatile} guarantees visibility (a write on one thread
 * becomes visible to reads on other threads) and prevents instruction
 * reordering around that field, but it does <em>not</em> give you atomicity
 * across multiple fields. Here we frequently need to update several fields
 * together (e.g. position AND speed AND direction after one movement tick)
 * and read several fields together for a consistent snapshot. That
 * multi-field atomicity is exactly what a monitor lock provides and
 * {@code volatile} cannot.</p>
 */
public class Vehicle {

    // Immutable identity - safe to read from any thread without synchronization.
    private final String id;
    private final VehicleType type;
    private final String driverName;

    // Mutable state - protected by this object's intrinsic lock (the monitor).
    private Position position;
    private int speed;
    private int direction; // degrees, 0-359
    private VehicleStatus status;
    private int batteryPercent;
    private int fuelPercent;

    /**
     * Creates a new vehicle.
     *
     * @param id          unique identifier (e.g. "TX101")
     * @param type        the vehicle's category
     * @param driverName  driver's name, or {@code null} if unassigned
     * @param position    initial position (must not be null)
     * @param speed       initial speed (>= 0)
     * @param direction   initial heading in degrees [0, 359]
     * @param status      initial status
     */
    public Vehicle(String id, VehicleType type, String driverName,
                    Position position, int speed, int direction,
                    VehicleStatus status) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Vehicle id must not be blank");
        }
        if (position == null) {
            throw new IllegalArgumentException("Vehicle position must not be null");
        }
        this.id = id;
        this.type = type;
        this.driverName = driverName;
        this.position = position;
        this.speed = speed;
        this.direction = direction;
        this.status = status;
        this.batteryPercent = 100;
        this.fuelPercent = 100;
    }

    // --- Immutable identity accessors: no synchronization needed. ---

    public String getId() {
        return id;
    }

    public VehicleType getType() {
        return type;
    }

    public String getDriverName() {
        return driverName;
    }

    // --- Mutable state: every accessor and mutator is synchronized. ---

    public synchronized Position getPosition() {
        return position; // Position is immutable, safe to hand out directly.
    }

    public synchronized int getSpeed() {
        return speed;
    }

    public synchronized int getDirection() {
        return direction;
    }

    public synchronized VehicleStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public synchronized int getBatteryPercent() {
        return batteryPercent;
    }

    public synchronized int getFuelPercent() {
        return fuelPercent;
    }

    /**
     * Atomically drains battery and fuel by the given amounts, clamped to
     * [0, 100]. Bundled into one synchronized method for the same reason as
     * {@link #applyMovement}: so no reader ever sees battery drained but
     * fuel not yet updated, or vice versa.
     */
    public synchronized void drain(int batteryDelta, int fuelDelta) {
        this.batteryPercent = clampPercent(this.batteryPercent - batteryDelta);
        this.fuelPercent = clampPercent(this.fuelPercent - fuelDelta);
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    /**
     * Atomically applies a full movement update: new position, speed, and
     * direction all at once.
     *
     * <p>This is deliberately a single method rather than three separate
     * setter calls. If a caller invoked {@code setPosition()}, then
     * {@code setSpeed()}, then {@code setDirection()} as three separate
     * synchronized calls, another thread (the display thread) could read the
     * vehicle <em>between</em> those calls and see a vehicle with its new
     * position but old speed â€” an inconsistent, half-updated snapshot. By
     * doing all three assignments inside one synchronized method, the entire
     * update is atomic: any other thread either sees the state entirely
     * before this update or entirely after it, never in between. This is the
     * core benefit of the Monitor Pattern: it lets you group several related
     * mutations into one indivisible operation.</p>
     *
     * @param newPosition the vehicle's new position
     * @param newSpeed    the vehicle's new speed
     * @param newDirection the vehicle's new heading in degrees
     */
    public synchronized void applyMovement(Position newPosition, int newSpeed, int newDirection) {
        this.position = newPosition;
        this.speed = newSpeed;
        this.direction = newDirection;
    }

    /**
     * Takes an atomic, consistent, immutable copy of this vehicle's current
     * state, safe to read from any thread with no further synchronization.
     *
     * <p>This is the method the display thread (and any future GUI) should
     * always use instead of calling individual getters one at a time, for
     * exactly the reason described in {@link #applyMovement}: individual
     * getters are each atomic on their own, but calling several of them in a
     * row on the caller's side is <em>not</em> atomic as a whole, since the
     * vehicle's update thread could mutate state between those individual
     * calls. Locking once here and copying every field together avoids
     * that.</p>
     *
     * @return an immutable snapshot of this vehicle's current state
     */
    public synchronized VehicleSnapshot snapshot() {
        return new VehicleSnapshot(id, type, driverName, position, speed, direction, status,
                batteryPercent, fuelPercent);
    }

    @Override
    public String toString() {
        return snapshot().toString();
    }
}
