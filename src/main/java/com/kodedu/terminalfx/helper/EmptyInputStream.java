package com.kodedu.terminalfx.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

/**
 * An empty input stream that never returns. Handy for using as stderr if you never use stderror
 */
public class EmptyInputStream extends InputStream {

	private Semaphore s;

	public EmptyInputStream() {
		s = new Semaphore(0);
	}

	@Override
	public int read() throws IOException {
		try {
			s.acquire();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
		return -1;
	}

	@Override
	public void close() throws IOException {
		super.close();
		s.release();
	}
}
