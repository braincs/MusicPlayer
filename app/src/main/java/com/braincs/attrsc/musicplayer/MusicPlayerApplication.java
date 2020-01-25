package com.braincs.attrsc.musicplayer;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.util.Log;

import com.braincs.attrsc.musicplayer.receiver.HeadSetReceiver;

/**
 * Created by Shuai
 * 25/01/2020.
 */
public class MusicPlayerApplication extends Application {
    private static final String TAG = MusicPlayerApplication.class.getSimpleName();
    public static boolean FIRST_START = true;

    private HandlerThread mHandlerThread;
    private HeadSetReceiver headSetReceiver;

    @Override
    public void onCreate() {
        super.onCreate();


//        mHandlerThread = new HandlerThread(TAG);
//        mHandlerThread.start();

        registerHeadsetReceiver();

    }

//    public final Looper getCustomLooper() {
//        return mHandlerThread.getLooper();
//    }

    @Override
    public void onTrimMemory(int level) {
        Log.d(TAG, "onTrimMemory: do");
        super.onTrimMemory(level);
    }

    private void registerHeadsetReceiver() {
        headSetReceiver = new HeadSetReceiver();
        IntentFilter filter = new IntentFilter();

        //有线耳机
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        //监听蓝牙耳机
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        registerReceiver(headSetReceiver, filter);
    }

    public HeadSetReceiver getHeadSetReceiver() {
        return headSetReceiver;
    }
}
