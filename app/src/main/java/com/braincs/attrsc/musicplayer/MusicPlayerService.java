package com.braincs.attrsc.musicplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Shuai
 * 13/12/2019.
 */
public class MusicPlayerService extends Service {
    private final static String TAG = MusicPlayerService.class.getSimpleName();

    private static MediaPlayer mediaPlayer;
    private final static int UI_FRESH_INTERVAL = 1000;
    private Context mContext;
    private AudioManager audioManager;
    private PlayerBinder mBinder = new PlayerBinder(this);
    private List<String> musicList = new LinkedList<>();
    private int currentIndex = 0;
    private MServiceStateListener stateListener;
    private HandlerThread mHandlerThread;
    private Handler mWorkerHandler;
    private boolean isPlaying = false;



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
        //only init once
        Log.d(TAG, "--initPlayer--");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        audioManager.abandonAudioFocus(focusChangeListener);

        mediaPlayer.setOnCompletionListener(completeListener);
    }

    private void initHandler() {
        if (null == mHandlerThread) {
            mHandlerThread = new HandlerThread(TAG);
            mHandlerThread.start();
        }
        if (null == mWorkerHandler) {
            mWorkerHandler = new Handler(mHandlerThread.getLooper());
            mWorkerHandler.postDelayed(UIFreshRunnable, UI_FRESH_INTERVAL);
        }
    }

    private void releaseHanlder() {
        if (null != mWorkerHandler) {
            mWorkerHandler.removeCallbacksAndMessages(null);
            mWorkerHandler = null;
        }
        if (null != mHandlerThread) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
        // resetState mContext
        this.mContext = null;
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "--onStartCommand--");
        initPlayerSafely();
        initHandler();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "--onBind--");
        initPlayerSafely();
        initHandler();
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

        releaseHanlder();
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
            currentIndex++;
            if (currentIndex >= musicList.size()) {
                currentIndex = 0;
            }
            playList();
        }
    };

    //endregion


    public int play(String mp3Path) {
        try {
            // need to reset before setDataSource, otherwise IllegalStateException
            mediaPlayer.reset();
            mediaPlayer.setDataSource(mp3Path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            int duration = mediaPlayer.getDuration();
            Log.d(TAG, "duration :" + duration);
            return duration;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private boolean updateDataSource(String path) {
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(path);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void startPlayer() {
        try {
            mediaPlayer.prepare();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    if (mWorkerHandler != null) {
                        mWorkerHandler.postDelayed(UIFreshRunnable, UI_FRESH_INTERVAL);
                    }
                    isPlaying = true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCurrentPosition() {
        if (!isPlaying)return 0;
        return mediaPlayer.getCurrentPosition() /1000;
    }

    public int getTotalDuration(){
        if (!isPlaying)return 0;
        return mediaPlayer.getDuration() /1000;
    }

    public void pause() {
        if (isPlaying){
            isPlaying = false;
            mediaPlayer.pause();
        }
    }

    public void updateMusicList(List<String> list) {
        musicList = list;
    }

    private void playList() {
        if (musicList == null) return;
        if (!updateDataSource(musicList.get(currentIndex))) return;
        startPlayer();
    }

    public void playList(List<String> list, int index) {
        musicList = list;
        currentIndex = index;
        if (list.size() > 0 ) {
            if (!updateDataSource(musicList.get(currentIndex))) return;
            startPlayer();
        }
    }

    public void setStateListener(MServiceStateListener stateListener) {
        this.stateListener = stateListener;
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

    private Runnable UIFreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (null != stateListener) {
//                Log.d(TAG, "isPlaying: " + isPlaying + ", currentPosition: " + getCurrentPosition() + ", totalDuration: " + getTotalDuration());
                stateListener.onStateUpdate(isPlaying, getCurrentPosition(), getTotalDuration());
                if (musicList.size() > 0) {
                    stateListener.onCurrentMusic(musicList.get(currentIndex));
                }else {
                    stateListener.onCurrentMusic("");
                }

            }
            mWorkerHandler.postDelayed(this, UI_FRESH_INTERVAL);
        }
    };

    interface MServiceStateListener {
        void onStateUpdate(boolean isPlaying, int currentPosition, int totalDuration);
        void onCurrentMusic(String path);
    }
}
