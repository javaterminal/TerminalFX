package com.kodcu;

import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by usta on 29.03.2015.
 */
public class Deneme {

    public static void main(String[] args) throws InterruptedException, TimeoutException, IOException {
        ProcessExecutor command = new ProcessExecutor("dir");
        command.redirectOutput(System.out).redirectError(System.err).execute();
    }
}
