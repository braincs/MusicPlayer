package com.braincs.attrsc.musicplayer;

import android.os.Environment;

import com.braincs.attrsc.musicplayer.utils.MediaUtil;

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

    public MusicPlayerModel(String directory) {
        this.directory = directory;
    }

    public List<String> getMusicList() {

        //todo async method implementation
        musicList = MediaUtil.getAllMediaMp3Files(directory);
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
}
