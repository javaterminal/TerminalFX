package com.kodedu.terminalfx;

import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The renderer, in a real WebView: what it draws, what size it says it is, and what it refuses.
 */
@DisplayName("the terminal on the screen")
class TerminalViewTest {

    @BeforeAll
    static void toolkit() throws Exception {
        Fx.start();
    }

    private TerminalView opened(TerminalLook look) throws Exception {
        TerminalView view = Fx.ask(() -> new TerminalView(look));
        Fx.show(view, 900, 500);
        Fx.waitUntil("the renderer came up", view::isReady);
        return view;
    }

    @Test
    @DisplayName("what was written before the page loaded is drawn when it has")
    void whatArrivedTooEarly() throws Exception {
        // A terminal is opened and connected in the same breath, and a far end can be finished
        // with it before a WebView has parsed its own document: a server that refuses the shell,
        // a container that exits at once, a connection that drops during the handshake. Those
        // bytes used to go nowhere, and the screen that would have said what happened came up
        // empty -- at the one moment somebody most needs to be told.
        // Made and written to in one turn of the UI thread: the page cannot have loaded, because
        // loading it needs this thread and this thread is here. Made in a separate call and the
        // engine has usually finished by the second one, which is the test quietly passing for
        // the wrong reason.
        TerminalView[] made = new TerminalView[1];
        boolean[] readyAlready = new boolean[1];
        Fx.run(() -> {
            made[0] = new TerminalView(TerminalLook.dark());
            readyAlready[0] = made[0].isReady();
            made[0].write("the shell ended with status 0\r\n");
        });
        TerminalView view = made[0];
        assertFalse(readyAlready[0], "the point of the test is that it is not up yet");

        Fx.show(view, 900, 500);
        Fx.waitUntil("the renderer came up", view::isReady);

        Fx.waitUntil("what was said before it could draw",
                () -> view.screenText().contains("the shell ended with status 0"));
    }

    @Test
    @DisplayName("says which mode its keys are in, because two terminals are rarely in the same one")
    void saysWhatModeItIsIn() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        assertFalse(Fx.ask(view::applicationCursorKeys), "a terminal starts in neither");
        assertFalse(Fx.ask(view::bracketedPaste));

        // What a program asks for when it wants the arrow keys to itself.
        Fx.run(() -> view.write("\u001b[?1h"));
        Fx.waitUntil("the mode to be set", view::applicationCursorKeys);

        Fx.run(() -> view.write("\u001b[?2004h"));
        Fx.waitUntil("bracketed paste too", view::bracketedPaste);

