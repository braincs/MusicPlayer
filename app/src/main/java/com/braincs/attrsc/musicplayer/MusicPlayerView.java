package com.braincs.attrsc.musicplayer;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public interface MusicPlayerView {
    void updateProgress(int progress, int total);

    void setMusicBtnPlay();
    void setMusicBtnPause();

    void setMusicBarName(String name);
}
