package com.kodedu.terminalfx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.LinkedBlockingQueue;

import com.kodedu.terminalfx.annotation.WebkitCall;
import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.helper.ThreadHelper;
import com.kodedu.terminalfx.processes.Pty;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class GenericTerminal<T extends Pty> extends TerminalView {

	private T process;
	private final ObjectProperty<Writer> outputWriterProperty;
	private final LinkedBlockingQueue<String> commandQueue;

	public GenericTerminal() {
		this(null, null);
	}

	public GenericTerminal(TerminalConfig terminalConfig, T virtualProcess) {
		setTerminalConfig(terminalConfig);
		this.process = virtualProcess;
		outputWriterProperty = new SimpleObjectProperty<>();
		commandQueue = new LinkedBlockingQueue<>();
	}

	@WebkitCall
	public void command(String command) {
		try {
			commandQueue.put(command);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
		ThreadHelper.start(() -> {
			try {
				final String commandToExecute = commandQueue.poll();
				getOutputWriter().write(commandToExecute);
				getOutputWriter().flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void onTerminalReady() {
		ThreadHelper.start(() -> {
			try {
				initializeProcess();
			} catch (final Exception e) {
			}
		});
	}

	private void initializeProcess() throws Exception {
		process.fork();

		columnsProperty().addListener(evt -> updateWinSize());
		rowsProperty().addListener(evt -> updateWinSize());
		updateWinSize();
		setInputReader(new BufferedReader(new InputStreamReader(process.getInputStream())));
		setErrorReader(new BufferedReader(new InputStreamReader(process.getErrorStream())));
		setOutputWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())));
		focusCursor();

		countDownLatch.countDown();

		process.waitFor();
	}

	private void updateWinSize() {
		try {
			process.setWinSize(getColumns(), getRows());
		} catch (Exception e) {
			//
		}
	}

	public ObjectProperty<Writer> outputWriterProperty() {
		return outputWriterProperty;
	}

	public Writer getOutputWriter() {
		return outputWriterProperty.get();
	}

	public void setOutputWriter(Writer writer) {
		outputWriterProperty.set(writer);
	}

	public T getPty() {
		return process;
	}

	public void destroy() {
		getPty().close();
	}

}
