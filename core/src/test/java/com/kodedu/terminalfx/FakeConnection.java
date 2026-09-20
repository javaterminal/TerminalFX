package com.kodedu.terminalfx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** A connection that records what it was told and answers when the test says so. */
final class FakeConnection implements TerminalConnection {

    private final List<String> sent = new ArrayList<>();
    private final List<TerminalSize> sizes = new ArrayList<>();
    private final AtomicInteger closes = new AtomicInteger();
    private final CountDownLatch opened = new CountDownLatch(1);

    private volatile Wiring wiring;
    private volatile Exception refuseToOpen;
    private volatile boolean readyOnOpen = true;
    private volatile IOException failOnSend;

    void refuseToOpen(Exception why) {
        this.refuseToOpen = why;
    }

    void readyOnOpen(boolean value) {
        this.readyOnOpen = value;
    }

    void failOnSend(IOException why) {
        this.failOnSend = why;
    }

    @Override
    public void open(Wiring wiring) throws Exception {
        this.wiring = wiring;
        opened.countDown();
        if (refuseToOpen != null) {
            throw refuseToOpen;
        }
        if (readyOnOpen) {
            wiring.ready();
        }
    }

    @Override
    public void send(byte[] bytes) throws IOException {
        if (failOnSend != null) {
            throw failOnSend;
        }
        synchronized (sent) {
            sent.add(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    @Override
    public void resize(TerminalSize size) {
        synchronized (sizes) {
            sizes.add(size);
        }
    }

    @Override
    public void close() {
        closes.incrementAndGet();
    }

    Wiring wiring() {
        return wiring;
    }

    CountDownLatch opened() {
        return opened;
    }

    List<String> sent() {
        synchronized (sent) {
            return List.copyOf(sent);
        }
    }

    List<TerminalSize> sizes() {
        synchronized (sizes) {
            return List.copyOf(sizes);
        }
    }

    int closes() {
        return closes.get();
    }
}
