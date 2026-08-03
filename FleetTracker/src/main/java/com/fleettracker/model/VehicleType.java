package com.fleettracker.model;

/**
 * Represents the category of a {@link Vehicle}.
 *
 * <p>Same rationale as {@link VehicleStatus}: an enum's constants are
 * immutable singletons, so this type is trivially thread-safe to share
 * across threads.</p>
 */
public enum VehicleType {
    TAXI,
    POLICE,
    DELIVERY,
    AMBULANCE,
    BUS
}
