package com.kodedu.terminalfx.processes;

import java.io.InputStream;
import java.io.OutputStream;

public interface Pty {
	void fork() throws Exception;

	InputStream getInputStream();

	InputStream getErrorStream();

	OutputStream getOutputStream();

	void waitFor() throws InterruptedException;

	void setWinSize(int columns, int rows);

	void close();
}
