package com.kodedu.terminalfx;

import java.nio.file.Path;

import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.processes.ExternalProcessPty;
import com.pty4j.PtyProcess;

public class Terminal extends GenericTerminal<ExternalProcessPty> {

    public Terminal() {
        this(null, null);
    }

    public Terminal(TerminalConfig terminalConfig, Path terminalPath) {
        super(terminalConfig, new ExternalProcessPty(terminalPath));
        getPty().setTerminalConfig(getTerminalConfig());
    }

    public Path getTerminalPath() {
        return getPty().getTerminalPath();
    }

    public PtyProcess getProcess() {
        return getPty().getProcess();
    }
}
