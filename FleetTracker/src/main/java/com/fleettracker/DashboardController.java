package com.fleettracker;

import com.fleettracker.service.FleetManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.Pane;
import javafx.scene.control.ListView;

/**
 * Controller for the main dashboard screen ({@code dashboard.fxml}).
 *
 * <p><b>Phase 2 scope:</b> this class currently only holds references to the
 * FXML elements and stores the {@link FleetManager} it's given. It does not
 * yet read live data or refresh anything - that's deliberately left for
 * Phase 3, so this phase can focus purely on getting the layout and styling
 * right without mixing in threading concerns yet.</p>
 *
 * <p><b>Why the FleetManager is injected rather than created here:</b> the
 * whole app must share exactly one {@code FleetManager} (and therefore one
 * {@code VehicleTracker}). If this controller constructed its own, it would
 * be observing an entirely different, empty fleet than the one actually
 * being simulated - a classic "two sources of truth" bug. {@link Main}
 * builds the one shared instance and hands it in via
 * {@link #initFleetManager(FleetManager)} after loading the FXML.</p>
 */
public class DashboardController {

    // --- Top bar ---
    @FXML private Label liveIndicatorLabel;

    // --- Quick stats sidebar ---
    @FXML private Label totalVehiclesLabel;
    @FXML private Label onlineLabel;
    @FXML private Label onTripLabel;
    @FXML private Label idleLabel;
    @FXML private Label offlineLabel;

    // --- Center map ---
    @FXML private Pane mapPane;

    // --- Vehicle details panel ---
    @FXML private Label detailsVehicleIdLabel;
    @FXML private Label detailsStatusLabel;
    @FXML private Label detailsDriverLabel;
    @FXML private Label detailsSpeedLabel;
    @FXML private Label detailsLocationLabel;
    @FXML private Label detailsBatteryLabel;
    @FXML private Label detailsFuelLabel;

    // --- Fleet summary cards ---
    @FXML private Label totalDistanceLabel;
    @FXML private Label totalTripsLabel;
    @FXML private Label totalFuelUsedLabel;
    @FXML private Label totalTimeLabel;

    // --- Vehicle status chart ---
    @FXML private PieChart statusPieChart;

    // --- Recent alerts ---
    @FXML private ListView<String> alertsListView;

    // --- Vehicle table ---
    @FXML private TableView<?> vehicleTable;

    private FleetManager fleetManager;

    /**
     * Called once by {@link Main} right after the FXML is loaded, to hand
     * this controller the single shared {@link FleetManager} instance.
     */
    public void initFleetManager(FleetManager fleetManager) {
        this.fleetManager = fleetManager;
        // Live data wiring (reading the tracker, refreshing labels/table/
        // chart, handling clicks) is implemented in Phase 3, together with
        // the Platform.runLater()-based refresh loop.
    }
}
