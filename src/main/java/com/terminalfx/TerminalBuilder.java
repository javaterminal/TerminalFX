package com.terminalfx;

import com.terminalfx.config.DefaultTabNameGenerator;
import com.terminalfx.config.TerminalConfig;
import com.terminalfx.config.TabNameGenerator;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Created by usta on 12.09.2016.
 */
public class TerminalBuilder {

    private Path terminalPath;
    private TerminalConfig terminalConfig;
    private TabNameGenerator nameGenerator;

    public TerminalBuilder() {
    }

    public TerminalBuilder(TerminalConfig terminalConfig) {
        this.terminalConfig = terminalConfig;
    }

    public TerminalConfig getTerminalConfig() {
        if (Objects.isNull(terminalConfig)) {
            terminalConfig = new TerminalConfig();
        }
        return terminalConfig;
    }

    public void setTerminalConfig(TerminalConfig terminalConfig) {
        this.terminalConfig = terminalConfig;
    }

    public TabNameGenerator getNameGenerator() {
        if (Objects.isNull(nameGenerator)) {
            nameGenerator = new DefaultTabNameGenerator();
        }
        return nameGenerator;
    }

    public void setNameGenerator(TabNameGenerator nameGenerator) {
        this.nameGenerator = nameGenerator;
    }

    public Path getTerminalPath() {
        return terminalPath;
    }

    public void setTerminalPath(Path terminalPath) {
        this.terminalPath = terminalPath;
    }

    public TerminalTab newTerminal() {
        TerminalTab terminalTab = new TerminalTab(terminalConfig, nameGenerator, terminalPath);
        return terminalTab;
    }
}
