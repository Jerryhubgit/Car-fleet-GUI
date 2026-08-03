package com.fleettracker.tracker;

import com.fleettracker.model.Vehicle;
import com.fleettracker.model.VehicleSnapshot;
import com.fleettracker.model.VehicleStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single, shared, thread-safe repository of all vehicles in the fleet.
 *
 * <p>This class is the "collection-level" monitor in this design (as opposed
 * to {@link Vehicle}, which is the "per-object" monitor). Every thread in the
 * system - update threads, the display thread, and the main/command thread -
 * reaches the fleet's vehicles <b>only</b> through this class. Nothing else
 * ever holds a direct reference to the backing map.</p>
 *
 * <p><b>Why {@link ConcurrentHashMap} instead of a {@code synchronized
 * HashMap}?</b> A plain {@code HashMap} wrapped in {@code synchronized}
 * blocks would force every single operation - a lookup of one vehicle by ID,
 * a display-thread iteration over all vehicles, an add, a remove - to
 * acquire the same one lock and queue up behind each other, even when two
 * operations touch completely unrelated entries. {@code ConcurrentHashMap}
 * uses fine-grained internal locking, so unrelated reads/writes on different
 * keys can proceed concurrently, while still guaranteeing that operations on
 * the same key (and structural operations like resizing) remain safe. This
 * is the standard, idiomatic choice for a concurrently-accessed map in
 * Java.</p>
 *
 * <p><b>Why defensive copying on every "read many" method?</b> If
 * {@code getAllVehicles()} returned the live map's values directly, a caller
 * iterating over that view on one thread could throw a
 * {@code ConcurrentModificationException} (for non-concurrent collections)
 * or, worse, silently observe a fleet that changes mid-iteration if a
 * vehicle is added/removed by another thread while it's being displayed.
 * Returning a defensive, unmodifiable snapshot list means the caller always
 * sees one consistent, frozen picture of the fleet at the moment they asked
 * for it - regardless of what happens afterward on other threads.</p>
 */
public class VehicleTracker {

    private final ConcurrentHashMap<String, Vehicle> vehicles = new ConcurrentHashMap<>();

    /**
     * Adds a vehicle to the fleet.
     *
     * @param vehicle the vehicle to add (must not be null)
     * @throws IllegalArgumentException if a vehicle with the same ID already exists
     */
    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle must not be null");
        }
        // putIfAbsent is an atomic check-then-act operation on ConcurrentHashMap.
        // Using plain containsKey() followed by put() would be a race condition:
        // two threads could both pass containsKey()==false and both put(),
        // with the second silently overwriting the first.
        Vehicle previous = vehicles.putIfAbsent(vehicle.getId(), vehicle);
        if (previous != null) {
            throw new IllegalArgumentException("Vehicle with id " + vehicle.getId() + " already exists");
        }
    }

    /**
     * Removes a vehicle from the fleet by ID.
     *
     * @param id the vehicle's unique ID
     * @return true if a vehicle was removed, false if no such vehicle existed
     */
    public boolean removeVehicle(String id) {
        return vehicles.remove(id) != null;
    }

    /**
     * Looks up a vehicle by ID and returns an immutable snapshot of its
     * current state, or {@link Optional#empty()} if no such vehicle exists.
     *
     * <p>Note this deliberately returns a {@link VehicleSnapshot}, not the
     * live {@link Vehicle} object - callers outside this package should
     * never be handed a mutable, shared {@code Vehicle} reference.</p>
     */
    public Optional<VehicleSnapshot> findVehicle(String id) {
        Vehicle vehicle = vehicles.get(id);
        return vehicle == null ? Optional.empty() : Optional.of(vehicle.snapshot());
    }

    /**
     * Returns the live, internal {@link Vehicle} object for a given ID, for
     * use <b>only</b> by trusted internal collaborators that need to mutate
     * it (namely {@link simulation.VehicleUpdateTask} during simulation).
     * External callers (display, command handling, a future GUI) must use
     * {@link #findVehicle(String)} or {@link #getAllVehicles()} instead,
     * which return safe, immutable snapshots.
     */
    public Optional<Vehicle> getManagedVehicle(String id) {
        return Optional.ofNullable(vehicles.get(id));
    }

    /**
     * Returns an immutable, defensive-copy snapshot of every vehicle
     * currently in the fleet, safe to iterate on any thread without further
     * synchronization.
     */
    public List<VehicleSnapshot> getAllVehicles() {
        List<VehicleSnapshot> snapshot = new ArrayList<>(vehicles.size());
        for (Vehicle vehicle : vehicles.values()) {
            snapshot.add(vehicle.snapshot());
        }
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Returns every internal {@link Vehicle} the tracker currently manages,
     * as a defensive copy of the list itself (the map's structure cannot be
     * corrupted through this list), for use by the simulator to schedule
     * update tasks. The {@code Vehicle} objects themselves remain the shared,
     * synchronized instances - which is intentional, since the simulator
     * needs to mutate them.
     */
    public List<Vehicle> getManagedVehicles() {
        return Collections.unmodifiableList(new ArrayList<>(vehicles.values()));
    }

    /**
     * Returns snapshots of every vehicle currently in the given status.
     */
    public List<VehicleSnapshot> findByStatus(VehicleStatus status) {
        List<VehicleSnapshot> result = new ArrayList<>();
        for (Vehicle vehicle : vehicles.values()) {
            VehicleSnapshot snapshot = vehicle.snapshot();
            if (snapshot.getStatus() == status) {
                result.add(snapshot);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * @return the number of vehicles currently tracked
     */
    public int size() {
        return vehicles.size();
    }
}
