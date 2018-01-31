package com.kodedu.terminalfx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodedu.terminalfx.annotation.WebkitCall;
import com.kodedu.terminalfx.config.DefaultTabNameGenerator;
import com.kodedu.terminalfx.config.TabNameGenerator;
import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.helper.IOHelper;
import com.kodedu.terminalfx.helper.ThreadHelper;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.sun.jna.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Created by usta on 16.09.2015.
 */
public class TerminalTab extends Tab {

    private WebView webView;

    private int columns = 150;
    private int rows = 15;
    private PtyProcess process;
    private BufferedReader inputReader;
    private BufferedReader errorReader;
    private BufferedWriter outputWriter;
    private Path terminalPath;
    private String[] termCommand;
    private LinkedBlockingQueue<String> commandQueue;
    private TerminalConfig terminalConfig = new TerminalConfig();
    private TabNameGenerator tabNameGenerator;
    private boolean isTerminalReady = false;
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public TerminalTab(TerminalConfig terminalConfig, TabNameGenerator tabNameGenerator, Path terminalPath) {
        this.terminalConfig = terminalConfig;
        this.tabNameGenerator = tabNameGenerator;
        this.terminalPath = terminalPath;
        initialize();
    }


    public TerminalTab() {
        initialize();
    }

    public TerminalTab(String text) {
        super(text);
        initialize();
    }

    public TerminalTab(String text, Node content) {
        super(text, content);
        initialize();
    }

    public JSObject getWindow() {
        return (JSObject) webEngine().executeScript("window");
    }

    private WebEngine webEngine() {
        return webView.getEngine();
    }


