package com.kodedu.terminalfx.processes;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.helper.IOHelper;
import com.kodedu.terminalfx.helper.ThreadHelper;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.sun.jna.Platform;

public class ExternalProcessPty implements Pty {
	private TerminalConfig config;

	final private Path dataDir;
	private Path terminalPath;

	private PtyProcess process;

	private String[] termCommand;

	public ExternalProcessPty() {
		final String userHome = System.getProperty("user.home");
		dataDir = Paths.get(userHome).resolve(".terminalfx");
		config = null;
		terminalPath = null;
	}

	public ExternalProcessPty(Path terminalPath) {
		this();
		this.terminalPath = terminalPath;
	}

	public TerminalConfig getTerminalConfig() {
		return config;
	}

	public void setTerminalConfig(TerminalConfig config) {
		this.config = config;
	}

	public Path getDataDir() {
		return dataDir;
	}

	public PtyProcess getProcess() {
		return process;
	}

	public Path getTerminalPath() {
		return terminalPath;
	}

	@Override
	public void fork() throws Exception {
		final Path dataDir = getDataDir();
		IOHelper.copyLibPty(dataDir);

		if (Platform.isWindows()) {
			this.termCommand = getTerminalConfig().getWindowsTerminalStarter().split("\\s+");
		} else {
			this.termCommand = getTerminalConfig().getUnixTerminalStarter().split("\\s+");
		}

		final Map<String, String> envs = new HashMap<>(System.getenv());
		envs.put("TERM", "xterm");

		System.setProperty("PTY_LIB_FOLDER", dataDir.resolve("libpty").toString());

		if (Objects.nonNull(terminalPath) && Files.exists(terminalPath)) {
			this.process = PtyProcess.exec(termCommand, envs, terminalPath.toString());
		} else {
			this.process = PtyProcess.exec(termCommand, envs);
		}
	}

	@Override
	public InputStream getInputStream() {
		return getProcess().getInputStream();
	}

	@Override
	public InputStream getErrorStream() {
		return getProcess().getErrorStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return getProcess().getOutputStream();
	}

	@Override
	public void waitFor() throws InterruptedException {
		getProcess().waitFor();
	}

	@Override
	public void setWinSize(int columns, int rows) {
		getProcess().setWinSize(new WinSize(columns, rows));
	}

	@Override
	public void close() {
		while (Objects.isNull(getProcess())) {
			ThreadHelper.sleep(250);
		}
		getProcess().destroy();
	}

}
