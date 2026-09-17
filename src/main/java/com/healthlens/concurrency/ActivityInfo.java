package com.healthlens.concurrency;

/**
 * One row in the Activity Center: a background job the app is actually
 * running, described in words a user understands, plus the live Thread
 * behind it so its real {@link Thread.State} can be shown.
 *
 * LAB CONCEPTS: Thread.getState(), the six thread states (Lab 2, Task 4),
 * Thread.currentThread()/getName(), and safe publication of the mutable
 * status field via {@code volatile}.
 */
public class ActivityInfo {

    private final String id;
    private final String displayName;
    private final String plainDescription;
    private final String labConcept;

    /** May be null for work that runs on a pool instead of a dedicated thread. */
    private final Thread thread;

    /** Written by a worker thread, read by the JavaFX thread -> must be volatile. */
    private volatile String status;

    public ActivityInfo(String id,
                        String displayName,
                        String plainDescription,
                        String labConcept,
                        Thread thread,
                        String initialStatus) {
        this.id = id;
        this.displayName = displayName;
        this.plainDescription = plainDescription;
        this.labConcept = labConcept;
        this.thread = thread;
        this.status = initialStatus;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPlainDescription() {
        return plainDescription;
    }

    public String getLabConcept() {
        return labConcept;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** Live thread state, read at the moment the UI asks for it. */
    public String getThreadStateText() {
        if (thread == null) {
            return "POOL";
        }
        return thread.getState().name();
    }

    public String getThreadName() {
        return thread == null ? "pool worker" : thread.getName();
    }

    public boolean isAlive() {
        return thread != null && thread.isAlive();
    }

    /**
     * Plain-English gloss of a thread state, so the Activity Center reads
     * like a task manager instead of a textbook.
     */
    public String getFriendlyState() {
        if (thread == null) {
            return "handled by the sync pool";
        }
        switch (thread.getState()) {
            case NEW:
                return "not started yet";
            case RUNNABLE:
                return "working";
            case BLOCKED:
                return "queued behind another job";
            case WAITING:
                return "idle, waiting for something to do";
            case TIMED_WAITING:
                return "sleeping until its next turn";
            case TERMINATED:
                return "finished";
            default:
                return "unknown";
        }
    }
}
