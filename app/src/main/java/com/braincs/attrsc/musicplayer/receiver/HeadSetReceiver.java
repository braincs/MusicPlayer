package com.braincs.attrsc.musicplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.braincs.attrsc.musicplayer.presenter.MusicPlayerPresenter;

/**
 * Created by Shuai
 * 07/01/2020.
 */
public class HeadSetReceiver extends BroadcastReceiver {
    private final static String TAG = HeadSetReceiver.class.getSimpleName();
    private final MusicPlayerPresenter presenter;

    public HeadSetReceiver(MusicPlayerPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() ==null) return;
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)){
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    Log.d(TAG, "Headset is unplugged");
                    if (presenter.isPlaying())
                        presenter.pause();
                    break;
                case 1:
                    Log.d(TAG, "Headset is plugged");
                    break;
                default:
                    Log.d(TAG, "I have no idea what the headset state is");
            }
        }
    }
}
