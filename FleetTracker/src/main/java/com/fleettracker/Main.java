package com.fleettracker;

import com.fleettracker.model.Position;
import com.fleettracker.model.Vehicle;
import com.fleettracker.model.VehicleStatus;
import com.fleettracker.model.VehicleType;
import com.fleettracker.service.FleetManager;
import com.fleettracker.simulation.MapBounds;
import com.fleettracker.simulation.RandomMovement;
import com.fleettracker.tracker.VehicleTracker;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JavaFX entry point for the Fleet Tracker desktop application.
 *
 * <p>This class replaces the terminal version's command-loop {@code Main} -
 * but notice how little actually changed underneath: it still creates a
 * {@link VehicleTracker} and a {@link FleetManager}, and still calls {@link
 * FleetManager#startSimulation()} exactly as before. The <em>only</em> new
 * responsibility here is loading a JavaFX window instead of running a
 * {@code Scanner} command loop.</p>
 *
 * <p><b>Threading note:</b> {@link Application#start(Stage)} runs on the
 * <b>JavaFX Application Thread</b> - the one thread JavaFX reserves for all
 * UI work. Everything in this method (loading FXML, building the Scene,
 * showing the Stage) is expected to run here and complete quickly. The fleet
 * simulation, by contrast, keeps running on its own background threads via
 * {@link FleetManager#startSimulation()} - exactly as it did in the terminal
 * version. This class is careful never to block the JavaFX thread on
 * anything simulation-related; screens will observe the tracker's state and
 * push updates back onto this thread safely via {@code Platform.runLater()}
 * in Phase 3.</p>
 */
public class Main extends Application {

    private static final MapBounds MAP_BOUNDS = new MapBounds(0, 0, 100, 100);

    private FleetManager fleetManager;

    @Override
    public void start(Stage primaryStage) throws IOException {
        VehicleTracker tracker = new VehicleTracker();
        fleetManager = new FleetManager(tracker, new RandomMovement(), MAP_BOUNDS);
        seedFleet(fleetManager);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        Parent root = loader.load();

        // Hand the already-constructed FleetManager to the controller rather
        // than letting the controller build its own - this keeps exactly one
        // FleetManager (and therefore exactly one VehicleTracker) for the
        // whole application, which matters for thread-safety: every screen
        // must observe the same shared, synchronized state.
        DashboardController controller = loader.getController();
        controller.initFleetManager(fleetManager);

        Scene scene = new Scene(root, 1536, 1024);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        primaryStage.setTitle("FleetTracker");
        primaryStage.setScene(scene);
        primaryStage.show();

        fleetManager.startSimulation();
    }

    @Override
    public void stop() {
        // JavaFX calls this when the window closes. Just like the terminal
        // version's shutdown hook, we must cleanly stop the background
        // executors so the JVM doesn't hang around after the window closes.
        if (fleetManager != null) {
            fleetManager.shutdown();
        }
    }

    private void seedFleet(FleetManager fleetManager) {
        String[][] seed = {
                {"TX101", "TAXI", "Ada Lovelace"},
                {"TX102", "TAXI", "Grace Hopper"},
                {"DL301", "DELIVERY", "Linus Torvalds"},
                {"DL302", "DELIVERY", "Margaret Hamilton"},
                {"PC201", "POLICE", "Alan Turing"},
                {"PC210", "POLICE", "Barbara Liskov"},
                {"AM105", "AMBULANCE", "Katherine Johnson"},
                {"BU401", "BUS", "Dennis Ritchie"},
        };

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (String[] entry : seed) {
            Position start = new Position(
                    random.nextInt(MAP_BOUNDS.getMinX(), MAP_BOUNDS.getMaxX() + 1),
                    random.nextInt(MAP_BOUNDS.getMinY(), MAP_BOUNDS.getMaxY() + 1));
            Vehicle vehicle = new Vehicle(
                    entry[0],
                    VehicleType.valueOf(entry[1]),
                    entry[2],
                    start,
                    random.nextInt(0, 60),
                    random.nextInt(0, 360),
                    VehicleStatus.MOVING);
            fleetManager.addVehicle(vehicle);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
