package com.kodedu.terminalfx.processes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileCapturePty implements Pty {

	private FileInputStream stderr, stdout;
	private FileOutputStream stdin;

	public FileCapturePty(File stdinDestination, File stdout, File stderr) throws FileNotFoundException {
		this.stdin = new FileOutputStream(stdinDestination);
		this.stdout = new FileInputStream(stdout);
		this.stderr = new FileInputStream(stderr);
	}

	@Override
	public void fork() throws Exception {
		// nothing to do here
	}

	@Override
	public InputStream getInputStream() {
		return stdout;
	}

	@Override
	public InputStream getErrorStream() {
		return stderr;
	}

	@Override
	public OutputStream getOutputStream() {
		return stdin;
	}

	@Override
	public void waitFor() throws InterruptedException {
		// don't wait
	}

	@Override
	public void setWinSize(int columns, int rows) {
		System.out.println("Window size set to " + columns + "x" + rows);
	}

	@Override
	public void close() {
		try {
			stdin.close();
			stdout.close();
			stderr.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