        Fx.run(() -> view.write("\u001b[?1l"));
        Fx.waitUntil("and off again", () -> !view.applicationCursorKeys());
    }

    @Test
    @DisplayName("draws ASCII, colour, Turkish, CJK, emoji and box drawing")
    void drawsWhatItIsSent() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        Fx.run(() -> {
            view.write("plain ascii\r\n");
            view.write("\u001b[31mred\u001b[0m and \u001b[1mbold\u001b[0m\r\n");
            view.write("türkçe ğüşiöç İIı\r\n");
            view.write("日本語 中文 한국어\r\n");
            view.write("emoji 😀 👍\r\n");
            view.write("box ┌─┬─┐ │ └─┴─┘\r\n");
        });
        Fx.waitUntil("the output was drawn", () -> view.screenText().contains("└─┴─┘"));
        String screen = Fx.ask(view::screenText);
        assertTrue(screen.contains("plain ascii"), screen);
        assertTrue(screen.contains("türkçe ğüşiöç İIı"), screen);
        assertTrue(screen.contains("日本語 中文 한국어"), screen);
        assertTrue(screen.contains("😀"), screen);
        assertTrue(screen.contains("red and bold"), screen);
    }

    @Test
    @DisplayName("says how many characters fit, and says it again when the box changes")
    void reportsItsSize() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        List<TerminalSize> announced = new ArrayList<>();
        Fx.run(() -> view.onResize(announced::add));
        Fx.waitUntil("it sized itself", () -> view.size().columns() > 50);
        TerminalSize wide = Fx.ask(view::size);

        Stage stage = Fx.ask(() -> (Stage) view.getScene().getWindow());
        Fx.run(() -> stage.setWidth(460));
        Fx.waitUntil("it narrowed", () -> view.size().columns() < wide.columns());
        assertNotEquals(wide, Fx.ask(view::size));
        assertFalse(announced.isEmpty(), "a resize nobody was told about");
    }

    @Test
    @DisplayName("fits itself again when its page is resized, with nobody in Java asking")
    void refitsWhenItsPageResizes() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        Fx.waitUntil("it sized itself", () -> view.size().columns() > 50);
        TerminalSize wide = Fx.ask(view::size);

        // A fit measured before the page had its room: two columns, the least it will go to.
        Fx.run(() -> view.getEngineForTest().executeScript(
                "document.getElementById('screen').style.width = '10px'; window.tfxFit();"));
        Fx.waitUntil("it was squeezed", () -> view.size().columns() == 2);

        // The room arrives the way WebKit hands it over: as the page's own resize, a frame later.
        Fx.run(() -> view.getEngineForTest().executeScript(
                "document.getElementById('screen').style.width = '';"
                + " window.dispatchEvent(new Event('resize'));"));
        Fx.waitUntil("it fitted itself to the room", () -> view.size().equals(wide));
    }

    @Test
    @DisplayName("keeps only as much scrollback as it was told to")
    void scrollbackIsBounded() throws Exception {
        TerminalView view = opened(TerminalLook.dark().withScrollback(200));
        Fx.run(() -> {
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < 2000; i++) {
                lines.append("line ").append(i).append("\r\n");
            }
            view.write(lines.toString());
        });
        Fx.waitUntil("the lines were drawn", () -> view.screenText().contains("line 1999"));
        int kept = Fx.ask(view::scrollbackLines);
        assertTrue(kept <= 200, "kept " + kept + " lines of scrollback");
    }

    @Test
    @DisplayName("finds text that is on the screen and does not find what is not")
    void find() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        Fx.run(() -> view.write("the needle is in here\r\n"));
        Fx.waitUntil("drawn", () -> view.screenText().contains("needle"));
        assertTrue(Fx.ask(() -> view.find("needle", true)));
        assertFalse(Fx.ask(() -> view.find("haystack", true)));
        Fx.run(view::clearFind);
    }

    @Test
    @DisplayName("reads a line that wraps over hundreds of rows once, not once for every row")
    void findReadsALongLineOnce() throws Exception {
        // A minified file or a log line with no breaks in it is one line that the terminal wraps
        // over hundreds of rows. xterm.js's search addon went back from every one of those rows to
        // the row the line began on and read the whole line again, so the work grew with the square
        // of the line's length -- on the thread that draws the window. One 60,000-character line
        // froze the window for five to ten seconds each time Find answered "not found".
        TerminalView view = opened(TerminalLook.dark().withScrollback(5000));
        String line = "z".repeat(80_000);
        Fx.run(() -> view.write("before the line\r\n" + line + "Needle-At-The-End\r\nafter the line\r\n"));
        Fx.waitUntil("drawn", () -> view.screenText().contains("after the line"));

        long started = System.nanoTime();
        boolean found = Fx.ask(() -> view.find("not anywhere", true));
        long took = (System.nanoTime() - started) / 1_000_000;
        assertFalse(found);
        assertTrue(took < 1000, "Find held the UI thread for " + took + " ms");

        assertTrue(Fx.ask(() -> view.find("needle-at-the-end", true)));
        assertEquals("Needle-At-The-End", Fx.ask(view::selection));
    }

    @Test
    @DisplayName("Next goes on from what it found, Previous goes back, and both come round again")
    void findGoesOnAndBack() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        Fx.run(() -> view.write("one apple\r\ntwo Apple\r\nthree APPLE and pear\r\n"));
        Fx.waitUntil("drawn", () -> view.screenText().contains("pear"));

        assertTrue(Fx.ask(() -> view.find("apple", true)));
        assertEquals("apple", Fx.ask(view::selection));
        assertTrue(Fx.ask(() -> view.find("apple", true)));
        assertEquals("Apple", Fx.ask(view::selection));
        assertTrue(Fx.ask(() -> view.find("apple", true)));
        assertEquals("APPLE", Fx.ask(view::selection));
        assertTrue(Fx.ask(() -> view.find("apple", true)), "from the last one it comes round");
        assertEquals("apple", Fx.ask(view::selection));

        assertTrue(Fx.ask(() -> view.find("apple", false)), "and back past the first");
        assertEquals("APPLE", Fx.ask(view::selection));
        assertTrue(Fx.ask(() -> view.find("apple", false)));
        assertEquals("Apple", Fx.ask(view::selection));
    }

    @Test
    @DisplayName("finds text across the place where a line wraps, and after wide letters")
    void findAcrossAWrap() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        Fx.waitUntil("it sized itself", () -> view.size().columns() > 50);
        int columns = Fx.ask(() -> view.size().columns());
        // A wide letter is one character and two cells: where a match is in the text is not where
        // it is on the screen, and selecting by the first would select the wrong cells.
        String wide = "日本語";
        String filler = "x".repeat(columns - wide.length() * 2 - 3);
        Fx.run(() -> view.write(wide + filler + "spanning the wrap\r\n"));
        Fx.waitUntil("drawn", () -> view.screenText().contains("the wrap"));

        assertTrue(Fx.ask(() -> view.find("spanning", true)));
        assertEquals("spanning", Fx.ask(view::selection));
        assertTrue(Fx.ask(() -> view.find("本語", true)));
        assertEquals("本語", Fx.ask(view::selection));
    }

    @Test
    @DisplayName("hands a title from the far end over as text, and runs nothing")
    void titlesAreData() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        List<String> titles = new ArrayList<>();
        Fx.run(() -> view.onTitle(titles::add));
        // A title that would be a script if anybody were foolish enough to treat it as one.
        Fx.run(() -> view.write("\u001b]0;</script><img src=x onerror=alert(1)>\u0007"));
        Fx.waitUntil("the title arrived", () -> !titles.isEmpty());
        assertEquals("</script><img src=x onerror=alert(1)>", titles.get(0));
    }

    @Test
    @DisplayName("Ctrl+Shift+C and Ctrl+Shift+V are copy and paste, through the embedder")
    void linuxCopyAndPaste() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        List<String> copied = new ArrayList<>();
        int[] pastes = new int[1];
        Fx.run(() -> {
            view.onCopy(copied::add);
            view.onPasteRequested(() -> pastes[0]++);
            view.write("copy-canary\r\n");
        });
        Fx.waitUntil("the line was drawn", () -> view.screenText().contains("copy-canary"));
        Fx.run(view::selectAll);

        Fx.run(() -> pressIn(view, "C"));
        Fx.waitUntil("Ctrl+Shift+C copied the selection", () -> !copied.isEmpty());
        assertTrue(copied.get(0).contains("copy-canary"), copied.get(0));

        // The WebView's own paste would type the clipboard in without asking the embedder,
        // which is where a multi-line paste is previewed.
        Fx.run(() -> pressIn(view, "V"));
        Fx.waitUntil("Ctrl+Shift+V asked the embedder to paste", () -> pastes[0] == 1);
    }

    /** A Ctrl+Shift keystroke as the page receives it, at xterm.js's own input. */
    private static void pressIn(TerminalView view, String key) {
        view.getEngineForTest().executeScript(
                "document.querySelector('.xterm-helper-textarea').dispatchEvent(new KeyboardEvent("
                        + "'keydown', {key: '" + key + "', ctrlKey: true, shiftKey: true, "
                        + "bubbles: true, cancelable: true}))");
    }

    @Test
    @DisplayName("ignores a clipboard write from the far end unless it was allowed")
    void remoteClipboardIsOffByDefault() throws Exception {
        TerminalView off = opened(TerminalLook.dark());
        List<String> asked = new ArrayList<>();
        Fx.run(() -> off.onRemoteClipboard(asked::add));
        Fx.run(() -> off.write("\u001b]52;c;aGVsbG8=\u0007hello\r\n"));
        Fx.waitUntil("the output after it was drawn", () -> off.screenText().contains("hello"));
        assertTrue(asked.isEmpty(), "the far end wrote the clipboard without being allowed to");

        TerminalView on = opened(TerminalLook.dark().withAllowRemoteClipboard(true));
        List<String> allowed = new ArrayList<>();
        Fx.run(() -> on.onRemoteClipboard(allowed::add));
        Fx.run(() -> on.write("\u001b]52;c;aGVsbG8=\u0007hello\r\n"));
        Fx.waitUntil("the request arrived", () -> !allowed.isEmpty());
        assertTrue(allowed.get(0).contains("aGVsbG8="), allowed.get(0));
    }

    @Test
    @DisplayName("a megabyte of output is text, never script")
    void outputIsNeverParsed() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        String dangerous = "\"; window.wasParsed = true; \"";
        Fx.run(() -> view.write(dangerous + "\r\n"));
        Fx.waitUntil("drawn", () -> view.screenText().contains("wasParsed"));
        assertEquals("undefined", Fx.ask(() ->
                String.valueOf(view.getEngineForTest().executeScript("typeof window.wasParsed"))));
    }

    @Test
    @DisplayName("wears a different look without being rebuilt")
    void looksCanChange() throws Exception {
        TerminalView view = opened(TerminalLook.dark());
        Fx.run(() -> view.look(TerminalLook.light().withFontSize(18)));
        assertEquals(18, Fx.ask(() -> view.look().fontSize()));
        Fx.waitUntil("it is still drawing", view::isReady);
    }
}
