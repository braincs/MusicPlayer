package com.braincs.attrsc.musicplayer.utils;

/**
 * Created by Shuai
 * 18/12/2019.
 */
public class TimeUtil {
    public static String int2TimeStr(int time){
        String min = ""+ time / 60;
        String sec = ""+ time % 60;
        return min + ":"+ sec;
    }
}
