package com.kodedu.terminalfx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a session promises, with nothing on the screen and nothing on a socket.
 *
 * <p>The UI executor is a queue the test drains by hand, so "the renderer became ready while the
 * connection was still opening" is a thing this test can arrange rather than hope for.
 */
@DisplayName("a terminal session")
class TerminalSessionTest {

    /** The UI thread, as a list of things waiting to happen on it. */
    private final Queue<Runnable> onUi = new ArrayDeque<>();

    /**
     * Not try-with-resources: {@code ExecutorService.close()} waits for every task, and one of
     * them is the session's writer, which waits for something to write. A test that failed
     * before closing its session would hang there instead of reporting.
     */
    private ExecutorService workers;

    @BeforeEach
    void pool() {
        workers = Executors.newCachedThreadPool();
    }

    @AfterEach
    void endThePool() {
        workers.shutdownNow();
    }

    /**
     * Starts, and drains the UI queue while waiting — because in this test this thread is the UI
     * thread, and a plain {@code start().get()} would be the session waiting for a thread that is
     * waiting for the session.
     */
    private void startAndWait(TerminalSession session) throws Exception {
        CompletableFuture<Void> ready = session.start();
        waitFor(ready::isDone);
        ready.get(5, TimeUnit.SECONDS);
    }

    private void drainUi() {
        while (true) {
            Runnable next;
            synchronized (onUi) {
                next = onUi.poll();
            }
            if (next == null) {
                return;
            }
            next.run();
        }
    }

    private void post(Runnable work) {
        synchronized (onUi) {
            onUi.add(work);
        }
    }

    private TerminalSession session(FakeScreen screen, FakeConnection connection) {
        return new TerminalSession(screen, connection, workers, this::post);
    }

    private void settle() throws Exception {
        for (int i = 0; i < 100; i++) {
            drainUi();
            Thread.sleep(10);
        }
    }

