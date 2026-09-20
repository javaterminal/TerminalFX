package com.kodedu.terminalfx;

import javafx.application.Platform;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * One terminal, joined to one connection, for as long as both live.
 *
 * <p>This is where every ordering rule of the library is kept, and it is the reason the view and
 * the connection can both be simple:
 *
 * <ul>
 *   <li><b>Two readinesses, one result.</b> The renderer comes up and the connection comes up, in
 *       either order. {@link #start()} completes when both have, fails if the connection could
 *       not, and completes exceptionally if the session was closed while starting. It always
 *       completes.</li>
 *   <li><b>One writer.</b> Everything typed goes through a single queue drained by a single
 *       task, so bytes reach the far end in the order they were produced, and a slow connection
 *       makes the queue fill rather than the window freeze.</li>
 *   <li><b>Bounded in both directions.</b> The input queue has a size; when it is full the
 *       oldest unsent keystrokes are <em>not</em> dropped — the caller is told. Output is limited
 *       by how many chunks may be waiting for the UI thread at once, so a program printing as
 *       fast as it can slows down rather than filling the heap.</li>
 *   <li><b>Resizes coalesce.</b> Only the last size matters, and it is sent on the writer, so it
 *       cannot interleave with a half-written keystroke.</li>
 *   <li><b>Close is idempotent and works before the start.</b> A callback that arrives after it
 *       cannot revive the session, and an old connection cannot write into a new one, because
 *       the wiring it holds belongs to this session and checks.</li>
 * </ul>
 */
public final class TerminalSession implements AutoCloseable {

    /** How many keystrokes may be waiting to be sent before the caller is told to stop. */
    private static final int INPUT_QUEUE = 512;

    /** How many chunks of output may be waiting for the UI thread at once. */
    private static final int OUTPUT_CHUNKS = 8;

    private final TerminalScreen view;
    private final TerminalConnection connection;
    private final Executor blocking;
    private final Executor ui;

    private final BlockingQueue<Object> outgoing = new ArrayBlockingQueue<>(INPUT_QUEUE);
    private final AtomicReference<TerminalSize> pendingResize = new AtomicReference<>();
    private final Semaphore drawing = new Semaphore(OUTPUT_CHUNKS);
    private final Utf8Stream incoming = new Utf8Stream();

    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final AtomicBoolean transportReady = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private final CompletableFuture<Void> ready = new CompletableFuture<>();
    private final List<Consumer<TerminalExit>> exits = new ArrayList<>();
    private final AtomicReference<TerminalExit> exit = new AtomicReference<>();

    /** Put on the queue to wake the writer for a resize; the size itself is coalesced apart. */
    private static final Object RESIZE = new Object();

    /** Put on the queue to end the writer. */
    private static final Object STOP = new Object();

    /**
     * @param view       the terminal on the screen
     * @param connection what it is connected to
     * @param blocking   where work that may block is run — the embedder's pool, never a thread
     *                   this library starts behind its back. Two tasks are taken for the life of
     *                   the session: one opens the connection, one writes to it
     */
    public TerminalSession(TerminalScreen view, TerminalConnection connection, Executor blocking) {
        this(view, connection, blocking, Platform::runLater);
    }

    /**
     * As above, with the way onto the UI thread named.
     *
     * <p>An application with one of its own — and every application this library was written for
     * has one — hands it over here rather than having this library reach for
     * {@code Platform.runLater} behind its back. It is also what lets the ordering rules be
     * tested without a toolkit.
     */
    public TerminalSession(TerminalScreen view, TerminalConnection connection, Executor blocking,
            Executor ui) {
        this.view = Objects.requireNonNull(view, "view");
        this.connection = Objects.requireNonNull(connection, "connection");
        this.blocking = Objects.requireNonNull(blocking, "blocking");
        this.ui = Objects.requireNonNull(ui, "ui");

        view.onInput(this::send);
        view.onResize(size -> {
            pendingResize.set(size);
            if (transportReady.get()) {
                outgoing.offer(RESIZE);
            }
        });
    }

    /** What is drawing. The node itself is the embedder's; it made it. */
    public TerminalScreen screen() {
        return view;
    }

    /**
     * Opens the connection and, when the renderer is up too, completes.
     *
     * <p>Calling it twice returns the same result rather than opening a second connection.
     */
    public CompletableFuture<Void> start() {
        if (!started.compareAndSet(false, true)) {
            return ready;
        }
        if (closed.get()) {
            ready.completeExceptionally(new IllegalStateException("closed before it started"));
            return ready;
        }
        view.whenReady(this::bothReady);
        blocking.execute(() -> {
            try {
                connection.open(new Wired());
            } catch (Throwable failed) {
                ended(new TerminalExit.Failed(failed));
            }
        });
        return ready;
    }

    /**
     * Sends text as the person typed it.
     *
     * @return false when the queue is full, which means the far end is not reading; the caller
     *         decides what to tell whoever is typing. Nothing is dropped silently
     */
    public boolean send(String text) {
        if (text == null || text.isEmpty() || closed.get()) {
            return false;
        }
        return send(text.getBytes(StandardCharsets.UTF_8));
    }

    /** As {@link #send(String)}, for input that is already bytes. */
    public boolean send(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || closed.get()) {
            return false;
        }
        return outgoing.offer(bytes.clone());
    }

    /** Whether anything typed is still waiting to be sent. */
    public int queued() {
        return outgoing.size();
    }

    public boolean isClosed() {
        return closed.get();
    }

    /** How this terminal ended, once it has. */
    public TerminalExit exit() {
        return exit.get();
    }

    /**
     * Told once, with the first reason the session ended. Listeners added after it has already
     * ended are called straight away, so there is no race in which an embedder misses the end.
     */
    public void onExit(Consumer<TerminalExit> listener) {
        Objects.requireNonNull(listener);
        TerminalExit already = exit.get();
        if (already != null) {
            listener.accept(already);
            return;
        }
        synchronized (exits) {
            TerminalExit now = exit.get();
            if (now != null) {
                listener.accept(now);
            } else {
                exits.add(listener);
            }
        }
    }

    /**
     * Ends the session. Safe before {@link #start()}, safe twice, and safe from any thread.
     *
     * <p>What it does not do is wait: closing a connection can take as long as a socket takes,
     * and the window asking for it is not the place to find that out.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        outgoing.clear();
        outgoing.offer(STOP);
        blocking.execute(() -> {
            try {
                connection.close();
            } finally {
                ended(new TerminalExit.ClosedByUser());
            }
        });
        if (!ready.isDone()) {
            ready.completeExceptionally(new IllegalStateException("closed before it was ready"));
        }
    }

    private void bothReady() {
        if (closed.get() || !transportReady.get() || ready.isDone()) {
            return;
        }
        // The first size the far end hears is the one the renderer worked out for its box.
        pendingResize.set(view.size());
        outgoing.offer(RESIZE);
        ready.complete(null);
    }

    private void ended(TerminalExit how) {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        exit.set(how);
        outgoing.offer(STOP);
        if (!ready.isDone()) {
            if (how instanceof TerminalExit.Failed failed) {
                ready.completeExceptionally(failed.cause());
            } else {
                ready.completeExceptionally(new IllegalStateException("ended: " + how));
            }
        }
        List<Consumer<TerminalExit>> told;
        synchronized (exits) {
            told = List.copyOf(exits);
            exits.clear();
        }
        ui.execute(() -> told.forEach(listener -> listener.accept(how)));
    }

    /** The one place the connection is written to, and the reason input keeps its order. */
    private void writeLoop() {
        try {
            while (true) {
                Object next = outgoing.take();
                if (next == STOP) {
                    return;
                }
                if (next == RESIZE) {
                    TerminalSize size = pendingResize.getAndSet(null);
                    if (size != null) {
                        connection.resize(size);
                    }
                    continue;
                }
                connection.send((byte[]) next);
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        } catch (Throwable broken) {
            ended(new TerminalExit.Failed(broken));
        }
    }

    /**
     * What this session's connection says back. It belongs to this session and checks: a
     * connection left over from before a reconnect cannot draw into the terminal that replaced
     * it, because its wiring is this one and this one is closed.
     */
    private final class Wired implements TerminalConnection.Wiring {

        @Override
        public void ready() {
            if (closed.get() || !transportReady.compareAndSet(false, true)) {
                return;
            }
            blocking.execute(TerminalSession.this::writeLoop);
            ui.execute(TerminalSession.this::bothReady);
        }

        @Override
        public void output(byte[] bytes, int offset, int length) {
            if (closed.get() || length <= 0) {
                return;
            }
            String text = incoming.decode(bytes, offset, length);
            if (text.isEmpty()) {
                return;
            }
            try {
                // The one piece of back pressure that matters: a program printing faster than the
                // window can draw makes this wait rather than making the heap grow.
                if (!drawing.tryAcquire(30, TimeUnit.SECONDS)) {
                    return;
                }
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
                return;
            }
            ui.execute(() -> {
                try {
                    if (!closed.get()) {
                        view.write(text);
                    }
                } finally {
                    drawing.release();
                }
            });
        }

        @Override
        public void ended(TerminalExit how) {
            TerminalSession.this.ended(how);
        }
    }
}
