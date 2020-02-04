package com.braincs.attrsc.musicplayer.utils;

import java.util.Locale;

/**
 * Created by Shuai
 * 18/12/2019.
 */
public class TimeUtil {
    /**
     * milliseconds to time with format hh:mm:ss
     *
     * @param time milliseconds
     * @return time with format hh:mm:ss
     */
    public static String milliSec2TimeStr(int time) {
        time = time / 1000;
        int hour = time / 3600;
        int min = time % 3600 / 60;
        int sec = time % 3600 % 60;
        return String.format(Locale.getDefault(),"%02d : %02d : %02d", hour, min, sec);
    }
}
