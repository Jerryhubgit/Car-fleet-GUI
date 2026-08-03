package com.fleettracker.simulation;

import com.fleettracker.model.Vehicle;

/**
 * Strategy interface for how a vehicle decides its next move.
 *
 * <p>This is the Strategy Pattern applied to movement: {@link
 * VehicleUpdateTask} doesn't know or care <em>how</em> a vehicle picks its
 * next position/speed/direction - it just calls {@link #move}. Today the
 * only implementation is {@link RandomMovement}, but this seam makes it easy
 * to later add e.g. {@code PatrolMovement} or {@code FixedRouteMovement}
 * without touching any scheduling or concurrency code.</p>
 *
 * <p>Implementations must be stateless (or otherwise thread-safe), since a
 * single strategy instance is shared by every {@link VehicleUpdateTask}
 * across every vehicle and every update thread.</p>
 */
public interface MovementStrategy {

    /**
     * Computes and applies the next movement step for the given vehicle,
     * respecting the given map boundaries.
     *
     * <p>Implementations should read the vehicle's current state via {@link
     * Vehicle#snapshot()} and apply any change via {@link
     * Vehicle#applyMovement} / {@link Vehicle#setStatus}, so that the
     * vehicle's own monitor lock protects the update - this method itself
     * does no locking of its own.</p>
     *
     * @param vehicle the vehicle to move (mutated in place via its own synchronized methods)
     * @param bounds  the map boundaries the vehicle must stay within
     */
    void move(Vehicle vehicle, MapBounds bounds);
}
