package com.kodedu.terminalfx;

import java.io.Reader;
import java.io.Writer;

import com.kodedu.terminalfx.config.TabNameGenerator;
import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.helper.IOHelper;
import com.kodedu.terminalfx.helper.ThreadHelper;
import com.kodedu.terminalfx.processes.Pty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyEvent;

public class TerminalTab<T extends Pty> extends Tab {

    private final GenericTerminal<T> terminal;
    private final TabNameGenerator tabNameGenerator;
    private static final String NEW_TAB_KEY = "T";
    private final TerminalGenerator<T> generator;

    public TerminalTab(GenericTerminal<T> terminal, TabNameGenerator tabNameGenerator) {
        this(terminal, null, tabNameGenerator);
    }

    public TerminalTab(TerminalGenerator<T> generator, TabNameGenerator tabNameGenerator) {
        this(generator.generate(), generator, tabNameGenerator);
    }

    public TerminalTab(GenericTerminal<T> terminal, TerminalGenerator<T> generator, TabNameGenerator tabNameGenerator) {
        this.generator = generator;
        this.terminal = terminal;
        this.tabNameGenerator = tabNameGenerator;

        this.terminal.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            if (event.isShortcutDown() && NEW_TAB_KEY.equalsIgnoreCase(event.getText())) {
                newTerminal();
            }
        });

        this.setOnCloseRequest(event -> {
            event.consume();
            closeTerminal();
        });

        final String tabName = getTabNameGenerator().next();
        setText(tabName);

        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem newTab = new MenuItem("New Tab");
        final MenuItem closeTab = new MenuItem("Close");
        final MenuItem closeOthers = new MenuItem("Close Others");
        final MenuItem closeAll = new MenuItem("Close All");

        newTab.setOnAction(this::newTerminal);
        closeTab.setOnAction(this::closeTerminal);
        closeAll.setOnAction(this::closeAllTerminal);
        closeOthers.setOnAction(this::closeOtherTerminals);

        if (generator != null)
            contextMenu.getItems().add(newTab);
        contextMenu.getItems().addAll(closeTab, closeOthers, closeAll);
        this.setContextMenu(contextMenu);

        setContent(terminal);
    }

    private void closeOtherTerminals(ActionEvent actionEvent) {
        final ObservableList<Tab> tabs = FXCollections.observableArrayList(this.getTabPane().getTabs());
        for (final Tab tab : tabs) {
            if (tab instanceof TerminalTab) {
                if (tab != this) {
                    ((TerminalTab<?>) tab).closeTerminal();
                }
            }
        }
    }

    private void closeAllTerminal(ActionEvent actionEvent) {
        final ObservableList<Tab> tabs = FXCollections.observableArrayList(this.getTabPane().getTabs());
        for (final Tab tab : tabs) {
            if (tab instanceof TerminalTab) {
                ((TerminalTab<?>) tab).closeTerminal();
            }
        }
    }

    public void newTerminal(ActionEvent... actionEvent) {
        if (generator == null)
            return;

        final TerminalTab<T> terminalTab = new TerminalTab<T>(generator, getTabNameGenerator());
        getTabPane().getTabs().add(terminalTab);
        getTabPane().getSelectionModel().select(terminalTab);
    }

    public void closeTerminal(ActionEvent... actionEvent) {
        ThreadHelper.runActionLater(() -> {
            final ObservableList<Tab> tabs = this.getTabPane().getTabs();
            if (tabs.size() == 1) {
                newTerminal(actionEvent);
            }
            tabs.remove(this);

            destroy();
        });
    }

    public void destroy() {
        ThreadHelper.start(() -> {
            terminal.destroy();
            IOHelper.close(getInputReader(), getErrorReader(), getOutputWriter());
        });
    }

    public void onTerminalFxReady(Runnable onReadyAction) {
        terminal.onTerminalFxReady(onReadyAction);
    }

    public TabNameGenerator getTabNameGenerator() {
        return tabNameGenerator;
    }

    public TerminalConfig getTerminalConfig() {
        return terminal.getTerminalConfig();
    }

    public Pty getPty() {
        return terminal.getPty();
    }

    public Reader getInputReader() {
        return terminal.getInputReader();
    }

    public Reader getErrorReader() {
        return terminal.getErrorReader();
    }

    public Writer getOutputWriter() {
        return terminal.getOutputWriter();
    }

    public GenericTerminal<T> getTerminalView() {
        return terminal;
    }
}