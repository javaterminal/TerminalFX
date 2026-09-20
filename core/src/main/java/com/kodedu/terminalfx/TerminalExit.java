package com.kodedu.terminalfx;

/**
 * Why a terminal stopped, which is four different things and used to be none.
 *
 * <p>A shell that ran {@code exit 3}, a stream that ended, a connection that broke and a person
 * who closed the tab are four different events, and an embedder that cannot tell them apart
 * cannot say anything useful about any of them. A reconnect offered after a clean {@code exit} is
 * wrong; silence after a broken pipe is worse.
 */
public sealed interface TerminalExit {

    /** The program ended by itself. */
    record Finished(int status) implements TerminalExit {
        @Override
        public String toString() {
            return "finished with status " + status;
        }
    }

    /** The output stream ended without a status — the far end went away politely. */
    record EndOfStream() implements TerminalExit {
        @Override
        public String toString() {
            return "the stream ended";
        }
    }

    /** Somebody closed this terminal. Not a failure and not worth reporting as one. */
    record ClosedByUser() implements TerminalExit {
        @Override
        public String toString() {
            return "closed";
        }
    }

    /** The transport broke, or never came up. */
    record Failed(Throwable cause) implements TerminalExit {
        @Override
        public String toString() {
            return "failed: " + cause;
        }
    }
}
