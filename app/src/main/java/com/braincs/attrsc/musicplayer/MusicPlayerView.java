package com.braincs.attrsc.musicplayer;

import android.content.Context;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public interface MusicPlayerView {
    Context getContext();
    void updateProgress(int progress, int total);

    void setMusicBtnPlay();
    void setMusicBtnPause();

    void setMusicBarName(String name);
}
