/**
 * The terminal itself: what draws, what a connection has to do, and the session that joins them.
 *
 * <p>No PTY, no native library, no application. {@code com.kodedu.terminalfx.local} is a separate
 * module for the one case that needs those.
 */
module com.kodedu.terminalfx {

    requires javafx.controls;
    requires javafx.web;
    requires jdk.jsobject;

    exports com.kodedu.terminalfx;

    // The page calls into TerminalBridge, and the WebView's script engine reaches it by
    // reflection, so this package is open as well as exported.
    opens com.kodedu.terminalfx;
}
