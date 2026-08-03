package com.fleettracker.simulation;

import com.fleettracker.model.Vehicle;

/**
 * A single unit of periodic work: "move this one vehicle one tick forward."
 *
 * <p>Each {@code Vehicle} in the fleet gets its own {@code VehicleUpdateTask},
 * scheduled to run repeatedly by {@link VehicleSimulator}. This is what
 * makes vehicles move independently, as required: vehicle A's task and
 * vehicle B's task are entirely separate {@link Runnable} instances that the
 * executor's thread pool can run truly in parallel, since they touch
 * different {@code Vehicle} monitors and share no mutable state with each
 * other (they only share the stateless {@link MovementStrategy} and the
 * immutable {@link MapBounds}).</p>
 */
public class VehicleUpdateTask implements Runnable {

    private final Vehicle vehicle;
    private final MovementStrategy strategy;
    private final MapBounds bounds;

    public VehicleUpdateTask(Vehicle vehicle, MovementStrategy strategy, MapBounds bounds) {
        this.vehicle = vehicle;
        this.strategy = strategy;
        this.bounds = bounds;
    }

    @Override
    public void run() {
        try {
            strategy.move(vehicle, bounds);
        } catch (RuntimeException e) {
            // A single vehicle's update failing must never silently kill the
            // ScheduledExecutorService's repeating schedule for that task
            // (an uncaught exception from a scheduled task cancels all
            // future executions of it). We log and move on so the rest of
            // the fleet is unaffected.
            System.err.println("Update failed for vehicle " + vehicle.getId() + ": " + e.getMessage());
        }
    }
}
