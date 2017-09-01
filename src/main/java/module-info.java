module terminalfx {

    requires jackson.annotations;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jackson.core;
    requires pty4j;
    requires jackson.databind;
    requires jna;
    requires jdk.jsobject;

    exports com.terminalfx;
    exports com.terminalfx.config to jackson.databind;


}