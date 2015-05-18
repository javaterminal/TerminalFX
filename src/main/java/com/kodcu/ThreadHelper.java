package com.kodcu;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Created by usta on 29.03.2015.
 */
public class ThreadHelper {

    public static void runTaskLater(Runnable runnable) {
        Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                runnable.run();
                return null;
            }
        };

        new Thread(task).start();

    }

    public static void runActionLater(Runnable runnable) {
        Platform.runLater(runnable);
    }
}
