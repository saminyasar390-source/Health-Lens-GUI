package com.healthlens.concurrency;

/**
 * The guided breathing exercise, wrapped up as a proper little service
 * instead of a thread created inline in a button handler.
 *
 * This is the clearest everyday case for a background thread in the whole
 * app: the exercise is mostly sleeping. Four seconds in, four seconds hold,
 * four seconds out. Doing that on the JavaFX thread would freeze the entire
 * window solid for the length of the session — no theme toggle, no sliders,
 * no close button. Off the FX thread it costs nothing and the app stays
 * completely usable.
 *
 * Stopping uses interrupt() rather than any kind of kill: the thread is
 * asked to stop, notices the request the next time it sleeps, restores the
 * interrupt flag, and reports that it ended early. Nothing is forced, and
 * the session always gets to run its own cleanup.
 *
 * LAB CONCEPTS: Thread + Runnable (Tasks 2 and 3), sleep() and
 * TIMED_WAITING (Task 4), cooperative interrupt() with the flag restored
 * (Task 4), join() (Task 4), a Thread object being single-use (Task 2.3).
 */
public class BreathingSession {

    public static final String ACTIVITY_ID = "breathing-session";

    /** Callbacks fire on the breathing thread — wrap UI work in Platform.runLater. */
    public interface Listener {
        void onPhase(String phase, int phaseIndex, int totalPhases);

        void onFinished(boolean completedFully);
    }

    private static final String[] CYCLE = {
            "Breathe in…", "Hold…", "Breathe out…", "Hold…"
    };

    private final long phaseMillis;

    private Thread thread;
    private volatile boolean running;

    public BreathingSession(long phaseMillis) {
        this.phaseMillis = phaseMillis;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Starts a session of {@code cycles} four-phase rounds. A Thread object
     * can only be started once, so a brand new one is created every time -
     * calling start() twice on the same object throws
     * IllegalThreadStateException.
     */
    public void start(int cycles, Listener listener) {
        if (running) {
            return;
        }
        running = true;

        final int totalPhases = cycles * CYCLE.length;

        thread = new Thread(() -> {
            boolean completedFully = true;
            try {
                for (int i = 0; i < totalPhases; i++) {
                    String phase = CYCLE[i % CYCLE.length];
                    listener.onPhase(phase, i + 1, totalPhases);
                    ActivityRegistry.getInstance().updateStatus(ACTIVITY_ID,
                            phase + " (" + (i + 1) + " of " + totalPhases + ")");
                    Thread.sleep(phaseMillis);
                }
            } catch (InterruptedException e) {
                // Restore the request rather than swallowing it.
                Thread.currentThread().interrupt();
                completedFully = false;
            } finally {
                running = false;
                ActivityRegistry.getInstance().unregister(ACTIVITY_ID);
                listener.onFinished(completedFully);
            }
        }, "breathing-session");

        thread.setDaemon(true);
        thread.start();

        ActivityRegistry.getInstance().register(new ActivityInfo(
                ACTIVITY_ID,
                "Breathing session",
                "Paces your breathing without freezing the rest of the app.",
                "Thread + sleep() + cooperative interrupt(), sits in TIMED_WAITING",
                thread,
                "starting"));
    }

    /** Asks the session to stop. Returns immediately. */
    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    /** join(): waits for the session thread to actually finish. */
    public void awaitFinish(long millis) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
