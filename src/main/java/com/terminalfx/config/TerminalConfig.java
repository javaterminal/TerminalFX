package com.terminalfx.config;


import com.terminalfx.helper.FxHelper;
import javafx.scene.paint.Color;

import javax.json.bind.annotation.JsonbAnnotation;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import java.util.Properties;

/**
 * Created by usta on 12.09.2016.
 */
public class TerminalConfig {

    @JsonbProperty(value = "send-encoding", nillable = true)
    private String sendEncoding = "utf-8";

    @JsonbProperty(value = "receive-encoding", nillable = true)
    private String receiveEncoding = "utf-8";

    @JsonbProperty("use-default-window-copy")
    private boolean useDefaultWindowCopy = true;

    @JsonbProperty("clear-selection-after-copy")
    private boolean clearSelectionAfterCopy = true;

    @JsonbProperty("copy-on-select")
    private boolean copyOnSelect = false;

    @JsonbProperty("ctrl-c-copy")
    private boolean ctrlCCopy = true;

    @JsonbProperty("ctrl-v-paste")
    private boolean ctrlVCopy = true;

    @JsonbProperty("cursor-color")
    private String cursorColor = "black";

    @JsonbProperty(value = "background-color", nillable = true)
    private String backgroundColor = "white";

    @JsonbProperty("font-size")
    private int fontSize = 12;

    @JsonbProperty(value = "foreground-color", nillable = true)
    private String foregroundColor = "black";

    @JsonbProperty("cursor-blink")
    private boolean cursorBlink = false;

    @JsonbProperty("scrollbar-visible")
    private boolean scrollbarVisible = true;

    @JsonbProperty("enable-clipboard-notice")
    private boolean enableClipboardNotice = true;

    @JsonbProperty("scroll-wheel-move-multiplier")
    private double scrollWhellMoveMultiplier = 0.1;

    @JsonbProperty("font-family")
    private String fontFamily = "\"DejaVu Sans Mono\", \"Everson Mono\", FreeMono, \"Menlo\", \"Terminal\", monospace";

    @JsonbProperty(value = "user-css", nillable = true)
    private String userCss = "data:text/plain;base64," + "eC1zY3JlZW4geyBjdXJzb3I6IGF1dG87IH0=";

    @JsonbTransient
    private String windowsTerminalStarter = "cmd.exe";

    @JsonbTransient
    private String unixTerminalStarter = "/bin/bash -i";

    public String getSendEncoding() {
        return sendEncoding;
    }

    public void setSendEncoding(String sendEncoding) {
        this.sendEncoding = sendEncoding;
    }

    public boolean isUseDefaultWindowCopy() {
        return useDefaultWindowCopy;
    }

    public void setUseDefaultWindowCopy(boolean useDefaultWindowCopy) {
        this.useDefaultWindowCopy = useDefaultWindowCopy;
    }

    public boolean isClearSelectionAfterCopy() {
        return clearSelectionAfterCopy;
    }

    public void setClearSelectionAfterCopy(boolean clearSelectionAfterCopy) {
        this.clearSelectionAfterCopy = clearSelectionAfterCopy;
    }

    public boolean isCopyOnSelect() {
        return copyOnSelect;
    }

    public void setCopyOnSelect(boolean copyOnSelect) {
        this.copyOnSelect = copyOnSelect;
    }

    public boolean isCtrlCCopy() {
        return ctrlCCopy;
    }

    public void setCtrlCCopy(boolean ctrlCCopy) {
        this.ctrlCCopy = ctrlCCopy;
    }

    public boolean isCtrlVCopy() {
        return ctrlVCopy;
    }

    public void setCtrlVCopy(boolean ctrlVCopy) {
        this.ctrlVCopy = ctrlVCopy;
    }

    public String getCursorColor() {
        return cursorColor;
    }

    public void setCursorColor(String cursorColor) {
        this.cursorColor = cursorColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(String foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public boolean isCursorBlink() {
        return cursorBlink;
    }

    public void setCursorBlink(boolean cursorBlink) {
        this.cursorBlink = cursorBlink;
    }

    public boolean isScrollbarVisible() {
        return scrollbarVisible;
    }

    public void setScrollbarVisible(boolean scrollbarVisible) {
        this.scrollbarVisible = scrollbarVisible;
    }

    public double getScrollWhellMoveMultiplier() {
        return scrollWhellMoveMultiplier;
    }

    public void setScrollWhellMoveMultiplier(double scrollWhellMoveMultiplier) {
        this.scrollWhellMoveMultiplier = scrollWhellMoveMultiplier;
    }

    public String getUserCss() {
        return userCss;
    }

    public void setUserCss(String userCss) {
        this.userCss = userCss;
    }

    public String getWindowsTerminalStarter() {
        return windowsTerminalStarter;
    }

    public void setWindowsTerminalStarter(String windowsTerminalStarter) {
        this.windowsTerminalStarter = windowsTerminalStarter;
    }

    public String getUnixTerminalStarter() {
        return unixTerminalStarter;
    }

    public void setUnixTerminalStarter(String unixTerminalStarter) {
        this.unixTerminalStarter = unixTerminalStarter;
    }

    public void setBackgroundColor(Color color) {
        setBackgroundColor(FxHelper.colorToHex(color));
    }

    public void setForegroundColor(Color color) {
        setForegroundColor(FxHelper.colorToHex(color));
    }

    public void setCursorColor(Color color) {
        setCursorColor(FxHelper.colorToHex(color));
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getReceiveEncoding() {
        return receiveEncoding;
    }

    public void setReceiveEncoding(String receiveEncoding) {
        this.receiveEncoding = receiveEncoding;
    }

    public boolean isEnableClipboardNotice() {
        return enableClipboardNotice;
    }

    public void setEnableClipboardNotice(boolean enableClipboardNotice) {
        this.enableClipboardNotice = enableClipboardNotice;
    }
}
