package com.braincs.attrsc.musicplayer;

import android.os.Environment;
import android.support.annotation.NonNull;

import com.braincs.attrsc.musicplayer.utils.MediaUtil;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;

/**
 * Created by Shuai
 * 18/12/2019.
 */
public class MusicPlayerModel {
    public final static int STATE_IDLE = 0;
    public final static int STATE_PLAYING = 1;
    public final static int STATE_PAUSE = 2;
    private int state;
    private String directory;
    private List<String> musicList;
    private int currentIndex;
    private int currentPosition;
    private int totalDuration;

    public MusicPlayerModel(String directory) {
        this.directory = directory;
    }

    public List<String> getMusicList() {

        //todo async method implementation
        if (musicList == null || musicList.size() == 0 ) {
            musicList = MediaUtil.getAllMediaMp3Files(directory);
        }
        return musicList;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public String getDirectory() {
        File fdir = new File(Environment.getExternalStorageDirectory(), directory);
        return fdir.getAbsolutePath();
    }

    public String currentMusic(){
        return musicList.get(currentIndex);
    }

    public int next(){
        currentIndex ++;
        if (currentIndex >= musicList.size()){
            currentIndex = 0;
        }
        return currentIndex;
    }

    public int previous(){
        currentIndex --;
        if (currentIndex <= 0){
            currentIndex = musicList.size() -1;
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
