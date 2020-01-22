package com.braincs.attrsc.musicplayer.receiver;

import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import com.braincs.attrsc.musicplayer.presenter.MusicPlayerPresenter;
import com.braincs.attrsc.musicplayer.utils.SpUtil;

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
        if (intent.getAction() == null) return;
        if (intent.getAction().equalsIgnoreCase(AudioManager.ACTION_AUDIO_BECOMING_NOISY)){
            Log.d(TAG, "AudioManager.ACTION_AUDIO_BECOMING_NOISY");
            if (presenter.isPlaying())
                presenter.pause();
        }
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    Log.d(TAG, "Headset is unplugged");
                    if (presenter.isPlaying() && presenter.getHeadSetStatus() == 1)
                        presenter.pause();
                    presenter.setHeadSetStatus(0);
                    break;
                case 1:
                    Log.d(TAG, "Headset is plugged");
                    presenter.setHeadSetStatus(1);
                    break;
                default:
                    Log.d(TAG, "I have no idea what the headset state is");
            }
        }
    }
}
