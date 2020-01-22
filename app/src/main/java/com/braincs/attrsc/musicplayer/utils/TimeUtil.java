package com.braincs.attrsc.musicplayer.utils;

/**
 * Created by Shuai
 * 18/12/2019.
 */
public class TimeUtil {
    public static String int2TimeStr(int time) {
        time = time / 1000;
        int hour = time / 3600;
        int min = time % 3600 / 60;
        int sec = time % 3600 % 60;
        return String.format("%02d : %02d : %02d", hour, min, sec);
    }
}
