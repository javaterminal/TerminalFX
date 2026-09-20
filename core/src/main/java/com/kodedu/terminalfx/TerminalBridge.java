package com.kodedu.terminalfx;

/**
 * What the page calls. Public because the WebView's JavaScript engine will only reach a public
 * class's public methods, and held by {@link TerminalView} because a bridge that is collected is
 * a terminal that stops answering.
 *
 * <p>Everything arriving here came from the page, and most of it came from the far end before
 * that: a title, a link, a clipboard request. None of it is executed; it is handed to the
 * embedder as data.
 */
public final class TerminalBridge {

    /** What the page asks of the embedder. Set once, by {@link TerminalView}. */
    public interface Listener {
        void ready();

        void input(String text);

        void resized(int columns, int rows);

        void bell();

        void title(String title);

        void selected(String text);

        void pasteRequested();

        void remoteClipboard(String payload);

        boolean claimsKey(String key, boolean control, boolean alt, boolean shift, boolean meta);
    }

    private final Listener listener;

    TerminalBridge(Listener listener) {
        this.listener = listener;
    }

    public void ready() {
        listener.ready();
    }

    public void input(String text) {
        listener.input(text);
    }

    /** Bytes the page sends as a string of code points 0–255, which is what xterm.js gives. */
    public void inputBinary(String text) {
        listener.input(text);
    }

    public void resized(int columns, int rows) {
        listener.resized(columns, rows);
    }

    public void bell() {
        listener.bell();
    }

    public void title(String title) {
        listener.title(title);
    }

    public void selected(String text) {
        listener.selected(text);
    }

    public void pasteRequested() {
        listener.pasteRequested();
    }

    public void remoteClipboard(String payload) {
        listener.remoteClipboard(payload);
    }

    public boolean claimsKey(String key, boolean control, boolean alt, boolean shift,
            boolean meta) {
        return listener.claimsKey(key, control, alt, shift, meta);
    }
}
