package com.terminalfx.config;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terminalfx.helper.FxHelper;
import javafx.scene.paint.Color;

/**
 * Created by usta on 12.09.2016.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TerminalConfig {

    @JsonProperty(value = "send-encoding")
    private String sendEncoding = "utf-8";

    @JsonProperty(value = "receive-encoding")
    private String receiveEncoding = "utf-8";

    @JsonProperty("use-default-window-copy")
    private boolean useDefaultWindowCopy = true;

    @JsonProperty("clear-selection-after-copy")
    private boolean clearSelectionAfterCopy = true;

    @JsonProperty("copy-on-select")
    private boolean copyOnSelect = false;

    @JsonProperty("ctrl-c-copy")
    private boolean ctrlCCopy = true;

    @JsonProperty("ctrl-v-paste")
    private boolean ctrlVPaste = true;

    @JsonProperty("cursor-color")
    private String cursorColor = "black";

    @JsonProperty(value = "background-color")
    private String backgroundColor = "white";

    @JsonProperty("font-size")
    private int fontSize = 14;

    @JsonProperty(value = "foreground-color")
    private String foregroundColor = "black";

    @JsonProperty("cursor-blink")
    private boolean cursorBlink = false;

    @JsonProperty("scrollbar-visible")
    private boolean scrollbarVisible = true;

    @JsonProperty("enable-clipboard-notice")
    private boolean enableClipboardNotice = true;

    @JsonProperty("scroll-wheel-move-multiplier")
    private double scrollWhellMoveMultiplier = 0.1;

    @JsonProperty("font-family")
    private String fontFamily = "\"DejaVu Sans Mono\", \"Everson Mono\", FreeMono, \"Menlo\", \"Terminal\", monospace";

    @JsonProperty(value = "user-css")
    private String userCss = "data:text/plain;base64," + "eC1zY3JlZW4geyBjdXJzb3I6IGF1dG87IH0=";

    @JsonIgnore
    private String windowsTerminalStarter = "cmd.exe";

    @JsonIgnore
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

    public boolean isCtrlVPaste() {
        return ctrlVPaste;
    }

    public void setCtrlVPaste(boolean ctrlVPaste) {
        this.ctrlVPaste = ctrlVPaste;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TerminalConfig that = (TerminalConfig) o;

        if (useDefaultWindowCopy != that.useDefaultWindowCopy) return false;
        if (clearSelectionAfterCopy != that.clearSelectionAfterCopy) return false;
        if (copyOnSelect != that.copyOnSelect) return false;
        if (ctrlCCopy != that.ctrlCCopy) return false;
        if (ctrlVPaste != that.ctrlVPaste) return false;
        if (fontSize != that.fontSize) return false;
        if (cursorBlink != that.cursorBlink) return false;
        if (scrollbarVisible != that.scrollbarVisible) return false;
        if (enableClipboardNotice != that.enableClipboardNotice) return false;
        if (Double.compare(that.scrollWhellMoveMultiplier, scrollWhellMoveMultiplier) != 0) return false;
        if (sendEncoding != null ? !sendEncoding.equals(that.sendEncoding) : that.sendEncoding != null) return false;
        if (receiveEncoding != null ? !receiveEncoding.equals(that.receiveEncoding) : that.receiveEncoding != null)
            return false;
        if (cursorColor != null ? !cursorColor.equals(that.cursorColor) : that.cursorColor != null) return false;
        if (backgroundColor != null ? !backgroundColor.equals(that.backgroundColor) : that.backgroundColor != null)
            return false;
        if (foregroundColor != null ? !foregroundColor.equals(that.foregroundColor) : that.foregroundColor != null)
            return false;
        if (fontFamily != null ? !fontFamily.equals(that.fontFamily) : that.fontFamily != null) return false;
        if (userCss != null ? !userCss.equals(that.userCss) : that.userCss != null) return false;
        if (windowsTerminalStarter != null ? !windowsTerminalStarter.equals(that.windowsTerminalStarter) : that.windowsTerminalStarter != null)
            return false;
        return unixTerminalStarter != null ? unixTerminalStarter.equals(that.unixTerminalStarter) : that.unixTerminalStarter == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = sendEncoding != null ? sendEncoding.hashCode() : 0;
        result = 31 * result + (receiveEncoding != null ? receiveEncoding.hashCode() : 0);
        result = 31 * result + (useDefaultWindowCopy ? 1 : 0);
        result = 31 * result + (clearSelectionAfterCopy ? 1 : 0);
        result = 31 * result + (copyOnSelect ? 1 : 0);
        result = 31 * result + (ctrlCCopy ? 1 : 0);
        result = 31 * result + (ctrlVPaste ? 1 : 0);
        result = 31 * result + (cursorColor != null ? cursorColor.hashCode() : 0);
        result = 31 * result + (backgroundColor != null ? backgroundColor.hashCode() : 0);
        result = 31 * result + fontSize;
        result = 31 * result + (foregroundColor != null ? foregroundColor.hashCode() : 0);
        result = 31 * result + (cursorBlink ? 1 : 0);
        result = 31 * result + (scrollbarVisible ? 1 : 0);
        result = 31 * result + (enableClipboardNotice ? 1 : 0);
        temp = Double.doubleToLongBits(scrollWhellMoveMultiplier);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (fontFamily != null ? fontFamily.hashCode() : 0);
        result = 31 * result + (userCss != null ? userCss.hashCode() : 0);
        result = 31 * result + (windowsTerminalStarter != null ? windowsTerminalStarter.hashCode() : 0);
        result = 31 * result + (unixTerminalStarter != null ? unixTerminalStarter.hashCode() : 0);
        return result;
    }
}
