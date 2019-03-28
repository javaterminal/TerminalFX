package com.kodedu.terminalfx;

import com.kodedu.terminalfx.processes.Pty;

@FunctionalInterface
public interface TerminalGenerator<T extends Pty> {

	GenericTerminal<T> generate();
}
