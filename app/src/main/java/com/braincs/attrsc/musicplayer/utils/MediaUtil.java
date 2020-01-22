package com.braincs.attrsc.musicplayer.utils;

import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public class MediaUtil {
    private final static String TAG = MediaUtil.class.getSimpleName();
    private final static String[] MUSIC_SURFIX = {"mp3", "Mp3", "MP3"};

    public static List<String> getAllMediaMp3Files(String subSdcard) {

        Log.d(TAG, "--getAllMediaMp3Files--");
        File externalStorageDirectory = Environment.getExternalStorageDirectory();

        List<String> surfixList = Arrays.asList(MUSIC_SURFIX);
        return getFiles(new File(externalStorageDirectory, subSdcard), surfixList);
    }

    public static List<String> getAllMediaMp3Files() {

        Log.d(TAG, "--getAllMediaMp3Files--");
        File externalStorageDirectory = Environment.getExternalStorageDirectory();

        List<String> surfixList = Arrays.asList(MUSIC_SURFIX);
        return getFiles(new File(externalStorageDirectory, "Music"), surfixList);
    }

    /**
     * getFiles from directory with specific suffix
     * Note that: it is a time-consuming function
     *
     * @param directory directory
     * @param suffix    suffix e.g. ["mp3", "MP3"].asList();
     * @return list of file abs path
     */
    public static List<String> getFiles(File directory, List<String> suffix) {
        List<String> files = new LinkedList<>();
        File[] fList = directory.listFiles();
        if (null == fList || fList.length == 0) return files;
        for (File file : fList) {

            if (file.isFile()) {
                String fileName = file.getName();
                String[] s = fileName.split("\\.");
                Log.d(TAG, file.getName() + Arrays.toString(s));
                if (s.length < 1) continue;
                String curSurfix = s[s.length - 1];
                if (suffix.contains(curSurfix)) {
                    files.add(file.getAbsolutePath());
                }
            } else if (file.isDirectory()) {
                List<String> subFiles = getFiles(file, suffix);
                files.addAll(subFiles);
            }
        }

        return files;

    }

    public static Comparator<String> integerComparator = new Comparator<String>() {
        public int compare(String o1, String o2) {
            // 去掉尾缀 .mp3
//            String[] split1 = new File(o1).getName().split("\\.");
//            String[] split2 = new File(o2).getName().split("\\.");
//            String s1 = Arrays.toString(Arrays.copyOf(split1, split1.length - 1));
//            String s2 = Arrays.toString(Arrays.copyOf(split2, split2.length - 1));
            String n1 = new File(o1).getName();
            String n2 = new File(o2).getName();
            String s1 = n1.substring(0, n1.lastIndexOf('.'));
            String s2 = n2.substring(0, n2.lastIndexOf('.'));
            return extractInt(s1) - extractInt(s2);
        }

        int extractInt(String s) {
            String num = s.replaceAll("\\D", "");
            Log.d(TAG, num);
            // return 0 if no digits found
            return num.isEmpty() ? 0 : Integer.parseInt(num.trim());
        }
    };
}
