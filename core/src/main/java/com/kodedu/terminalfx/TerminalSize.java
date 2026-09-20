package com.kodedu.terminalfx;

/**
 * How many characters fit, which is the only size the far end is told about.
 *
 * <p>One value rather than two fields, because the pair is what a resize means: a terminal that
 * had its columns applied and not its rows has been told something that was never true. Resizes
 * are coalesced by keeping the last of these, not by remembering two numbers separately.
 */
public record TerminalSize(int columns, int rows) {

    public TerminalSize {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException(columns + "x" + rows);
        }
    }

    @Override
    public String toString() {
        return columns + "x" + rows;
    }
}
