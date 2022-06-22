package com.kodedu.terminalfx;

import com.kodedu.terminalfx.helper.ThreadHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;


public class TerminalAppStarter extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        InputStream sceneStream = TerminalAppStarter.class.getResourceAsStream("/fxml/Terminal_Scene.fxml");
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(sceneStream);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(TerminalAppStarter.class.getResource("/styles/Styles.css").toExternalForm());

        stage.setTitle("TerminalFX");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        ThreadHelper.stopExecutorService();
        Platform.exit();
        System.exit(0);
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
