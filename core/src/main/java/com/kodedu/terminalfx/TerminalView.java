package com.kodedu.terminalfx;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The terminal on the screen: a {@link WebView} with xterm.js in it and nothing else.
 *
 * <p>No process, no socket, no settings store. What it draws comes from {@link #write}, what is
 * typed into it goes to {@link #onInput}, and how big it is goes to {@link #onResize}. A
 * {@link TerminalSession} is what usually joins those to a {@link TerminalConnection}; this on
 * its own is enough for a page of read-only output.
 *
 * <h2>Where the renderer comes from</h2>
 *
 * <p>xterm.js, its stylesheet and three addons are read out of this jar and handed to the engine
 * as one document. Nothing is written to a temporary directory — the version this replaces
 * unpacked seven hundred kilobytes into {@code /tmp} on the first terminal of every process and
 * removed it in a shutdown hook, which is a file on disk with somebody's screen in it and a
 * shutdown hook that may not run.
 *
 * <h2>Threads</h2>
 *
 * <p>Every method here is for the JavaFX application thread, which is where a JavaFX control
 * belongs. {@link #whenReady} is the one exception and can be asked from anywhere.
 */
public final class TerminalView extends Region implements TerminalScreen {

    private final WebView web = new WebView();
    private final TerminalBridge bridge;
    private final List<Consumer<String>> input = new ArrayList<>();
    private final List<Consumer<TerminalSize>> resized = new ArrayList<>();
    private final List<Runnable> bells = new ArrayList<>();
    private final List<Consumer<String>> titles = new ArrayList<>();
    private final List<Consumer<String>> copied = new ArrayList<>();
    private final List<Runnable> pastes = new ArrayList<>();
    private final List<Consumer<String>> remoteClipboard = new ArrayList<>();
    private final List<Runnable> whenReady = new ArrayList<>();

    /**
     * What was written before the page could draw it, waiting.
     *
     * <p>The UI thread's, like everything else that touches the renderer: {@link #write} is the
     * embedder's own thread only in the sense that a session marshals onto this one first.
     */
    private final StringBuilder beforeReady = new StringBuilder();

    /** How much of that is kept. Enough for a greeting and a refusal, not for a build log. */
    private static final int WAITING_ROOM = 64 * 1024;

    private volatile boolean ready;
    private volatile TerminalSize size = new TerminalSize(80, 24);
    private TerminalLook look;
    private KeyClaim keyClaim = (key, control, alt, shift, meta) -> false;

    /** Whether a keystroke belongs to the embedder rather than to the shell. */
    @FunctionalInterface
    public interface KeyClaim {
        boolean claims(String key, boolean control, boolean alt, boolean shift, boolean meta);
    }

    public TerminalView(TerminalLook look) {
        this.look = Objects.requireNonNull(look, "look");
        this.bridge = new TerminalBridge(new Listening());
        getChildren().add(web);
        web.setContextMenuEnabled(false);
        WebEngine engine = web.getEngine();
        engine.getLoadWorker().stateProperty().addListener((property, was, now) -> {
            if (now == Worker.State.SUCCEEDED) {
                window().setMember("bridge", bridge);
                call("tfxStart", json(this.look));
            }
        });
        engine.loadContent(page());
        // Sizing happens in layoutChildren, once the box has a size.
    }

    private double laidOutWidth;
    private double laidOutHeight;

    /**
     * The renderer does not size itself to its box; it is told, and it has to be told <em>after</em>
     * the box has one. Asking before the first layout pass is how a terminal ends up two columns
     * wide, which is what the first version of this did.
     */
    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        web.resizeRelocate(0, 0, width, height);
        if (width != laidOutWidth || height != laidOutHeight) {
            laidOutWidth = width;
            laidOutHeight = height;
            if (ready) {
                Platform.runLater(this::fit);
            }
        }
    }

    /** Whether the page has loaded and the terminal exists. */
    @Override
    public boolean isReady() {
        return ready;
    }

    /**
     * Runs the action once the renderer is up, now if it already is.
     *
     * <p>The one method here that may be called from another thread: an embedder waiting for two
     * things to be ready is usually not on the UI thread while it waits.
     */
    @Override
    public void whenReady(Runnable action) {
        Objects.requireNonNull(action, "action");
        Platform.runLater(() -> {
            if (ready) {
                action.run();
            } else {
                whenReady.add(action);
            }
        });
    }

    /**
     * Draws the text. The renderer takes it as output, never as script.
     *
     * <p>Text that arrives before the page has loaded is kept and drawn when it has. A terminal
     * is opened and connected in the same breath, and the far end can be finished with it before
     * a WebView has parsed its own document — a server that refuses the shell, a container that
     * exits, a connection that drops during the handshake. Dropping those bytes means the one
     * screen that would have said what happened comes up empty.
     *
     * <p>Bounded, because a page that never loads must not become a heap of output nobody will
     * ever see. What is kept is the beginning rather than the end: the first thing a shell says
     * is the greeting or the refusal, and that is what somebody needs.
     */
    @Override
    public void write(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!ready) {
            if (beforeReady.length() < WAITING_ROOM) {
                beforeReady.append(text);
            }
            return;
        }
        call("tfxWrite", text);
    }

    /** Whether everything handed to {@link #write} has been drawn. */
    public boolean isIdle() {
        return !ready || Boolean.TRUE.equals(script("window.tfxIdle()"));
    }

    /** Dresses the terminal again, live. */
    public void look(TerminalLook look) {
        this.look = Objects.requireNonNull(look, "look");
        if (ready) {
            call("tfxLook", json(look));
        }
    }

    public TerminalLook look() {
        return look;
    }

    /** How many characters fit right now. */
    @Override
    public TerminalSize size() {
        return size;
    }

    @Override
    public Runnable onInput(Consumer<String> listener) {
        input.add(Objects.requireNonNull(listener));
        return () -> input.remove(listener);
    }

    @Override
    public Runnable onResize(Consumer<TerminalSize> listener) {
        resized.add(Objects.requireNonNull(listener));
        return () -> resized.remove(listener);
    }

    public void onBell(Runnable listener) {
        bells.add(Objects.requireNonNull(listener));
    }

    /** The far end set a title. Untrusted text: show it, do not run it. */
    public void onTitle(Consumer<String> listener) {
        titles.add(Objects.requireNonNull(listener));
    }

    /** Text the terminal wants on the clipboard, because the person selected or copied it. */
    public void onCopy(Consumer<String> listener) {
        copied.add(Objects.requireNonNull(listener));
    }

    /** The person asked to paste; the embedder decides what, and whether to ask first. */
    public void onPasteRequested(Runnable listener) {
        pastes.add(Objects.requireNonNull(listener));
    }

    /**
     * The far end asked to write the clipboard (OSC 52). Only ever called when the look allows
     * it, which it does not by default.
     */
    public void onRemoteClipboard(Consumer<String> listener) {
        remoteClipboard.add(Objects.requireNonNull(listener));
    }

    /** Which keystrokes the embedder takes before the shell sees them. */
    public void keysClaimedBy(KeyClaim claim) {
        this.keyClaim = Objects.requireNonNull(claim);
    }

    public void focusTerminal() {
        web.requestFocus();
        if (ready) {
            script("window.tfxFocus()");
        }
    }

    public String selection() {
        return ready ? String.valueOf(script("window.tfxSelection()")) : "";
    }

    /** Selects everything in the buffer, scrollback included; Copy then takes all of it. */
    public void selectAll() {
        if (ready) {
            script("window.tfxSelectAll()");
        }
    }

    public void paste(String text) {
        if (ready && text != null && !text.isEmpty()) {
            call("tfxPaste", text);
        }
    }

    /** @return whether something was found */
    public boolean find(String text, boolean forward) {
        if (!ready || text == null || text.isEmpty()) {
            return false;
        }
        Object found = window().call("tfxFind", text, forward);
        return Boolean.TRUE.equals(found);
    }

    /**
     * Kept for embedders that call it when they close their find bar. Find leaves nothing behind
     * but its answer, selected, and that stays so it can be copied.
     */
    public void clearFind() {
    }

    public void clear() {
        if (ready) {
            script("window.tfxClear()");
        }
    }

    public void scrollToBottom() {
        if (ready) {
            script("window.tfxScrollToBottom()");
        }
    }

    /** How many lines are kept above the screen. For a test and for a memory budget. */
    public int scrollbackLines() {
        return ready ? ((Number) script("window.tfxScrollbackLines()")).intValue() : 0;
    }

    /**
     * Whether the program at the far end has asked for application cursor keys.
     *
     * <p>The one mode an embedder has to know about, and only because of one thing: writing key
     * sequences itself rather than through the terminal. An editor running at the far end turns
     * this on and a shell sitting at its prompt does not, and Up is {@code ESC O A} in the first
     * and {@code ESC [ A} in the second.
     */
    public boolean applicationCursorKeys() {
        return modes().charAt(0) == '1';
    }

    /**
     * Whether the far end has asked for bracketed paste.
     *
     * <p>Not needed to paste — {@link #paste(String)} asks the renderer, which knows its own
     * mode — but an embedder deciding what to warn about may want to know.
     */
    public boolean bracketedPaste() {
        return modes().charAt(1) == '1';
    }

    private String modes() {
        if (!ready) {
            return "00";
        }
        Object said = script("window.tfxModes()");
        String flags = said == null ? "00" : String.valueOf(said);
        return flags.length() < 2 ? "00" : flags;
    }

    /** What is on the screen, as text. For tests; not a public contract for embedders. */
    public String screenText() {
        return ready ? String.valueOf(script("window.tfxScreenText()")) : "";
    }

    /** The engine, for the library's own tests. Not part of what an embedder is offered. */
    javafx.scene.web.WebEngine getEngineForTest() {
        return web.getEngine();
    }

    private void fit() {
        if (ready) {
            script("window.tfxFit()");
        }
    }

    private JSObject window() {
        return (JSObject) web.getEngine().executeScript("window");
    }

    /**
     * Calls a function with the value as an argument rather than pasting the value into a script.
     * A megabyte of somebody's output is not JavaScript and must never be parsed as any.
     */
    private void call(String function, Object argument) {
        window().call(function, argument);
    }

    private Object script(String javascript) {
        return web.getEngine().executeScript(javascript);
    }

    private final class Listening implements TerminalBridge.Listener {

        @Override
        public void ready() {
            ready = true;
            fit();
            if (!beforeReady.isEmpty()) {
                String waiting = beforeReady.toString();
                beforeReady.setLength(0);
                call("tfxWrite", waiting);
            }
            List.copyOf(whenReady).forEach(Runnable::run);
            whenReady.clear();
            // The first layout pass may have happened before the page loaded, in which case
            // nothing will ask again: fit once more when this one has settled.
            Platform.runLater(TerminalView.this::fit);
        }

        @Override
        public void input(String text) {
            input.forEach(listener -> listener.accept(text));
        }

        @Override
        public void resized(int columns, int rows) {
            if (columns < 1 || rows < 1) {
                return;
            }
            size = new TerminalSize(columns, rows);
            resized.forEach(listener -> listener.accept(size));
        }

        @Override
        public void bell() {
            bells.forEach(Runnable::run);
        }

        @Override
        public void title(String title) {
            titles.forEach(listener -> listener.accept(title));
        }

        @Override
        public void selected(String text) {
            copied.forEach(listener -> listener.accept(text));
        }

        @Override
        public void pasteRequested() {
            pastes.forEach(Runnable::run);
        }

        @Override
        public void remoteClipboard(String payload) {
            remoteClipboard.forEach(listener -> listener.accept(payload));
        }

        @Override
        public boolean claimsKey(String key, boolean control, boolean alt, boolean shift,
                boolean meta) {
            return keyClaim.claims(key, control, alt, shift, meta);
        }
    }

    private String page() {
        return read("terminal.html")
                .replace("/*XTERM_CSS*/", read("xterm.css"))
                .replace("/*XTERM_JS*/", read("xterm.js"))
                .replace("/*FIT_JS*/", read("addon-fit.js"))
                .replace("/*UNICODE_JS*/", read("addon-unicode11.js"));
    }

    private static String read(String name) {
        try (InputStream in = TerminalView.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException(name + " is not in this jar");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new IllegalStateException("could not read " + name, unreadable);
        }
    }

    /** The look as the page reads it. Written by hand: one object, no JSON library. */
    private static String json(TerminalLook look) {
        return "{"
                + "\"fontFamily\":" + quote(look.fontFamily())
                + ",\"fontSize\":" + look.fontSize()
                + ",\"background\":" + quote(css(look.background()))
                + ",\"foreground\":" + quote(css(look.foreground()))
                + ",\"cursor\":" + quote(css(look.cursor()))
                + ",\"cursorBlink\":" + look.cursorBlink()
                + ",\"scrollback\":" + look.scrollback()
                + ",\"scrollMultiplier\":" + look.scrollMultiplier()
                + ",\"copyOnSelect\":" + look.copyOnSelect()
                + ",\"clearSelectionAfterCopy\":" + look.clearSelectionAfterCopy()
                + ",\"ctrlCCopies\":" + look.ctrlCCopies()
                + ",\"ctrlVPastes\":" + look.ctrlVPastes()
                + ",\"allowRemoteClipboard\":" + look.allowRemoteClipboard()
                + ",\"userCss\":" + quote(look.userCss())
                + "}";
    }

    private static String css(Color colour) {
        return String.format("#%02X%02X%02X",
                Math.round(colour.getRed() * 255),
                Math.round(colour.getGreen() * 255),
                Math.round(colour.getBlue() * 255));
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
