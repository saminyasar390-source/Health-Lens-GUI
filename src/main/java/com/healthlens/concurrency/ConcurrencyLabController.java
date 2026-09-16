package com.healthlens.concurrency;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Duration;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * CONTROLLER for ConcurrencyLab.fxml.
 *
 * CRITICAL RULE FOLLOWED THROUGHOUT THIS CLASS: JavaFX UI controls can only
 * be touched from the JavaFX Application Thread. Every method here that
 * starts a background Thread wraps its UI updates in Platform.runLater(...)
 * so the label/list updates happen safely back on the FX thread, no matter
 * which worker thread produced the result.
 */
public class ConcurrencyLabController {

    // --- Tab 1 ---
    @FXML private Label extendedThreadStateLabel;
    @FXML private Label runnableThreadStateLabel;
    @FXML private ListView<String> countdownLogList;
    private Thread countdownThread;

    // --- Tab 2 ---
    @FXML private Label unsafeResultLabel;
    @FXML private Label safeResultLabel;

    // --- Tab 3 ---
    @FXML private Label boxStatusLabel;
    @FXML private ListView<String> pcLogList;

    // --- Tab 4 ---
    @FXML private ListView<String> poolLogList;
    @FXML private Label callableStatusLabel;
    @FXML private Label shutdownStatusLabel;
    private ExecutorService pool;

    @FXML
    public void initialize() {
        countdownLogList.setItems(FXCollections.observableArrayList());
        pcLogList.setItems(FXCollections.observableArrayList());
        poolLogList.setItems(FXCollections.observableArrayList());
        pool = Executors.newFixedThreadPool(2);
    }

    /** Called by HealthLensController when this window is closed, so the pool doesn't leak. */
    public void shutdown() {
        if (countdownThread != null) {
            countdownThread.interrupt();
        }
        if (pool != null && !pool.isShutdown()) {
            pool.shutdownNow();
        }
    }

    // ===================== TAB 1: THREADS & LIFECYCLE =====================

    private static class NumberThread extends Thread {
        @Override
        public void run() {
            for (int i = 1; i <= 5; i++) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @FXML
    private void handleStartExtendedThread() {
        NumberThread worker = new NumberThread();
        worker.setName("extends-Thread-demo");
        pollThreadState(worker, extendedThreadStateLabel);
        worker.start();
    }

    @FXML
    private void handleStartRunnableThread() {
        Runnable task = () -> {
            for (int i = 1; i <= 5; i++) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        };
        Thread worker = new Thread(task, "Runnable-demo");
        pollThreadState(worker, runnableThreadStateLabel);
        worker.start();
    }

    /** Polls a thread's state every 150ms (safe from the FX thread) until it terminates. */
    private void pollThreadState(Thread thread, Label label) {
        label.setText("State: " + thread.getState());
        Timeline poll = new Timeline(new KeyFrame(Duration.millis(150), e -> {
            label.setText("State: " + thread.getState());
        }));
        poll.setCycleCount(30); // ~4.5s safety cap
        poll.play();
        // Also react as soon as the thread actually finishes, via a tiny watcher thread.
        Thread watcher = new Thread(() -> {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
            Platform.runLater(() -> {
                poll.stop();
                label.setText("State: " + thread.getState());
            });
        }, "state-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    @FXML
    private void handleStartCountdown() {
        countdownLogList.getItems().clear();
        countdownThread = new Thread(() -> {
            try {
                String[] phases = {"Breathe in...", "Hold...", "Breathe out...", "Hold...", "Breathe in..."};
                for (int i = 0; i < phases.length; i++) {
                    final String phase = phases[i];
                    Platform.runLater(() -> countdownLogList.getItems().add(phase));
                    Thread.sleep(500);
                }
                Platform.runLater(() -> countdownLogList.getItems().add("Session finished. Nicely done."));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> countdownLogList.getItems().add("Session stopped early (interrupted) — that's fine too."));
            }
        }, "breathing-session-worker");
        countdownThread.start();
    }

    @FXML
    private void handleCancelCountdown() {
        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }
    }

    // ===================== TAB 2: RACE CONDITIONS =====================

    @FXML
    private void handleRunUnsafeCounter() {
        unsafeResultLabel.setText("Running...");
        new Thread(() -> {
            UnsafeCounter counter = new UnsafeCounter();
            Runnable task = () -> {
                for (int i = 0; i < 100_000; i++) counter.increment();
            };
            Thread t1 = new Thread(task, "unsafe-1");
            Thread t2 = new Thread(task, "unsafe-2");
            t1.start();
            t2.start();
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException ignored) {
            }
            String verdict = counter.count == 200_000 ? " (correct this run — try again!)" : " ❌ steps were lost!";
            String result = "Combined steps: " + counter.count + " / expected 200000" + verdict;
            Platform.runLater(() -> unsafeResultLabel.setText(result));
        }, "unsafe-step-sync").start();
    }