    public void initialize() {

        commandQueue = new LinkedBlockingQueue<>();
        webView = new WebView();

        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            getWindow().setMember("app", this);
        });

        webEngine().load(TerminalTab.class.getResource("/hterm.html").toExternalForm());

        this.setOnCloseRequest(event -> {
            event.consume();
            closeTerminal();
        });

        String tabName = getTabNameGenerator().next();
        setText(tabName);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem newTab = new MenuItem("New Tab");
        MenuItem closeTab = new MenuItem("Close");
        MenuItem closeOthers = new MenuItem("Close Others");
        MenuItem closeAll = new MenuItem("Close All");

        newTab.setOnAction(this::newTerminal);
        closeTab.setOnAction(this::closeTerminal);
        closeAll.setOnAction(this::closeAllTerminal);
        closeOthers.setOnAction(this::closeOtherTerminals);

        contextMenu.getItems().addAll(newTab, closeTab, closeOthers, closeAll);
        this.setContextMenu(contextMenu);
    }

    private void closeOtherTerminals(ActionEvent actionEvent) {
        ObservableList<Tab> tabs = FXCollections.observableArrayList(this.getTabPane().getTabs());
        for (Tab tab : tabs) {
            if (tab instanceof TerminalTab) {
                if (tab != this) {
                    ((TerminalTab) tab).closeTerminal();
                }
            }
        }
    }

    private void closeAllTerminal(ActionEvent actionEvent) {
        ObservableList<Tab> tabs = FXCollections.observableArrayList(this.getTabPane().getTabs());
        for (Tab tab : tabs) {
            if (tab instanceof TerminalTab) {
                ((TerminalTab) tab).closeTerminal();
            }
        }
    }

    public void newTerminal(ActionEvent... actionEvent) {
        TerminalTab terminalTab = new TerminalTab(getTerminalConfig(), getTabNameGenerator(), getTerminalPath());
        getTabPane().getTabs().add(terminalTab);
        getTabPane().getSelectionModel().select(terminalTab);
    }

    @WebkitCall(from = "hterm")
    public String getPrefs() {
        try {
            return new ObjectMapper().writeValueAsString(getTerminalConfig());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updatePrefs(TerminalConfig terminalConfig) {

        if (getTerminalConfig().equals(terminalConfig)) {
            return;
        }

        setTerminalConfig(terminalConfig);
        String prefs = getPrefs();

        ThreadHelper.runActionLater(() -> {
            try {
                getWindow().call("updatePrefs", prefs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, true);
    }

    @WebkitCall(from = "hterm")
    public void resizeTerminal(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        if (Objects.nonNull(process)) {
            ThreadHelper.runActionLater(() -> {
                process.setWinSize(new WinSize(columns, rows));
            }, true);
        }
    }

    @WebkitCall
    public void onTerminalInit() {
        ThreadHelper.runActionLater(() -> {
            setContent(webView);
        }, true);
    }

    @WebkitCall
    /**
     * Internal use only
     */
    public void onTerminalReady() {

        ThreadHelper.start(() -> {
            try {
                initializeProcess();
            } catch (Exception e) {
                // e.printStackTrace();
            }

        });
    }

    public void onTerminalFxReady(Runnable onReadyAction) {

        ThreadHelper.start(() -> {

            try {
                countDownLatch.await();
            } catch (Exception e) {
            }

            if (Objects.nonNull(onReadyAction)) {
                ThreadHelper.start(onReadyAction);
            }
        });
    }

    @WebkitCall
    public void command(String command) {
        try {
            commandQueue.put(command);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ThreadHelper.start(() -> {
            try {
                outputWriter.write(commandQueue.poll());
                outputWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void print(String text) {
        long count = countDownLatch.getCount();
        if (count == 1) {
            throw new RuntimeException("Terminal is not ready yet.");
        }
        ThreadHelper.runActionLater(() -> {
            getTerminalIO().call("print", text);
        });

    }

    public void focusCursor() {
        ThreadHelper.runActionLater(() -> {
            webView.requestFocus();
            getTerminal().call("focus");
        }, true);
    }

    private JSObject getTerminal() {
        return (JSObject) webEngine().executeScript("t");
    }

    private JSObject getTerminalIO() {
        return (JSObject) webEngine().executeScript("t.io");
    }


    private void initializeProcess() throws Exception {

        Path dataDir = getDataDir();
        IOHelper.copyLibPty(dataDir);

        if (Platform.isWindows()) {
            this.termCommand = terminalConfig.getWindowsTerminalStarter().split("\\s+");
        } else {
            this.termCommand = terminalConfig.getUnixTerminalStarter().split("\\s+");
        }

        Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("TERM", "xterm");

        System.setProperty("PTY_LIB_FOLDER", dataDir.resolve("libpty").toString());

        if (Objects.nonNull(terminalPath) && Files.exists(terminalPath)) {
            this.process = PtyProcess.exec(termCommand, envs, terminalPath.toString());
        } else {
            this.process = PtyProcess.exec(termCommand, envs);
        }

        process.setWinSize(new WinSize(columns, rows));
        this.inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        this.outputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        ThreadHelper.start(() -> {
            printReader(inputReader);
        });

        ThreadHelper.start(() -> {
            printReader(errorReader);
        });

        focusCursor();

        countDownLatch.countDown();
        isTerminalReady = true;

        process.waitFor();
    }

    private Path getDataDir() {
        String userHome = System.getProperty("user.home");
        Path dataDir = Paths.get(userHome).resolve(".terminalfx");
        return dataDir;
    }

    private String detectTerminalCharacter() {

        String charset = "UTF-8";

        if (Platform.isWindows()) {
            return windowsCmdCharset().orElse(charset);
        } else {
            return unixTerminalCharset().orElse(charset);
        }

    }

    private void printReader(BufferedReader bufferedReader) {
        try {
            int nRead;
            char[] data = new char[1 * 1024];

            while ((nRead = bufferedReader.read(data, 0, data.length)) != -1) {
                StringBuilder builder = new StringBuilder(nRead);
                builder.append(data, 0, nRead);
                print(builder.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        ThreadHelper.start(() -> {
            while (Objects.isNull(process)) {
                ThreadHelper.sleep(250);
            }
            process.destroy();
            IOHelper.close(inputReader, errorReader, outputWriter);
        });
    }

    @WebkitCall(from = "hterm")
    public void copy(String text) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(text);
        clipboard.setContent(clipboardContent);
    }

    public void closeTerminal(ActionEvent... actionEvent) {
        ThreadHelper.runActionLater(() -> {
            ObservableList<Tab> tabs = this.getTabPane().getTabs();
            if (tabs.size() == 1) {
                newTerminal(actionEvent);
            }
            tabs.remove(this);

            destroy();
        });
    }

    public Optional<String> unixTerminalCharset() {

        final String[] charset = {null};

        try {
            Process process = Runtime.getRuntime().exec(new String[]{termCommand[0], "-c", "locale charmap"});

            String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n"))
                    .trim();

            if (!result.isEmpty()) {
                charset[0] = result;
            }
        } catch (Exception e) {
            return Optional.empty();
        }

        return Optional.ofNullable(charset[0]);
    }

    public Optional<String> windowsCmdCharset() {

        final String[] charset = {null};

        try {
            Process process = Runtime.getRuntime().exec(new String[]{termCommand[0], "/C", "chcp"});

            String result = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n"))
                    .split(":")[1]
                    .trim();

            if (!result.isEmpty()) {
                Integer chcp = Integer.valueOf(result);
                charset[0] = "CP" + chcp;
            }
        } catch (Exception e) {
            return Optional.empty();
        }

        return Optional.ofNullable(charset[0]);
    }


    public void setTerminalPath(Path terminalPath) {
        this.terminalPath = terminalPath;
    }

    public Path getTerminalPath() {
        return terminalPath;
    }


    public TerminalConfig getTerminalConfig() {
        if (Objects.isNull(terminalConfig)) {
            terminalConfig = new TerminalConfig();
        }
        return terminalConfig;
    }

    public void setTerminalConfig(TerminalConfig terminalConfig) {
        this.terminalConfig = terminalConfig;
    }

    public TabNameGenerator getTabNameGenerator() {
        if (Objects.isNull(tabNameGenerator)) {
            tabNameGenerator = new DefaultTabNameGenerator();
        }
        return tabNameGenerator;
    }

    public void setTabNameGenerator(TabNameGenerator tabNameGenerator) {
        this.tabNameGenerator = tabNameGenerator;
    }

    public boolean isTerminalReady() {
        return isTerminalReady;
    }
}
