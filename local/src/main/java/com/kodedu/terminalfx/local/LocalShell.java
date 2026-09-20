package com.kodedu.terminalfx.local;

import com.kodedu.terminalfx.TerminalConnection;
import com.kodedu.terminalfx.TerminalExit;
import com.kodedu.terminalfx.TerminalSize;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A shell on this machine, as a {@link TerminalConnection}.
 *
 * <p>The only part of this library that needs a native library, and the reason it is a module of
 * its own: an embedder that connects to something else — a remote shell, a serial port, a fake —
 * depends on the core and never pulls pty4j or JNA in behind it.
 *
 * <p>What it starts, where, and with which environment is {@link LocalShellSpec}'s; nothing here
 * reads a settings file or knows what an application is.
 */
public final class LocalShell implements TerminalConnection {

    private final LocalShellSpec spec;
    private final Executor blocking;
    private final AtomicBoolean closed = new AtomicBoolean();

    private volatile PtyProcess process;
    private volatile OutputStream toShell;
    private volatile TerminalSize size;

    /**
     * @param spec     what to start
     * @param blocking where the two reader loops run. The embedder's pool: this class starts no
     *                 threads of its own
     */
    public LocalShell(LocalShellSpec spec, Executor blocking) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.blocking = Objects.requireNonNull(blocking, "blocking");
    }

    /** The process, once it is running — for an embedder that wants to know it is alive. */
    public PtyProcess process() {
        return process;
    }

    @Override
    public void open(Wiring wiring) throws Exception {
        Map<String, String> environment = new LinkedHashMap<>(System.getenv());
        environment.put("TERM", spec.term());
        environment.putAll(spec.environment());

        PtyProcessBuilder builder = new PtyProcessBuilder(spec.command())
                .setEnvironment(environment)
                .setConsole(false)
                .setUseWinConPty(true);
        Path directory = spec.directory();
        if (directory != null && Files.isDirectory(directory)) {
            builder.setDirectory(directory.toString());
        }
        TerminalSize wanted = size;
        if (wanted != null) {
            builder.setInitialColumns(wanted.columns());
            builder.setInitialRows(wanted.rows());
        }

        PtyProcess started = builder.start();
        this.process = started;
        this.toShell = started.getOutputStream();
        if (closed.get()) {
            // Closed while the process was starting: do not leave one running.
            started.destroy();
            return;
        }
        applySize();
        wiring.ready();

        blocking.execute(() -> pump(started.getInputStream(), wiring));
        blocking.execute(() -> {
            try {
                int status = started.waitFor();
                wiring.ended(closed.get()
                        ? new TerminalExit.ClosedByUser()
                        : new TerminalExit.Finished(status));
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
                wiring.ended(new TerminalExit.ClosedByUser());
            }
        });
    }

    private void pump(InputStream from, Wiring wiring) {
        byte[] buffer = new byte[8 * 1024];
        try {
            while (true) {
                int read = from.read(buffer);
                if (read < 0) {
                    return;
                }
                wiring.output(buffer, 0, read);
            }
        } catch (IOException broken) {
            if (!closed.get()) {
                wiring.ended(new TerminalExit.Failed(broken));
            }
        }
    }

    @Override
    public void send(byte[] bytes) throws IOException {
        OutputStream out = toShell;
        if (out == null || closed.get()) {
            return;
        }
        out.write(bytes);
        out.flush();
    }

    @Override
    public void resize(TerminalSize size) {
        this.size = size;
        applySize();
    }

    private void applySize() {
        PtyProcess running = process;
        TerminalSize wanted = size;
        if (running == null || wanted == null) {
            return;
        }
        try {
            running.setWinSize(new WinSize(wanted.columns(), wanted.rows()));
        } catch (RuntimeException cannot) {
            // A shell that has already ended cannot be resized, and that is not an error the
            // person typing needs to hear about.
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        PtyProcess running = process;
        if (running != null) {
            running.destroy();
        }
        OutputStream out = toShell;
        if (out != null) {
            try {
                out.close();
            } catch (IOException closing) {
                // Nothing left to do about a stream that will not shut.
            }
        }
    }
}
