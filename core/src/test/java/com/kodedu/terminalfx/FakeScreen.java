package com.kodedu.terminalfx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A terminal that draws into a StringBuilder. Twelve lines, which was the point of the seam. */
final class FakeScreen implements TerminalScreen {

    private final List<Consumer<String>> input = new ArrayList<>();
    private final List<Consumer<TerminalSize>> resized = new ArrayList<>();
    private final List<Runnable> waiting = new ArrayList<>();
    private final StringBuilder drawn = new StringBuilder();
    private TerminalSize size = new TerminalSize(80, 24);
    private boolean ready;

    @Override
    public void whenReady(Runnable action) {
        if (ready) {
            action.run();
        } else {
            waiting.add(action);
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void write(String text) {
        drawn.append(text);
    }

    @Override
    public TerminalSize size() {
        return size;
    }

    @Override
    public void onInput(Consumer<String> listener) {
        input.add(listener);
    }

    @Override
    public void onResize(Consumer<TerminalSize> listener) {
        resized.add(listener);
    }

    void becomeReady() {
        ready = true;
        List.copyOf(waiting).forEach(Runnable::run);
        waiting.clear();
    }

    void type(String text) {
        List.copyOf(input).forEach(listener -> listener.accept(text));
    }

    void resizeTo(TerminalSize next) {
        size = next;
        List.copyOf(resized).forEach(listener -> listener.accept(next));
    }

    String drawn() {
        return drawn.toString();
    }
}
