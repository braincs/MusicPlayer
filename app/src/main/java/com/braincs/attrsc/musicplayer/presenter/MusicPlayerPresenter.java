package com.braincs.attrsc.musicplayer.presenter;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.braincs.attrsc.musicplayer.MusicPlayerModel;
import com.braincs.attrsc.musicplayer.MusicPlayerService;
import com.braincs.attrsc.musicplayer.utils.Constants;
import com.braincs.attrsc.musicplayer.utils.SpUtil;
import com.braincs.attrsc.musicplayer.utils.TimeUtil;
import com.braincs.attrsc.musicplayer.view.MusicPlayerActivityView;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public class MusicPlayerPresenter implements BasePresenter {
    private static final String TAG = MusicPlayerPresenter.class.getSimpleName();
    private MusicPlayerActivityView mView;
    private MusicPlayerService mService;
    private MusicPlayerModel mModel;
    private boolean isBound = false;
    private volatile boolean isSeekBarFromUser;
    private Intent serviceIntent;
    private Timer timer;


    public MusicPlayerPresenter(MusicPlayerActivityView mView, MusicPlayerModel model) {
        this.mView = mView;
        this.mModel = model;
        timer = new Timer("stopTimer");

    }

    private void syncUIwithModel() {
        if (mModel.getMusicList().size() == 0) {
            mView.setMusicBarName("");
        } else {
            mView.setMusicBarName(new File(mModel.getMusicList().get(mModel.getCurrentIndex())).getName());
        }
        if (mModel.getState() == MusicPlayerModel.STATE_PLAYING) {
            mView.setMusicBtnPause();
        } else {
            mView.setMusicBtnPlay();
        }
        mView.updateProgress(mModel.getCurrentPosition(), mModel.getTotalDuration());
        mView.setItems(mModel);

    }


    private void startService() {
        serviceIntent = new Intent(mView.getContext(), MusicPlayerService.class);
        mView.getContext().startService(serviceIntent);
        mView.getContext().bindService(serviceIntent, mConnection, Service.BIND_AUTO_CREATE);
    }

    private void unBindService() {
        if (isBound) {
            mView.getContext().unbindService(mConnection);
            isBound = false;
        }
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "--onServiceConnected--");
            if (service != null) {
                mService = ((MusicPlayerService.PlayerBinder) service).getService();
                mService.setStateListener(mStateListener);
                mService.setPresenter(MusicPlayerPresenter.this);

                isBound = true;
                // update ui
                syncUIwithModel();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            mService.setPresenter(null);
            mService.setStateListener(null);
        }
    };

    public void playList(int index) {
        // reset cached model
        mModel.setCurrentIndex(index);
        mModel.setCurrentPosition(0);
        SpUtil.putObject(mView.getContext(), mModel);

        play();
        syncUIwithModel();
    }

    public void playControl() {
//        mService.playControl();
        if (mModel.getState() == MusicPlayerModel.STATE_PLAYING) {
            pause();
        } else {
            play();
        }
    }

    /**
     * @deprecated playAndSeek: use {@link #play()}
     */
    @Deprecated
    public void playAndSeek() {
        play();
        mService.seek(mModel.getCurrentPosition());
    }

    /**
     * play: play music from cached model
     */
    public void play() {
        updateControlState(true);
        mService.play();
    }

    public void pause() {
        updateControlState(false);
        mService.pause();
    }

    public void seekTo(int currentPosition) {
        mModel.setCurrentPosition(currentPosition);
        mService.seek(currentPosition);
    }

    public void next() {
        // reset cached model
        mModel.next();
        SpUtil.putObject(mView.getContext(), mModel);

        // play
        mService.play();
        syncUIwithModel();
    }

    public void previous() {
        // reset cached model
        mModel.previous();
        SpUtil.putObject(mView.getContext(), mModel);

        //play
        mService.play();
        syncUIwithModel();
    }

    public void speedUp() {

    }

    public void speedDown() {

    }

    /**
     * manually shutdown
     */
    public void shutdown() {
        pause();
        if (null != mService && isBound) {
            mService.setStateListener(null);

        }
        onStop();

        if (null != mView && null != mView.getContext()) {
            mView.getContext().stopService(new Intent(mView.getContext(), MusicPlayerService.class));
            ActivityManager am = (ActivityManager) mView.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            am.killBackgroundProcesses(mView.getContext().getPackageName());
            System.exit(0);
        }


    }

    public void destroy(){
        // destroy activity and view
        if (null != mView && null != mView.getContext()) {
            mView.getContext().stopService(new Intent(mView.getContext(), MusicPlayerService.class));
            ActivityManager am = (ActivityManager) mView.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            am.killBackgroundProcesses(mView.getContext().getPackageName());
            ((Activity)mView.getContext()).finishAndRemoveTask();
        }
    }

    public void scrollToTop(boolean isSmooth) {
        mView.scrollTo(0, isSmooth);
    }

    public void scrollToCurrent(boolean isSmooth) {
        mView.scrollTo(mModel.getCurrentIndex(), isSmooth);
    }

    public boolean isSeekBarFromUser() {
        return isSeekBarFromUser;
    }

    /**
     * update seekbar progress if fromUser
     *
     * @param progress progress
     * @param fromUser is touched by user
     */
    public void updateSeekBarFromUser(int progress, boolean fromUser) {
        isSeekBarFromUser = fromUser;
        if (isSeekBarFromUser) {
            mModel.setCurrentPosition(progress);
            mView.updateProgress(progress, mModel.getTotalDuration());
        }
    }

    private void updateControlState(boolean isPlaying) {
        if (isPlaying) {
            mModel.setState(MusicPlayerModel.STATE_PLAYING);
            mView.setMusicBtnPause();
        } else {
            mModel.setState(MusicPlayerModel.STATE_PAUSE);
            mView.setMusicBtnPlay();
        }
    }

    private MusicPlayerService.MServiceStateListener mStateListener = new MusicPlayerService.MServiceStateListener() {
        @Override
        public void onStateUpdate(boolean isPlaying, int currentPosition, int totalDuration) {
//            Log.d(TAG, "isPlaying: " + isPlaying +", currentPosition: "+ currentPosition + ", totalDuration: "+totalDuration);
            updateControlState(isPlaying);

//            if (!isPlaying) return; //paused position and duration may 0
            mModel.setState(isPlaying ? MusicPlayerModel.STATE_PLAYING : MusicPlayerModel.STATE_PAUSE);
            mModel.setCurrentPosition(currentPosition);
            mModel.setTotalDuration(totalDuration);
//            SpUtil.putObject(mView.getContext(), mModel);
//            Log.d(TAG, "isSeekBarFromUser = " + isSeekBarFromUser);
            if (!isSeekBarFromUser) {
                mView.updateProgress(currentPosition, totalDuration);
            }
        }

        @Override
        public void onCurrentMusic(int index) {
            if (null == mModel || mModel.getMusicList() == null || mModel.getMusicList().size() < 1)
                return;
            // 列表播放时更新 index
            mModel.setCurrentIndex(index);
            File file = new File(mModel.getMusicList().get(index));
            mView.setMusicBarName(file.getName());
        }

        @Override
        public void onRemainTime(int milliseconds) {
            mView.updateTimerLeft(TimeUtil.milliSec2TimeStr(milliseconds));
        }
    };

    public boolean isBound() {
        return isBound;
    }

    public boolean isPlaying() {
        return isBound && mService.isPlaying();
    }

    public void scanMusic() {
        mView.setFreshing(true);
        mModel.scanMusic();
        mView.setItems(mModel);
        mView.setFreshing(false);
    }

    public void stopAndFinish(int min) {
        mService.countDownStop(min);
    }

    public void swapTheme() {
        int currentTag = (int) SpUtil.get(mView.getContext(), Constants.SP_KEY_THEME_TAG, 1);
        currentTag = 0 - currentTag;
        SpUtil.put(mView.getContext(), Constants.SP_KEY_THEME_TAG, currentTag);
        mView.themeUpdate();
    }


    @Override
    public void onStart() {
    }

    @Override
    public void onResume() {
        startService();
        //update UI
        scrollToCurrent(false);
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStop() {
        if (isBound)
            unBindService();
    }

    @Override
    public void onDestory() {
//        unregisterHeadsetReceiver();
    }
}