    @Test
    @DisplayName("is ready only when both the renderer and the connection are")
    void bothReadinesses() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);

            CompletableFuture<Void> ready = session.start();
            assertTrue(connection.opened().await(5, TimeUnit.SECONDS));
            drainUi();
            assertFalse(ready.isDone(), "the renderer has not come up yet");

            screen.becomeReady();
            // Drained until it lands, as the other order is: the connection says it is open
            // before it says it is ready, and a single drain could come between the two and
            // leave the ready on the UI queue with nobody to run it (timed out in a release run).
            for (int i = 0; i < 500 && !ready.isDone(); i++) {
                drainUi();
                Thread.sleep(10);
            }
            ready.get(5, TimeUnit.SECONDS);

            // The first thing the far end is told is how big the screen is.
            waitFor(() -> !connection.sizes().isEmpty());
            assertEquals(new TerminalSize(80, 24), connection.sizes().get(0));
            session.close();
        }
    }

    @Test
    @DisplayName("is ready when they arrive the other way round")
    void theOtherOrder() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);
            CompletableFuture<Void> ready = session.start();
            for (int i = 0; i < 100 && !ready.isDone(); i++) {
                drainUi();
                Thread.sleep(10);
            }
            ready.get(5, TimeUnit.SECONDS);
            session.close();
        }
    }

    @Test
    @DisplayName("fails once, and says why, when the connection will not open")
    void refusedConnection() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            connection.refuseToOpen(new IOException("no route to host"));
            TerminalSession session = session(screen, connection);
            List<TerminalExit> ends = new ArrayList<>();
            session.onExit(ends::add);

            CompletableFuture<Void> ready = session.start();
            ExecutionException failed = assertThrows(ExecutionException.class,
                    () -> ready.get(5, TimeUnit.SECONDS));
            assertEquals("no route to host", failed.getCause().getMessage());

            settle();
            assertEquals(1, ends.size(), "told once");
            assertInstanceOf(TerminalExit.Failed.class, ends.get(0));
        }
    }

    @Test
    @DisplayName("closes before it has started, twice, and stays closed")
    void closeIsIdempotentAndWorksEarly() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);

            session.close();
            session.close();
            CompletableFuture<Void> ready = session.start();
            assertThrows(ExecutionException.class, () -> ready.get(5, TimeUnit.SECONDS));
            settle();
            assertTrue(session.isClosed());
            assertTrue(connection.closes() <= 2, "closed " + connection.closes() + " times");
        }
    }

    @Test
    @DisplayName("a callback that arrives after the close cannot draw into it")
    void lateCallbacksAreIgnored() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);
            startAndWait(session);
            drainUi();

            session.close();
            settle();
            byte[] late = "this arrived too late".getBytes(StandardCharsets.UTF_8);
            connection.wiring().output(late, 0, late.length);
            connection.wiring().ended(new TerminalExit.Finished(0));
            settle();

            assertEquals("", screen.drawn());
            assertInstanceOf(TerminalExit.ClosedByUser.class, session.exit());
        }
    }

    @Test
    @DisplayName("sends what was typed, in the order it was typed")
    void inputKeepsItsOrder() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);
            startAndWait(session);
            drainUi();

            for (int i = 0; i < 200; i++) {
                screen.type(String.valueOf((char) ('a' + i % 26)));
            }
            waitFor(() -> connection.sent().size() == 200);
            StringBuilder expected = new StringBuilder();
            for (int i = 0; i < 200; i++) {
                expected.append((char) ('a' + i % 26));
            }
            assertEquals(expected.toString(), String.join("", connection.sent()));
            session.close();
        }
    }

    @Test
    @DisplayName("says so rather than dropping a keystroke when the far end stops reading")
    void theQueueIsBoundedAndSaysSo() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            connection.readyOnOpen(false);
            TerminalSession session = session(screen, connection);
            session.start();
            assertTrue(connection.opened().await(5, TimeUnit.SECONDS));
            // The writer never starts, so everything typed piles up in the queue.
            boolean refused = false;
            for (int i = 0; i < 5000 && !refused; i++) {
                refused = !session.send("x");
            }
            assertTrue(refused, "the queue took 5000 keystrokes without a word");
            assertTrue(session.queued() > 0);
            session.close();
        }
    }

    @Test
    @DisplayName("sends the last size, not every size on the way to it")
    void resizesCoalesce() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);
            startAndWait(session);
            drainUi();
            waitFor(() -> !connection.sizes().isEmpty());

            for (int columns = 81; columns <= 160; columns++) {
                screen.resizeTo(new TerminalSize(columns, 24));
            }
            waitFor(() -> connection.sizes().contains(new TerminalSize(160, 24)));
            List<TerminalSize> seen = connection.sizes();
            assertEquals(new TerminalSize(160, 24), seen.get(seen.size() - 1));
            assertTrue(seen.size() < 82, "every intermediate size was sent: " + seen.size());
            session.close();
        }
    }

    @Test
    @DisplayName("draws a character that arrived in two pieces as one character")
    void utf8AcrossChunks() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);
            startAndWait(session);
            drainUi();

            byte[] text = "ğüş 日本 😀".getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < text.length; i++) {
                connection.wiring().output(text, i, 1);
            }
            waitFor(() -> {
                drainUi();
                return screen.drawn().contains("😀");
            });
            assertEquals("ğüş 日本 😀", screen.drawn());
            session.close();
        }
    }

    @Test
    @DisplayName("tells an exit from an end of stream from a failure")
    void theFourEndings() throws Exception {
        assertEnding(new TerminalExit.Finished(3), TerminalExit.Finished.class);
        assertEnding(new TerminalExit.EndOfStream(), TerminalExit.EndOfStream.class);
        assertEnding(new TerminalExit.Failed(new IOException("broken pipe")),
                TerminalExit.Failed.class);
    }

    private void assertEnding(TerminalExit how, Class<?> expected) throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);
            startAndWait(session);
            drainUi();
            AtomicReference<TerminalExit> told = new AtomicReference<>();
            session.onExit(told::set);

            connection.wiring().ended(how);
            connection.wiring().ended(new TerminalExit.ClosedByUser());
            waitFor(() -> {
                drainUi();
                return told.get() != null;
            });
            assertInstanceOf(expected, told.get(), "the first ending is the one that counts");
            session.close();
        }
    }

    @Test
    @DisplayName("a listener added after the end is still told")
    void exitAfterTheFact() throws Exception {
        {
            FakeScreen screen = new FakeScreen();
            screen.becomeReady();
            FakeConnection connection = new FakeConnection();
            TerminalSession session = session(screen, connection);
            startAndWait(session);
            drainUi();
            connection.wiring().ended(new TerminalExit.Finished(0));
            waitFor(() -> session.exit() != null);

            AtomicReference<TerminalExit> told = new AtomicReference<>();
            session.onExit(told::set);
            assertInstanceOf(TerminalExit.Finished.class, told.get());
        }
    }

    private void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            drainUi();
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(5);
        }
        drainUi();
        if (!condition.getAsBoolean()) {
            throw new TimeoutException("the condition never came true");
        }
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    @Test
    @DisplayName("a session that has closed stops being told what is typed")
    void aClosedSessionLetsGoOfTheScreen() {
        // An embedder that reconnects builds a second session over the same screen. The first one's
        // listeners used to stay on it, so a terminal somebody worked in all day collected one
        // dead listener per reconnect -- and every one of them was still being handed every
        // keystroke.
        FakeScreen screen = new FakeScreen();
        FakeConnection connection = new FakeConnection();
        TerminalSession session = session(screen, connection);
        int listening = screen.listeners();
        assertTrue(listening >= 2, "it listens for input and for size");

        session.close();

        assertEquals(0, screen.listeners(),
                "and gives both back, so the next session over this screen is the only one");
    }
}