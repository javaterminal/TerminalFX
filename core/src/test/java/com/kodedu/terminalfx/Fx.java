package com.kodedu.terminalfx;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** The toolkit, started once for the whole run, and a way to ask it something and wait. */
final class Fx {

    private static boolean started;

    static synchronized void start() throws InterruptedException {
        if (started) {
            return;
        }
        CountDownLatch up = new CountDownLatch(1);
        Platform.startup(up::countDown);
        if (!up.await(60, TimeUnit.SECONDS)) {
            throw new IllegalStateException("the toolkit never started");
        }
        Platform.setImplicitExit(false);
        started = true;
    }

    static <T> T ask(Supplier<T> work) {
        AtomicReference<T> answer = new AtomicReference<>();
        AtomicReference<RuntimeException> failed = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                answer.set(work.get());
            } catch (RuntimeException problem) {
                failed.set(problem);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the toolkit never came back");
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(stopped);
        }
        if (failed.get() != null) {
            throw failed.get();
        }
        return answer.get();
    }

    static void run(Runnable work) {
        ask(() -> {
            work.run();
            return null;
        });
    }

    /** A window holding the view, because a WebView outside a scene never lays out. */
    static Stage show(TerminalView view, int width, int height) {
        return ask(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(view, width, height));
            stage.show();
            return stage;
        });
    }

    static void waitUntil(String what, Supplier<Boolean> condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(ask(condition))) {
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException(what + " never happened");
    }

    private Fx() {
    }
}
