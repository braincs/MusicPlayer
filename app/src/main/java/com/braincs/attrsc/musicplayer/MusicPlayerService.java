package com.braincs.attrsc.musicplayer;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.braincs.attrsc.musicplayer.presenter.BasePresenter;
import com.braincs.attrsc.musicplayer.receiver.HeadSetReceiver;
import com.braincs.attrsc.musicplayer.utils.Constants;
import com.braincs.attrsc.musicplayer.utils.SpUtil;
import com.braincs.attrsc.musicplayer.view.NotificationView;

import java.io.File;
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
    private int currentPosition = 0;
    private int currentDuration = 0;
    private MServiceStateListener stateListener;
    private HandlerThread mHandlerThread;
    private Handler mWorkerHandler;
    private boolean isPlaying = false;
    private MediaSessionCompat mMediaSession;
    private BasePresenter mPresenter;
    private HeadSetReceiver headSetReceiver;
    private boolean isViewVisible;
    private NotificationView mNotificationView;

    //API21之前: 实现了一个 MediaButtonReceiver 获取监听
    public static class MMediaButtonReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "--onReceive-- ");

            if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                return;
            }

            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null || event.getAction() != KeyEvent.ACTION_UP) {
                return;
            }
            //do sth

            Log.d(TAG, "onReceive: " + intent.toString());

        }
    }

    //region Service lifecycle
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "--onCreate--");

        mContext = this;

        //notification bar
        initNotificationBar();

        initPlayer();

        registerHeadsetReceiver();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "--onStartCommand--");
        initAllSafely();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "--onBind--");
        initAllSafely();
        return mBinder;
    }

    /**
     * init all only first time effective
     */
    private void initAllSafely() {
        isViewVisible = true;

        // sync with cached model
        syncPlayerWithModel();

        // init MediaPlayer
        initPlayerSafely();

        // init WorkerHandler
        initHandler();

        // start UI update task
        if (mediaPlayer.isPlaying()) {
            startFreshUI();
        }

        // update notification
        updateNotificationView();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "--onUnbind--");

        // stop UI update
        isViewVisible = false;
        stopFreshUI();
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "--onDestroy--");

        pause();

        // unregister receiver
        unregisterReceiver(headSetReceiver);

        if (mPresenter != null) {
            mPresenter.onStop();
        }
        //Give up the audio focus.
        audioManager.abandonAudioFocus(focusChangeListener);

        releaseHanlder();
    }

    public class PlayerBinder extends Binder {
        private MusicPlayerService service;

        public PlayerBinder(MusicPlayerService service) {
            this.service = service;
        }

        public MusicPlayerService getService() {
            return service;
        }
    }
    //endregion

    //region Media
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

        mediaPlayer.setOnCompletionListener(completeListener);

        initMediaSession();
    }

    private void initMediaSession() {
        ComponentName mbr = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
        mMediaSession = new MediaSessionCompat(this, TAG, mbr, null);
        /* set flags to handle media buttons */
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // API21 之后 MediaSessionCompat.setCallback 是必须的
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                    return super.onMediaButtonEvent(intent);
                }

                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event == null || event.getAction() != KeyEvent.ACTION_UP) {
                    return super.onMediaButtonEvent(intent);
                }

                // do something

                //避免在Receiver里做长时间的处理，使得程序在CPU使用率过高的情况下出错，把信息发给handlera处理。
                int keyCode = event.getKeyCode();
                long eventTime = event.getEventTime() - event.getDownTime();//按键按下到松开的时长
                Message msg = Message.obtain();
                msg.what = 100;
                Bundle data = new Bundle();
                data.putInt("key_code", keyCode);
                data.putLong("event_time", eventTime);
                msg.setData(data);
                if (null != mWorkerHandler)
                    mWorkerHandler.sendMessage(msg);

                return true;
            }
        });

        /* to make sure the media session is active */
        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }
    }
    //endregion

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
            // fresh current state
            currentPosition = 0;
            currentIndex++;
            if (currentIndex >= musicList.size()) {
                currentIndex = 0;
            }
            // seekTo when stop will trigger onCompletion
            if (isPlaying)
                playList(musicList, currentIndex);
        }
    };

    //endregion


    @Deprecated
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

    private void startPlayerFreshUITask() {
        stopFreshUI();
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "File may not existed, please rescan files", Toast.LENGTH_SHORT).show();
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                seek(currentPosition);
                mediaPlayer.start();
                currentDuration = mediaPlayer.getDuration();
                cacheCurrentState();
                startFreshUI();
                isPlaying = true;
                updateNotificationView();
            }
        });
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getCurrentPosition() {
//        if (!isPlaying)return 0;
        return mediaPlayer.getCurrentPosition();
    }

    public int getTotalDuration() {
//        if (!isPlaying)return 0;
        return mediaPlayer.getDuration();
    }

    public void play(MusicPlayerModel model) {
        currentIndex = model.getCurrentIndex();
        currentPosition = model.getCurrentPosition();
        musicList = model.getMusicList();
        playList(musicList, currentIndex, currentPosition);
    }

    public void play() {
        MusicPlayerModel model = getCachedModel();
        currentIndex = model.getCurrentIndex();
        currentPosition = model.getCurrentPosition();
        musicList = model.getMusicList();
        playList(musicList, currentIndex, currentPosition);
    }

    public void setPresenter(BasePresenter presenter) {
        this.mPresenter = presenter;
    }

    public void pause() {
        if (isPlaying) {
            isPlaying = false;
            currentDuration = mediaPlayer.getDuration();
            mediaPlayer.pause();
            currentPosition = getCurrentPosition();

            //save to cache
            cacheCurrentState();

            updateNotificationView();
        }
    }

    public void queueEvent(Runnable runnable, long delayed) {
        mWorkerHandler.postDelayed(runnable, delayed);
    }

    public void updateMusicList(List<String> list) {
        musicList = list;
    }

    private void cacheCurrentState() {
//        Log.d(TAG, "cached currentPosition = " + currentPosition);
        MusicPlayerModel model = new MusicPlayerModel(
                isPlaying ? MusicPlayerModel.STATE_PLAYING : MusicPlayerModel.STATE_PAUSE,
                musicList,
                currentIndex,
                currentPosition,
                currentDuration);

        SpUtil.putObject(mContext, model);
    }

    private MusicPlayerModel getCachedModel() {
        return SpUtil.getObject(mContext, MusicPlayerModel.class);
    }

    private void playList(List<String> list, int index, int position) {
        if (list.size() <= 0 || index >= list.size())
            throw new Error("args error: index: " + index + ", list size: " + list.size());
        musicList = list;
        currentIndex = index;
        currentPosition = position;

        if (!updateDataSource(musicList.get(currentIndex))) return;
        startPlayerFreshUITask();
    }

    private void playList(List<String> list, int index) {
        playList(list, index, 0);
    }

    /**
     * start playing music
     * data sync from Model {@link MusicPlayerModel}
     */
    private void playList() {
        if (musicList == null || musicList.size() < 1) {
            // sync with model
            if (!syncPlayerWithModel()) return;
        }
        playList(musicList, currentIndex, currentPosition);
    }

    // 只在最初创建时候与模型同步
    private boolean syncPlayerWithModel() {
        MusicPlayerModel mModel = getCachedModel();
        if (mModel == null) return false;
        musicList = mModel.getMusicList();
        if (musicList.size() < 1) return false;
        currentIndex = mModel.getCurrentIndex();
        currentPosition = mModel.getCurrentPosition();
        return true;
    }


    public void setStateListener(MServiceStateListener stateListener) {
        this.stateListener = stateListener;
    }

    public void seek(int pos) {
        pos = Math.min(pos, mediaPlayer.getDuration());
        mediaPlayer.seekTo(pos);
    }

    private void startFreshUI() {
        if (mWorkerHandler != null && isViewVisible)
            mWorkerHandler.postDelayed(UIFreshRunnable, UI_FRESH_INTERVAL);
    }

    private void stopFreshUI() {
        if (mWorkerHandler != null) {
            Log.d(TAG, "--removeCallbacksAndMessages--");
            mWorkerHandler.removeCallbacks(UIFreshRunnable);
            mWorkerHandler.removeCallbacksAndMessages(null);
        }
    }

    private Runnable UIFreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (null != stateListener) {
                // pause 时，position = 0
//                if (isPlaying)
                currentPosition = getCurrentPosition();
                Log.d(TAG, "isPlaying: " + isPlaying + ", currentPosition: " + currentPosition + ", totalDuration: " + currentDuration);
                stateListener.onStateUpdate(isPlaying, currentPosition, currentDuration);
                // update current position
                if (musicList.size() > 0) {
                    stateListener.onCurrentMusic(currentIndex);

                    //set notification music name
                    mNotificationView.setMusicBarName(new File(musicList.get(currentIndex)).getName());

                } else {
                    stateListener.onCurrentMusic(-1);
                }

            }
            if (isPlaying)
                mWorkerHandler.postDelayed(this, UI_FRESH_INTERVAL);
        }
    };

    //region Headset
    private void registerHeadsetReceiver() {
        headSetReceiver = new HeadSetReceiver(this);
        IntentFilter filter = new IntentFilter();

        //有线耳机
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        //监听蓝牙耳机
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        registerReceiver(headSetReceiver, filter);
    }

    public void setHeadSetStatus(int status) {
        SpUtil.put(this, Constants.SP_KEY_HEADSET_STATUS, status);
    }

    public int getHeadSetStatus() {
        return (int) SpUtil.get(this, Constants.SP_KEY_HEADSET_STATUS, 0);
    }
    //endregion

    //region Notification
    private void initNotificationBar() {
        this.mNotificationView = new NotificationView(mContext);
        showNotification();
    }

    private void showNotification() {
        // show notification
        Notification notification = mNotificationView.displayNotification();
        startForeground(NotificationView.NOTIFICATION_ID, notification);

        updateNotificationView();
    }

    private void updateNotificationView() {
        if (musicList.size() == 0) {
            mNotificationView.setMusicBarName("");
        } else {
            mNotificationView.setMusicBarName(new File(musicList.get(currentIndex)).getName());
        }
        if (isPlaying) {
            mNotificationView.setMusicBtnPause();
        } else {
            mNotificationView.setMusicBtnPlay();
        }
    }
    //endregion

    //region WorkerHandler KeyEventHandler
    private void initHandler() {
        if (null == mHandlerThread) {
            mHandlerThread = new HandlerThread(TAG);
            mHandlerThread.start();
        }
        if (null == mWorkerHandler) {
            mWorkerHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    int what = msg.what;
                    switch (what) {
                        case 100:
                            Bundle data = msg.getData();
                            //按键值
                            int keyCode = data.getInt("key_code");
                            //按键时长
                            long eventTime = data.getLong("event_time");
                            //设置超过1000毫秒，就触发长按事件  //谷歌把超过1000s定义为长按。
                            boolean isLongPress = (eventTime > 1000);
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE://播放或暂停
                                case KeyEvent.KEYCODE_HEADSETHOOK://播放或暂停
                                    //playOrPause();
                                    Log.d(TAG, "--playOrPause--");
                                    if (isPlaying) {
                                        pause();
                                    } else {
                                        playList();
                                    }
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_PAUSE://播放或暂停
                                    if (isPlaying) {
                                        pause();
                                    }
                                    break;

                                case KeyEvent.KEYCODE_MEDIA_PLAY:
                                    if (!isPlaying) {
                                        playList();
                                    }
//                                //短按=播放下一首音乐，长按=音量加
//                                case KeyEvent.KEYCODE_MEDIA_NEXT:
//                                    if(isLongPress){
//                                        adjustVolume(true);//自定义
//                                    }else{
//                                        playNext();//自定义
//                                    }
//                                    break;
//                                //短按=播放上一首音乐，长按=音量减
//                                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
//                                    if(isLongPress){
//                                        adjustVolume(false);//自定义
//                                    }else{
//                                        playPrevious();//自定义
//                                    }
//                                    break;
                            }
                            break;
                        default://其他消息-则扔回上层处理
                            super.handleMessage(msg);
                    }
                }
            };
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
    //endregion

    public interface MServiceStateListener {
        void onStateUpdate(boolean isPlaying, int currentPosition, int totalDuration);

        void onCurrentMusic(int index);
    }
}
