/** A shell on this machine, and the only module of this library that needs a native library. */
module com.kodedu.terminalfx.local {

    requires com.kodedu.terminalfx;
    requires pty4j;

    exports com.kodedu.terminalfx.local;
}
