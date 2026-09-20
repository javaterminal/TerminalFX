package com.kodedu.terminalfx.local;

import com.kodedu.terminalfx.TerminalConnection;
import com.kodedu.terminalfx.TerminalExit;
import com.kodedu.terminalfx.TerminalSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A real shell, through the connection contract. No window: this is the transport half, and what
 * it has to get right is the process, the size and the ending.
 */
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "the Windows shell is verified on its runner")
@DisplayName("a shell on this machine")
class LocalShellTest {

    private ExecutorService workers;

    @BeforeEach
    void pool() {
        workers = Executors.newCachedThreadPool();
    }

    @AfterEach
    void endThePool() {
        workers.shutdownNow();
    }

    /** Collects what the shell printed, and the reason it stopped. */
    private static final class Heard implements TerminalConnection.Wiring {
        final StringBuilder text = new StringBuilder();
        final CountDownLatch up = new CountDownLatch(1);
        final CountDownLatch over = new CountDownLatch(1);
        final AtomicReference<TerminalExit> exit = new AtomicReference<>();

        @Override
        public void ready() {
            up.countDown();
        }

        @Override
        public void output(byte[] bytes, int offset, int length) {
            synchronized (text) {
                text.append(new String(bytes, offset, length, StandardCharsets.UTF_8));
            }
        }

        @Override
        public void ended(TerminalExit how) {
            exit.compareAndSet(null, how);
            over.countDown();
        }

        String said() {
            synchronized (text) {
                return text.toString();
            }
        }

        void waitForText(String wanted) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (System.nanoTime() < deadline) {
                if (said().contains(wanted)) {
                    return;
                }
                Thread.sleep(25);
            }
            throw new AssertionError("the shell never said " + wanted + "; it said: " + said());
        }
    }

    @Test
    @DisplayName("runs a command and prints what it printed")
    void runsACommand() throws Exception {
        LocalShell shell = new LocalShell(spec(), workers);
        Heard heard = new Heard();
        shell.resize(new TerminalSize(100, 30));
        shell.open(heard);
        assertTrue(heard.up.await(20, TimeUnit.SECONDS));

        shell.send("echo terminalfx-was-here\n".getBytes(StandardCharsets.UTF_8));
        heard.waitForText("terminalfx-was-here");
        shell.close();
    }

    @Test
    @DisplayName("starts where it was told to")
    void startsInADirectory() throws Exception {
        Path where = Files.createTempDirectory("terminalfx-cwd").toRealPath();
        LocalShell shell = new LocalShell(spec().in(where), workers);
        Heard heard = new Heard();
        shell.open(heard);
        assertTrue(heard.up.await(20, TimeUnit.SECONDS));
        shell.send("pwd\n".getBytes(StandardCharsets.UTF_8));
        heard.waitForText(where.toString());
        shell.close();
    }

    @Test
    @DisplayName("gives the shell the size it was told, and a new one when it changes")
    void sizeReachesTheShell() throws Exception {
        LocalShell shell = new LocalShell(spec(), workers);
        Heard heard = new Heard();
        shell.resize(new TerminalSize(101, 31));
        shell.open(heard);
        assertTrue(heard.up.await(20, TimeUnit.SECONDS));
        shell.send("stty size\n".getBytes(StandardCharsets.UTF_8));
        heard.waitForText("31 101");

        shell.resize(new TerminalSize(77, 25));
        shell.send("stty size\n".getBytes(StandardCharsets.UTF_8));
        heard.waitForText("25 77");
        shell.close();
    }

    @Test
    @DisplayName("says which status the shell ended with")
    void exitStatus() throws Exception {
        LocalShell shell = new LocalShell(spec(), workers);
        Heard heard = new Heard();
        shell.open(heard);
        assertTrue(heard.up.await(20, TimeUnit.SECONDS));
        shell.send("exit 7\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(heard.over.await(20, TimeUnit.SECONDS), "the shell never ended");
        TerminalExit how = heard.exit.get();
        assertInstanceOf(TerminalExit.Finished.class, how);
        assertEquals(7, ((TerminalExit.Finished) how).status());
        shell.close();
    }

    @Test
    @DisplayName("a close is a close, not a failure, and doing it twice is still one")
    void closing() throws Exception {
        LocalShell shell = new LocalShell(spec(), workers);
        Heard heard = new Heard();
        shell.open(heard);
        assertTrue(heard.up.await(20, TimeUnit.SECONDS));
        shell.close();
        shell.close();
        assertTrue(heard.over.await(20, TimeUnit.SECONDS));
        assertInstanceOf(TerminalExit.ClosedByUser.class, heard.exit.get());
    }

    @Test
    @DisplayName("passes an environment of its own on to the shell")
    void environment() throws Exception {
        LocalShell shell = new LocalShell(new LocalShellSpec(new String[] { "/bin/sh", "-i" },
                null, Map.of("TERMINALFX_TEST", "kodedu"), "xterm-256color"), workers);
        Heard heard = new Heard();
        shell.open(heard);
        assertTrue(heard.up.await(20, TimeUnit.SECONDS));
        shell.send("echo $TERMINALFX_TEST-$TERM\n".getBytes(StandardCharsets.UTF_8));
        heard.waitForText("kodedu-xterm-256color");
        shell.close();
    }

    private static LocalShellSpec spec() {
        return new LocalShellSpec(new String[] { "/bin/sh", "-i" }, null, Map.of(),
                "xterm-256color");
    }
}
