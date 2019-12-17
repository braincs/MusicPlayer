package com.braincs.attrsc.musicplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Shuai
 * 13/12/2019.
 */
public class MusicPlayerService extends Service {
    private final static String TAG = MusicPlayerService.class.getSimpleName();
    private static MediaPlayer mediaPlayer;
    private Context mContext;
    private AudioManager audioManager;
    private PlayerBinder mBinder = new PlayerBinder(this);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "--onCreate--");

        mContext = this;
        initPlayer();
    }

    private void initPlayerSafely() {
        synchronized (TAG) {
            if (mediaPlayer == null) {
                synchronized (TAG) {
                    initPlayer();
                }
            }
        }
    }

    private void initPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        audioManager.abandonAudioFocus(focusChangeListener);

        mediaPlayer.setOnCompletionListener(completeListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "--onStartCommand--");
        initPlayerSafely();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "--onBind--");
        initPlayerSafely();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "--onUnbind--");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "--onDestroy--");

    }


    //region Listener
    AudioManager.OnAudioFocusChangeListener focusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "--onAudioFocusChange--: " + focusChange);
        }
    };

    private MediaPlayer.OnCompletionListener completeListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "--OnCompletionListener--");
        }
    };

    //endregion


    public void play(String mp3Path) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(mp3Path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        mediaPlayer.pause();
    }

    class PlayerBinder extends Binder {
        private MusicPlayerService service;

        public PlayerBinder(MusicPlayerService service) {
            this.service = service;
        }

        public MusicPlayerService getService() {
            return service;
        }
    }
}