    @FXML
    private void handleRunSafeCounter() {
        safeResultLabel.setText("Running...");
        new Thread(() -> {
            SafeCounter counter = new SafeCounter();
            Runnable task = () -> {
                for (int i = 0; i < 100_000; i++) counter.increment();
            };
            Thread t1 = new Thread(task, "safe-1");
            Thread t2 = new Thread(task, "safe-2");
            t1.start();
            t2.start();
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException ignored) {
            }
            String result = "Combined steps: " + counter.getCount() + " / expected 200000 ✅ always correct";
            Platform.runLater(() -> safeResultLabel.setText(result));
        }, "safe-step-sync").start();
    }

    // ===================== TAB 3: PRODUCER-CONSUMER =====================

    @FXML
    private void handleStartProducerConsumer() {
        pcLogList.getItems().clear();
        Box<Integer> box = new Box<>();
        String[] tips = {
                "Drink a glass of water", "Take a short walk", "Stretch for a minute",
                "Check your posture", "Take a deep breath"
        };

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    box.put(i);
                    final String tip = tips[i - 1];
                    Platform.runLater(() -> {
                        pcLogList.getItems().add("Generated tip: " + tip);
                        boxStatusLabel.setText("Box: [" + tip + "]");
                    });
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "tip-generator");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    int value = box.take();
                    final String tip = tips[value - 1];
                    Platform.runLater(() -> {
                        pcLogList.getItems().add("Notified user: " + tip);
                        boxStatusLabel.setText("Box: [empty]");
                    });
                    Thread.sleep(400);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "notifier");

        producer.start();
        consumer.start();
    }

    // ===================== TAB 4: EXECUTORS & FUTURES =====================

    @FXML
    private void handleSubmitPoolTasks() {
        poolLogList.getItems().clear();
        if (pool == null || pool.isShutdown()) {
            poolLogList.getItems().add("Executor was shut down — reopen the lab to get a fresh pool.");
            return;
        }
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                String workerName = Thread.currentThread().getName();
                Platform.runLater(() -> poolLogList.getItems().add("Sync request " + taskId + " handled by " + workerName));
            });
        }
    }

    @FXML
    private void handleRunCallable() {
        if (pool == null || pool.isShutdown()) {
            callableStatusLabel.setText("Status: executor already shut down");
            return;
        }
        callableStatusLabel.setText("Status: main can keep working while this fetches...");
        Callable<Integer> weeklyStepsTask = () -> {
            int total = 0;
            for (int i = 1; i <= 100; i++) total += i * 50; // simulated daily step totals
            Thread.sleep(400);
            return total;
        };
        Future<Integer> future = pool.submit(weeklyStepsTask);

        new Thread(() -> {
            try {
                int result = future.get();
                Platform.runLater(() -> callableStatusLabel.setText("Status: Weekly step total = " + result));
            } catch (Exception e) {
                Platform.runLater(() -> callableStatusLabel.setText("Status: fetch failed — " + e.getMessage()));
            }
        }, "future-waiter").start();
    }

    @FXML
    private void handleShutdownExecutor() {
        if (pool == null || pool.isShutdown()) {
            shutdownStatusLabel.setText("Status: already shut down");
            return;
        }
        shutdownStatusLabel.setText("Status: shutting down...");
        pool.shutdown();
        new Thread(() -> {
            try {
                if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
                Platform.runLater(() -> shutdownStatusLabel.setText("Status: shut down cleanly"));
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Platform.runLater(() -> shutdownStatusLabel.setText("Status: interrupted during shutdown"));
            }
        }, "shutdown-watcher").start();
    }
}
