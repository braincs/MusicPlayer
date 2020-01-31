package com.braincs.attrsc.musicplayer;

import android.os.Environment;
import android.support.annotation.NonNull;

import com.braincs.attrsc.musicplayer.utils.Constants;
import com.braincs.attrsc.musicplayer.utils.MediaUtil;
import com.google.gson.Gson;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Created by Shuai
 * 18/12/2019.
 */
public class MusicPlayerModel {
    public final static int STATE_IDLE = 0;
    public final static int STATE_PAUSE = 1;
    public final static int STATE_PLAYING = 2;
    private int state = STATE_IDLE;
    private String directory;
    private List<String> musicList;
    private int currentIndex;
    private int currentPosition;
    private int totalDuration;

    public MusicPlayerModel(String directory) {
        this.directory = directory;
    }

    public MusicPlayerModel(int state, List<String> musicList, int currentIndex, int currentPosition, int totalDuration) {
        this.directory = Constants.MUSIC_DIRECTORY;
        this.state = state;
        this.musicList = musicList;
        this.currentIndex = currentIndex;
        this.currentPosition = currentPosition;
        this.totalDuration = totalDuration;
    }

    public List<String> getMusicList() {

        //todo async method implementation
        if (musicList == null || musicList.size() == 0) {
            scanMusic();
        }
        return musicList;
    }

    public void scanMusic() {
        musicList = MediaUtil.getAllMediaMp3Files(directory);
        // sort by integer string
        Collections.sort(musicList, MediaUtil.integerComparator);
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public String getDirectory() {
        File fdir = new File(Environment.getExternalStorageDirectory(), directory);
        return fdir.getAbsolutePath();
    }

    public String currentMusic() {
        return musicList.get(currentIndex);
    }

    public int next() {
        currentIndex++;
        currentPosition = 0;
        if (currentIndex >= musicList.size()) {
            currentIndex = 0;
        }
        return currentIndex;
    }

    public int previous() {
        currentIndex--;
        currentPosition = 0;
        if (currentIndex <= 0) {
            currentIndex = musicList.size() - 1;
        }
        return currentIndex;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
