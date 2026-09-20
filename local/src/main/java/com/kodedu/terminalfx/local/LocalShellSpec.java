package com.kodedu.terminalfx.local;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * What a local shell is: a command, a folder and an environment.
 *
 * <p>Separate from how the terminal looks, because they are settings of different kinds and
 * putting them in one object is what made the old library's {@code TerminalConfig} a place where
 * a colour and a command line lived together.
 *
 * @param command     argv, already split. No shell parsing here: a path with a space in it is
 *                    the embedder's problem to get right, not this library's to guess at
 * @param directory   where to start, or null for the process's own
 * @param environment added to the process's own environment
 * @param term        what to set {@code TERM} to
 */
public record LocalShellSpec(String[] command, Path directory, Map<String, String> environment,
        String term) {

    public LocalShellSpec {
        if (command == null || command.length == 0) {
            throw new IllegalArgumentException("a shell needs a command");
        }
        command = command.clone();
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        term = term == null || term.isBlank() ? "xterm-256color" : term;
    }

    /** The usual shell for this operating system, in the person's home directory. */
    public static LocalShellSpec defaultShell() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String[] command = windows
                ? new String[] { "cmd.exe" }
                : new String[] { shell(), "-i" };
        return new LocalShellSpec(command, Path.of(System.getProperty("user.home")), Map.of(),
                "xterm-256color");
    }

    private static String shell() {
        String configured = System.getenv("SHELL");
        return configured == null || configured.isBlank() ? "/bin/sh" : configured;
    }

    public LocalShellSpec in(Path directory) {
        return new LocalShellSpec(command, directory, environment, term);
    }

    public LocalShellSpec running(List<String> argv) {
        return new LocalShellSpec(argv.toArray(String[]::new), directory, environment, term);
    }

    @Override
    public String[] command() {
        return command.clone();
    }
}
