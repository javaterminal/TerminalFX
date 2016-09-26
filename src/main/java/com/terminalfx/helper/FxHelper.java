package com.terminalfx.helper;

import javafx.scene.paint.Color;

/**
 * Created by usta on 26.09.2016.
 */
public class FxHelper {

    public static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
