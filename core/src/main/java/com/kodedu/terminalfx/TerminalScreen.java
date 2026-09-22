package com.kodedu.terminalfx;

import java.util.function.Consumer;

/**
 * What a {@link TerminalSession} needs from whatever is drawing: text in, keystrokes and a size
 * out, and a way to know when it is there.
 *
 * <p>{@link TerminalView} is the one this library ships. The interface exists because the
 * ordering rules in {@link TerminalSession} — two readinesses, one writer, coalesced resizes,
 * a close that cannot be revived — are the part that is worth testing, and testing them should
 * not require a browser engine. A fake is a dozen lines.
 *
 * <p>Every method is called on the UI thread except {@link #whenReady}.
 */
public interface TerminalScreen {

    /** Runs the action once the renderer exists; from any thread. */
    void whenReady(Runnable action);

    boolean isReady();

    /** Draws output. Never parsed as script. */
    void write(String text);

    /** How many characters fit. */
    TerminalSize size();

    /** What was typed. */
    /**
     * Tells this listener what is typed.
     *
     * @return how to stop listening. A session that has been replaced — a terminal that
     *         reconnected into the same tab — must stop being told, or every reconnect leaves
     *         one more dead listener on a screen that may live all day
     */
    Runnable onInput(Consumer<String> listener);

    /** The screen changed size — which is what the far end has to be told. */
    /** As {@link #onInput}, for the size. */
    Runnable onResize(Consumer<TerminalSize> listener);
}
