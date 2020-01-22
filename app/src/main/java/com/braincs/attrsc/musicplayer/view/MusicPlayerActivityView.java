package com.braincs.attrsc.musicplayer.view;

import android.content.Context;

import com.braincs.attrsc.musicplayer.MusicPlayerModel;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public interface MusicPlayerActivityView extends MusicPlayerView {

    void setFreshing(boolean isFreshing);

    void scrollTo(int position, boolean isSmooth);

    void updateTimerLeft(String time);

    void themeUpdate();
}
