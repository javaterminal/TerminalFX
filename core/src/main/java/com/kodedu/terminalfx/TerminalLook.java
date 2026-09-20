package com.kodedu.terminalfx;

import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * How a terminal looks and what it does with the keyboard and the clipboard.
 *
 * <p>Per instance, not per application: two terminals in one window may be dressed differently,
 * and nothing here is read from a preference store. Whoever embeds this library decides where
 * these values come from.
 *
 * <p>A record with {@code with…} methods rather than a bean with setters: a look handed to a
 * terminal is the look it has, and changing one field produces a new value the terminal is told
 * about, so there is no moment in which a terminal is wearing half of two looks.
 *
 * @param fontFamily             a CSS font stack; the renderer measures the first face it has
 * @param fontSize               in points
 * @param background             the page behind the text
 * @param foreground             the default text colour
 * @param cursor                 the block or bar
 * @param cursorBlink            whether it blinks
 * @param scrollbarVisible       whether the renderer draws one
 * @param scrollback             how many lines above the screen are kept. Bounded on purpose:
 *                               an unbounded scrollback is a memory leak with a nice name
 * @param scrollMultiplier       how far one wheel notch moves
 * @param audibleBell            whether {@code \\a} rings. Off by default, and an option rather
 *                               than something a subclass has to arrange
 * @param copyOnSelect           selecting text puts it on the clipboard
 * @param clearSelectionAfterCopy the selection goes away once copied
 * @param ctrlCCopies            Ctrl+C copies when there is a selection, instead of interrupting
 * @param ctrlVPastes            Ctrl+V pastes
 * @param allowRemoteClipboard   whether the far end may write the clipboard through OSC 52.
 *                               <b>Off by default.</b> Output is not trusted input
 * @param userCss                extra CSS for the terminal's own document, or empty
 */
public record TerminalLook(
        String fontFamily,
        int fontSize,
        Color background,
        Color foreground,
        Color cursor,
        boolean cursorBlink,
        boolean scrollbarVisible,
        int scrollback,
        double scrollMultiplier,
        boolean audibleBell,
        boolean copyOnSelect,
        boolean clearSelectionAfterCopy,
        boolean ctrlCCopies,
        boolean ctrlVPastes,
        boolean allowRemoteClipboard,
        String userCss) {

    public TerminalLook {
        Objects.requireNonNull(fontFamily, "fontFamily");
        Objects.requireNonNull(background, "background");
        Objects.requireNonNull(foreground, "foreground");
        Objects.requireNonNull(cursor, "cursor");
        Objects.requireNonNull(userCss, "userCss");
        if (fontSize < 1) {
            throw new IllegalArgumentException("fontSize " + fontSize);
        }
        if (scrollback < 0) {
            throw new IllegalArgumentException("scrollback " + scrollback);
        }
    }

    /** A dark terminal in a monospaced stack every desktop has something for. */
    public static TerminalLook dark() {
        return new TerminalLook(
                "'DejaVu Sans Mono', 'Menlo', 'Consolas', monospace", 14,
                Color.web("#101010"), Color.web("#F0F0F0"), Color.web("#FFFFFF"),
                false, true, 1000, 1.0, false,
                false, true, true, true, false, "");
    }

    /** The same terminal on a white page. */
    public static TerminalLook light() {
        return dark()
                .withBackground(Color.web("#FFFFFF"))
                .withForeground(Color.web("#000000"))
                .withCursor(Color.web("#000000"));
    }

    public TerminalLook withFontFamily(String value) {
        return new TerminalLook(value, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withFontSize(int value) {
        return new TerminalLook(fontFamily, value, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withBackground(Color value) {
        return new TerminalLook(fontFamily, fontSize, value, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withForeground(Color value) {
        return new TerminalLook(fontFamily, fontSize, background, value, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withCursor(Color value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, value, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withCursorBlink(boolean value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, value,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withScrollbarVisible(boolean value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                value, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withScrollback(int value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, value, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withScrollMultiplier(double value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, value, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withAudibleBell(boolean value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, value, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withCopyOnSelect(boolean value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, value,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withClearSelectionAfterCopy(boolean value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                value, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withCtrlCCopies(boolean value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, value, ctrlVPastes, allowRemoteClipboard, userCss);
    }

    public TerminalLook withCtrlVPastes(boolean value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, value, allowRemoteClipboard, userCss);
    }

    public TerminalLook withAllowRemoteClipboard(boolean value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, value, userCss);
    }

    public TerminalLook withUserCss(String value) {
        return new TerminalLook(fontFamily, fontSize, background, foreground, cursor, cursorBlink,
                scrollbarVisible, scrollback, scrollMultiplier, audibleBell, copyOnSelect,
                clearSelectionAfterCopy, ctrlCCopies, ctrlVPastes, allowRemoteClipboard, value);
    }
}
