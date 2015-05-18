package com.kodcu;

import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FXMLController implements Initializable {

    public TextField input;
    final String $HOME = System.getProperty("user.home");
    public TabPane tabPane;
    Path currentPath = Paths.get($HOME);
    LinkedList<String> candidates = new LinkedList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Tab tab = createTab();
        tab.setText("terminal session");

        AnchorPane anchorPane = new AnchorPane();
        tab.setContent(anchorPane);
        TextArea output = new TextArea();
        AnchorPane.setBottomAnchor(output, 0.0);
        AnchorPane.setLeftAnchor(output, 0.0);
        AnchorPane.setRightAnchor(output, 0.0);
        AnchorPane.setTopAnchor(output, 0.0);

        anchorPane.getChildren().add(output);
        tabPane.getTabs().add(tab);
        final ProcessExecutor processExecutor = new ProcessExecutor();

        output.setText(currentPath.toString());
        input.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB) {
                List<String> latestList = getLatestList();
                String text = input.getText();
                String[] commands = text.split(" ");
                if (commands.length > 1) {
                    String command = commands[1];
                    if (candidates.size() == 0)
                        candidates = latestList.stream().filter(e -> e.contains(command)).collect(Collectors.toCollection(LinkedList::new));
                    if (candidates.size() > 1) {
                        String popped = candidates.pop();
                        input.setText(commands[0] + " " + popped);
                        input.positionCaret(input.getText().length());
                    }

                }
            }
        });

        input.setOnAction(event -> {
            String inputText = input.getText();
            input.setText("");

            ThreadHelper.runTaskLater(() -> {
                try {

                    String[] command = inputText.split(" ");

                    if (command.length > 0) {
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
                }
            });


        });
    }

    private Tab createTab() {
        Tab tab = new Tab();
        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem = new MenuItem("Start new session");
        menuItem.setOnAction(event -> {

        });
        contextMenu.getItems().add(menuItem);
        tab.setContextMenu(contextMenu);
        return tab;
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
