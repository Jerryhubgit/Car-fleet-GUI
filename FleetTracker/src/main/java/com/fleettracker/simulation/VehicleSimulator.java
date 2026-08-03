package com.fleettracker.simulation;

import com.fleettracker.model.Vehicle;
import com.fleettracker.tracker.VehicleTracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates the concurrent movement simulation for the whole fleet.
 *
 * <p>This class owns a single {@link ScheduledExecutorService} - a managed
 * thread pool purpose-built for periodic tasks - and schedules one {@link
 * VehicleUpdateTask} per vehicle onto it.</p>
 *
 * <p><b>Why {@code ScheduledExecutorService} instead of raw {@code Thread}
 * + {@code Thread.sleep()} loops?</b> Hand-rolling N threads each with a
 * {@code while(running) { move(); sleep(); }} loop means manually managing
 * thread creation, interruption on shutdown, and exception handling for
 * every one of them. A {@code ScheduledExecutorService}:
 * <ul>
 *   <li>pools and reuses a bounded number of OS threads instead of creating
 *       one raw thread per vehicle (important once fleets grow large);</li>
 *   <li>provides {@code scheduleAtFixedRate}, which handles the "run every
 *       N seconds, forever" timing logic correctly and safely;</li>
 *   <li>gives a clean, standard shutdown protocol ({@code shutdown()} /
 *       {@code shutdownNow()} / {@code awaitTermination()}) instead of
 *       ad-hoc flag-and-interrupt bookkeeping.</li>
 * </ul>
 * </p>
 *
 * <p><b>How pause/resume works.</b> Rather than cancelling and
 * re-submitting every vehicle's scheduled task (which is possible but
 * fiddly to get exactly right), this simulator uses a single shared {@link
 * AtomicBoolean} flag, {@code paused}. Every {@link VehicleUpdateTask}
 * still fires on schedule, but {@link RandomMovement} is never called while
 * paused - the task is a no-op for that tick. {@code AtomicBoolean} (rather
 * than a plain {@code boolean}) is used because the flag is written by
 * whichever thread calls {@code pause()}/{@code resume()} (typically the
 * main/command thread) and read by every update thread on every tick -
 * exactly the kind of single-flag, high-frequency-read/low-frequency-write
 * cross-thread communication {@code AtomicBoolean} is designed for. A plain
 * {@code boolean} would risk one thread never observing another thread's
 * write due to CPU-core caching (a visibility bug); {@code volatile boolean}
 * would also fix visibility, but {@code AtomicBoolean} additionally offers
 * safe compound operations if this design grows to need them (e.g.
 * {@code compareAndSet}).</p>
 */
public class VehicleSimulator {

    private static final int UPDATE_PERIOD_SECONDS = 2;

    private final VehicleTracker tracker;
    private final MovementStrategy movementStrategy;
    private final MapBounds bounds;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private ScheduledExecutorService executor;
    private volatile boolean running = false;

    public VehicleSimulator(VehicleTracker tracker, MovementStrategy movementStrategy, MapBounds bounds) {
        this.tracker = tracker;
        this.movementStrategy = movementStrategy;
        this.bounds = bounds;
    }

    /**
     * Starts the simulation: creates a fresh thread pool and schedules one
     * repeating {@link VehicleUpdateTask} per vehicle currently in the
     * tracker.
     *
     * <p>Vehicles added to the tracker after {@code start()} is called will
     * not automatically get a scheduled task in this simple design; call
     * {@code start()} again (after {@code stop()}) to pick up new vehicles,
     * or see the notes in the accompanying documentation for how this could
     * be extended.</p>
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        int poolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        executor = Executors.newScheduledThreadPool(poolSize);
        paused.set(false);

        for (Vehicle vehicle : tracker.getManagedVehicles()) {
            PausableUpdateTask task = new PausableUpdateTask(
                    new VehicleUpdateTask(vehicle, movementStrategy, bounds), paused);
            // A small random initial delay avoids every vehicle's task firing
            // in perfect lockstep, which would otherwise create a thundering
            // herd of updates every UPDATE_PERIOD_SECONDS instead of a
            // naturally staggered stream of them.
            long initialDelayMillis = (long) (Math.random() * 500);
            executor.scheduleAtFixedRate(task, initialDelayMillis,
                    UPDATE_PERIOD_SECONDS * 1000L, TimeUnit.MILLISECONDS);
        }
        running = true;
    }

    /**
     * Stops the simulation and shuts down its thread pool cleanly.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        running = false;
    }

    /**
     * Pauses movement without stopping the executor - update tasks keep
     * firing on schedule but become no-ops until {@link #resume()} is
     * called.
     */
    public void pause() {
        paused.set(true);
    }

    /**
     * Resumes movement after a {@link #pause()}.
     */
    public void resume() {
        paused.set(false);
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused.get();
    }

    /**
     * Wraps a {@link VehicleUpdateTask} so it checks the shared pause flag
     * before doing any work, without requiring {@link VehicleUpdateTask}
     * itself to know anything about pausing.
     */
    private static final class PausableUpdateTask implements Runnable {
        private final Runnable delegate;
        private final AtomicBoolean paused;

        private PausableUpdateTask(Runnable delegate, AtomicBoolean paused) {
            this.delegate = delegate;
            this.paused = paused;
        }

        @Override
        public void run() {
            if (!paused.get()) {
                delegate.run();
            }
        }
    }
}
