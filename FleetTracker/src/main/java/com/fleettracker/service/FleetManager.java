package com.fleettracker.service;

import com.fleettracker.model.Vehicle;
import com.fleettracker.model.VehicleSnapshot;
import com.fleettracker.model.VehicleStatus;
import com.fleettracker.simulation.MapBounds;
import com.fleettracker.simulation.MovementStrategy;
import com.fleettracker.tracker.VehicleTracker;

import java.util.List;
import java.util.Optional;

import com.fleettracker.simulation.VehicleSimulator;

/**
 * The public-facing facade for all fleet operations.
 *
 * <p>This is the single entry point {@link Main} (and, later, a GUI
 * controller) talks to. It deliberately hides {@link VehicleTracker} and
 * {@link VehicleSimulator} as implementation details - callers never need
 * to know a {@code ConcurrentHashMap} or a {@code ScheduledExecutorService}
 * is involved at all. This is what gives the project an MVC-friendly seam:
 * {@code FleetManager} plays the role of Controller/Model-facade, and the
 * JavaFX dashboard controller plays the role of View.</p>
 *
 * <p>This class itself does not need any {@code synchronized} keywords: it
 * simply delegates every operation to {@link VehicleTracker} (already
 * thread-safe) and {@link VehicleSimulator} (already thread-safe), so it
 * inherits their safety without adding any locking of its own. This is a
 * deliberate design principle worth naming: <b>compose thread-safe
 * building blocks rather than re-locking around them</b> - adding another
 * layer of synchronization here would be redundant at best and a source of
 * lock-ordering bugs at worst.</p>
 */
public class FleetManager {

    private final VehicleTracker tracker;
    private final VehicleSimulator simulator;

    public FleetManager(VehicleTracker tracker, MovementStrategy movementStrategy, MapBounds bounds) {
        this.tracker = tracker;
        this.simulator = new VehicleSimulator(tracker, movementStrategy, bounds);
    }

    // --- Fleet CRUD operations (delegate straight to the thread-safe tracker) ---

    public void addVehicle(Vehicle vehicle) {
        tracker.addVehicle(vehicle);
    }

    public boolean removeVehicle(String id) {
        return tracker.removeVehicle(id);
    }

    public Optional<VehicleSnapshot> findVehicle(String id) {
        return tracker.findVehicle(id);
    }

    /**
     * Updates a vehicle's status (e.g. sending it to MAINTENANCE or
     * OFFLINE). Position/speed/direction are intentionally not
     * user-settable while the simulation owns them, to avoid a user command
     * racing with the update thread in confusing ways; status changes are
     * safe because {@link Vehicle#setStatus} is itself synchronized.
     */
    public boolean updateVehicleStatus(String id, VehicleStatus newStatus) {
        Optional<Vehicle> vehicle = tracker.getManagedVehicle(id);
        vehicle.ifPresent(v -> v.setStatus(newStatus));
        return vehicle.isPresent();
    }

    public List<VehicleSnapshot> getAllVehicles() {
        return tracker.getAllVehicles();
    }

    public List<VehicleSnapshot> findByStatus(VehicleStatus status) {
        return tracker.findByStatus(status);
    }

    // --- Simulation control ---

    public void startSimulation() {
        simulator.start();
    }

    public void stopSimulation() {
        simulator.stop();
    }

    public void pauseSimulation() {
        simulator.pause();
    }

    public void resumeSimulation() {
        simulator.resume();
    }

    public boolean isSimulationRunning() {
        return simulator.isRunning();
    }

    public boolean isSimulationPaused() {
        return simulator.isPaused();
    }

    // --- Display control ---
    //
    // Note: the terminal version's periodic DisplayTask has been removed.
    // In Phase 3, the JavaFX dashboard will get its own refresh mechanism
    // (a scheduled task that reads the tracker and pushes updates onto the
    // UI thread via Platform.runLater()), which will live in the `ui`/
    // `controller` layer rather than here - FleetManager doesn't need to
    // know a GUI exists at all.

    public synchronized void shutdown() {
        stopSimulation();
    }
}
