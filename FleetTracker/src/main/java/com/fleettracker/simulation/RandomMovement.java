package com.fleettracker.simulation;

import com.fleettracker.model.Position;
import com.fleettracker.model.Vehicle;
import com.fleettracker.model.VehicleSnapshot;
import com.fleettracker.model.VehicleStatus;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A {@link MovementStrategy} that moves a vehicle randomly: it may change
 * direction slightly, change speed slightly, occasionally stop (become
 * {@link VehicleStatus#PARKED}), and occasionally resume from a stop -
 * always remaining within the configured {@link MapBounds}.
 *
 * <p><b>Why {@link ThreadLocalRandom} instead of a shared {@code
 * java.util.Random}?</b> This strategy instance is shared by every vehicle's
 * update task, running concurrently on a pool of threads. A single shared
 * {@code java.util.Random} is internally thread-safe (its methods are
 * synchronized/CAS-based), but every thread calling it concurrently would
 * contend for the same internal seed state, becoming a hidden bottleneck
 * that serializes otherwise-independent vehicle updates. {@code
 * ThreadLocalRandom.current()} gives each calling thread its own private
 * generator instance with no shared state and therefore no contention at
 * all - the correct tool whenever multiple threads need random numbers
 * concurrently.</p>
 */
public class RandomMovement implements MovementStrategy {

    private static final int MIN_SPEED = 0;
    private static final int MAX_SPEED = 80;
    private static final int MAX_SPEED_DELTA = 10;
    private static final int MAX_DIRECTION_DELTA_DEGREES = 45;
    private static final int STEP_DISTANCE_DIVISOR = 10; // higher speed -> larger step

    private static final double STOP_PROBABILITY = 0.08;
    private static final double RESUME_PROBABILITY = 0.30;

    @Override
    public void move(Vehicle vehicle, MapBounds bounds) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        VehicleSnapshot current = vehicle.snapshot();

        // Vehicles under maintenance or offline never move on their own.
        if (current.getStatus() == VehicleStatus.OFFLINE
                || current.getStatus() == VehicleStatus.MAINTENANCE) {
            return;
        }

        if (current.getStatus() == VehicleStatus.PARKED) {
            // A parked vehicle occasionally resumes moving.
            if (random.nextDouble() < RESUME_PROBABILITY) {
                vehicle.setStatus(VehicleStatus.MOVING);
            }
            return; // Stay put this tick regardless.
        }

        // Status is MOVING: occasionally stop instead of moving this tick.
        if (random.nextDouble() < STOP_PROBABILITY) {
            vehicle.setStatus(VehicleStatus.PARKED);
            return;
        }

        int newDirection = wrapDegrees(current.getDirection()
                + random.nextInt(-MAX_DIRECTION_DELTA_DEGREES, MAX_DIRECTION_DELTA_DEGREES + 1));

        int newSpeed = clamp(current.getSpeed()
                + random.nextInt(-MAX_SPEED_DELTA, MAX_SPEED_DELTA + 1), MIN_SPEED, MAX_SPEED);

        int stepDistance = Math.max(1, newSpeed / STEP_DISTANCE_DIVISOR);
        double radians = Math.toRadians(newDirection);
        int dx = (int) Math.round(Math.cos(radians) * stepDistance);
        int dy = (int) Math.round(Math.sin(radians) * stepDistance);

        Position newPosition = current.getPosition().move(
                dx, dy, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());

        vehicle.applyMovement(newPosition, newSpeed, newDirection);

        // Small, speed-proportional drain each tick - just enough to make
        // the battery/fuel fields change visibly once the GUI displays them.
        int drainAmount = Math.max(0, newSpeed / 40);
        vehicle.drain(drainAmount, drainAmount);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int wrapDegrees(int degrees) {
        int wrapped = degrees % 360;
        return wrapped < 0 ? wrapped + 360 : wrapped;
    }
}
