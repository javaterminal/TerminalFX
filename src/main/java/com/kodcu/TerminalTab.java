package com.kodcu;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by usta on 18.05.2015.
 */
public class TerminalTab extends Tab {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private final TabPane tabPane;
    private Path currentPath = Paths.get(System.getProperty("user.home"));
    private final TextArea output;
    private LinkedList<String> candidates = new LinkedList<>();
    private final ProcessExecutor processExecutor = new ProcessExecutor();
    private TextField input;

    public TerminalTab(TabPane tabPane) {
        this.tabPane = tabPane;
        this.input = new TextField();
        this.setText("Term#" + counter.incrementAndGet());
        this.initializeContextMenu();
        AnchorPane anchorPane = new AnchorPane();
        this.setContent(anchorPane);
        this.output = new TextArea();
        this.output.setFocusTraversable(false);
        this.output.setEditable(false);
        VBox box = new VBox();
        anchorPane.getChildren().add(box);
        box.getChildren().add(output);
        box.getChildren().add(input);
        this.fitToPanel(box);
        output.setText(currentPath.toString());
        this.input.setOnKeyPressed(this::listenCandidates);
        this.input.setOnAction(this::onAction);
        this.setOnClosed(this::closeRequested);
        tabPane.setFocusTraversable(false);

    }

    private void closeRequested(Event... event) {
        if (tabPane.getTabs().size() == 0)
            addNewTab();
    }

    private void onAction(ActionEvent event) {
        String inputText = input.getText();
        input.setText("");

        ThreadHelper.runTaskLater(() -> {
            try {

                String[] command = inputText.split(" ");

                if (command.length > 0) {
                    if (command[0].equalsIgnoreCase("exit")) {
                        ThreadHelper.runActionLater(() -> {
                            tabPane.getTabs().remove(this);
                            closeRequested();
                        });
                        return;
                    }
                    if (command[0].equalsIgnoreCase("clear") || command[0].equalsIgnoreCase("cls")) {
                        output.setText("");
                        return;
                    }
                    if (command[0].equalsIgnoreCase("pwd")) {
                        output.setText(currentPath.toString());
                        return;
                    }
                    if (command[0].equalsIgnoreCase("dir") || command[0].equalsIgnoreCase("ls")) {

                        List<String> lsList = getLatestList();
                        StringBuffer stringBuffer = new StringBuffer();

                        for (int i = 0; i < lsList.size(); i++) {
                            String ls = lsList.get(i);
                            stringBuffer.append(String.format("%-20s\t", ls));
                            if (i % 3 == 0)
                                stringBuffer.append("\n");
                        }

                        output.setText(stringBuffer.toString());
                        return;

                    }
                }

                if (command.length > 1) {
                    if (command[0].equalsIgnoreCase("cd")) {
                        String cdPath = command[1];

                        Path resolved = currentPath.resolve(cdPath).normalize();
                        if (!Files.isDirectory(resolved)) {
                            output.setText("It is not a directory!");
                            return;
                        }
                        currentPath = resolved;
                        output.setText(currentPath.toString());
                        return;
                    }


                }

                processExecutor.command(command);
                processExecutor.readOutput(true);
                processExecutor.directory(currentPath.toFile());

                ProcessResult execute = processExecutor.timeout(5, TimeUnit.SECONDS).execute();
                String value = execute.outputUTF8();
                ThreadHelper.runActionLater(() -> {
                    output.setText("\n" + value);
                });
            } catch (Exception e) {
                e.printStackTrace();
                output.setText(e.getMessage());
            }
        });
    }

    private void initializeContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem = new MenuItem("Start new session");
        menuItem.setOnAction(this::addNewTab);
        contextMenu.getItems().add(menuItem);
        this.setContextMenu(contextMenu);
    }

    private void addNewTab(ActionEvent... actionEvent) {
        TerminalTab tab = new TerminalTab(tabPane);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void fitToPanel(VBox box) {
        AnchorPane.setBottomAnchor(box, 0.0);
        AnchorPane.setLeftAnchor(box, 0.0);
        AnchorPane.setRightAnchor(box, 0.0);
        AnchorPane.setTopAnchor(box, 0.0);
        VBox.setVgrow(output, Priority.ALWAYS);
    }

    private void listenCandidates(KeyEvent event) {
        if (event.getCode() == KeyCode.TAB) {
            List<String> latestList = getLatestList();
            String text = input.getText();
            String[] commands = text.split(" ");
            if (commands.length > 1) {
                String command = commands[1];
                if (candidates.size() == 0)
                    candidates = latestList.stream()
                            .filter(e -> e.contains(command))
                            .collect(Collectors.toCollection(LinkedList::new));
                if (candidates.size() > 1) {
                    String popped = candidates.pop();
                    input.setText(commands[0] + " " + popped);
                    input.positionCaret(input.getText().length());
                }

            }
        }
    }

    private List<String> getLatestList() {
        Stream<Path> lsStream = null;
        try {
            lsStream = Files.list(currentPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> lsList = lsStream.map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
        return lsList;
    }

}
