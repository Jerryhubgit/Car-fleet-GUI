package com.fleettracker.model;

/**
 * Represents the operational status of a {@link Vehicle}.
 *
 * <p>Enums in Java are, by construction, a fixed set of singleton instances
 * created once by the JVM class loader. There is no way to mutate an enum
 * constant's identity after the fact, so passing {@code VehicleStatus} values
 * between threads requires no synchronization at all â€” another example of
 * "prefer immutability over locking" in this design.</p>
 */
public enum VehicleStatus {
    MOVING,
    PARKED,
    OFFLINE,
    MAINTENANCE
}
