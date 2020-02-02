package com.braincs.attrsc.musicplayer.view;

import android.content.Context;

import com.braincs.attrsc.musicplayer.MusicPlayerModel;

import java.util.List;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public interface MusicPlayerView {
    Context getContext();

    void setMusicBtnPlay();

    void setMusicBtnPause();

    void setMusicBarName(String name);

}
