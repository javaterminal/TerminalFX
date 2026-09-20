package com.kodedu.terminalfx;

import java.io.IOException;

/**
 * What a terminal is connected to: a local shell, an SSH channel, a serial port, a fake in a test.
 *
 * <p>Deliberately small, and deliberately ignorant of JavaFX. Everything about threads, ordering,
 * bounds and lifetime is {@link TerminalSession}'s; an implementation of this does the one thing
 * it is for. Nothing here is called on the JavaFX application thread.
 *
 * <h2>The rules an implementation may rely on</h2>
 *
 * <ul>
 *   <li>{@link #open} is called once, off the UI thread, before anything else.</li>
 *   <li>{@link #send} is called from one thread at a time, in the order the bytes were typed.</li>
 *   <li>{@link #resize} may be called before {@link Wiring#ready()} has been answered; an
 *       implementation that cannot resize yet remembers the size rather than throwing.</li>
 *   <li>{@link #close} may be called at any point, including before {@link #open} returns and
 *       more than once. It must be idempotent and must not block for long.</li>
 * </ul>
 */
public interface TerminalConnection {

    /**
     * Brings the connection up. Called off the UI thread; may block.
     *
     * <p>Throwing is how a connection reports it could not start; the session turns that into
     * {@link TerminalExit.Failed} and the embedder hears about it once.
     */
    void open(Wiring wiring) throws Exception;

    /** Sends what was typed. Called in order, from the session's single writer. */
    void send(byte[] bytes) throws IOException;

    /** Tells the far end how big the screen is. */
    void resize(TerminalSize size) throws IOException;

    /** Releases everything. Idempotent, and callable before {@link #open}. */
    void close();

    /** What a connection says back. Every method is safe to call from any thread. */
    interface Wiring {

        /**
         * The connection is up.
         *
         * <p>Separate from the renderer being ready: the two happen in either order, and a
         * session is only usable once both have. Calling this twice, or after the session has
         * been closed, does nothing.
         */
        void ready();

        /**
         * Output arrived. The array is copied by the session, so an implementation may reuse its
         * buffer.
         */
        void output(byte[] bytes, int offset, int length);

        /**
         * The connection ended. The first call wins; later ones are ignored, which is what makes
         * a late callback after a close harmless.
         */
        void ended(TerminalExit exit);
    }
}
