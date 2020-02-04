package com.braincs.attrsc.musicplayer.view;

import android.app.Notification;
import android.content.Context;

import com.braincs.attrsc.musicplayer.MusicPlayerModel;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public interface MusicPlayerNotificationView extends MusicPlayerView {
    Notification displayNotification();

    void cancel();
}
