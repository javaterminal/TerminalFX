package com.kodedu.terminalfx.config;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by usta on 26.09.2016.
 */
public class DefaultTabNameGenerator implements TabNameGenerator {

    private AtomicInteger counter = new AtomicInteger();
    private String prefix = "Terminal ";

    @Override
    public String next() {
        return prefix + "#" + counter.incrementAndGet();
    }

    public AtomicInteger getCounter() {
        return counter;
    }

    public void setCounter(AtomicInteger counter) {
        this.counter = counter;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
