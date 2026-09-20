package com.kodedu.terminalfx.example;

import com.kodedu.terminalfx.TerminalLook;
import com.kodedu.terminalfx.TerminalSession;
import com.kodedu.terminalfx.TerminalView;
import com.kodedu.terminalfx.local.LocalShell;
import com.kodedu.terminalfx.local.LocalShellSpec;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The whole library, used: a window, a terminal, a shell.
 *
 * <p>Eighteen lines of wiring, and every one of them is a decision an application would make for
 * itself — where blocking work runs, what the clipboard does, what happens when the shell ends.
 */
public final class TerminalApp extends Application {

    private ExecutorService workers;
    private TerminalSession session;

    @Override
    public void start(Stage stage) {
        workers = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

        TerminalView view = new TerminalView(TerminalLook.dark());
        view.onCopy(text -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        });
        view.onPasteRequested(() -> view.paste(Clipboard.getSystemClipboard().getString()));
        view.onTitle(stage::setTitle);

        session = new TerminalSession(view, new LocalShell(LocalShellSpec.defaultShell(), workers),
                workers);
        session.onExit(how -> Platform.runLater(stage::close));
        session.start();

        stage.setScene(new Scene(view, 900, 520));
        stage.setTitle("TerminalFX");
        stage.show();
        view.whenReady(view::focusTerminal);
    }

    @Override
    public void stop() {
        if (session != null) {
            session.close();
        }
        if (workers != null) {
            workers.shutdownNow();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
