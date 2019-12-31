package com.braincs.attrsc.musicplayer.presenter;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.braincs.attrsc.musicplayer.MusicPlayerModel;
import com.braincs.attrsc.musicplayer.MusicPlayerService;
import com.braincs.attrsc.musicplayer.view.MusicPlayerActivityView;
import com.braincs.attrsc.musicplayer.view.MusicPlayerNotificationView;
import com.braincs.attrsc.musicplayer.utils.SpUtil;
import com.braincs.attrsc.musicplayer.view.NotificationView;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public class MusicPlayerPresenter implements BasePresenter{
    private static final String TAG = MusicPlayerPresenter.class.getSimpleName();
    private MusicPlayerActivityView mView;
    private MusicPlayerNotificationView mNotificationView;
    private MusicPlayerService mService;
    private MusicPlayerModel mModel;
    private boolean isBound = false;
    private volatile boolean isSeekBarFromUser;
    private Intent serviceIntent;
    private Timer timer;


    public MusicPlayerPresenter(MusicPlayerActivityView mView, MusicPlayerModel model) {
        this.mView = mView;
        this.mModel = model;
        this.mNotificationView = new NotificationView(mView.getContext());
        timer = new Timer("stopTimer");
    }

    private void syncUIwithModel() {
        if (mModel.getMusicList().size() == 0){
            mView.setMusicBarName("");
        }else {
            mView.setMusicBarName(new File(mModel.getMusicList().get(mModel.getCurrentIndex())).getName());
        }
        if (mModel.getState() == MusicPlayerModel.STATE_PLAYING){
            mView.setMusicBtnPause();
        }else {
            mView.setMusicBtnPlay();
        }
        mView.updateProgress(mModel.getCurrentPosition(), mModel.getTotalDuration());
        mView.setItems(mModel);

        freshNotificationUI();
    }

    private void freshNotificationUI(){
        if (mModel.getMusicList().size() == 0){
            mNotificationView.setMusicBarName("");
        }else {
            mNotificationView.setMusicBarName(new File(mModel.getMusicList().get(mModel.getCurrentIndex())).getName());
        }
        if (mModel.getState() == MusicPlayerModel.STATE_PLAYING){
            mNotificationView.setMusicBtnPause();
        }else {
            mNotificationView.setMusicBtnPlay();
        }
    }

    public void playList(int position){
        mModel.setCurrentIndex(position);
        play();
        syncUIwithModel();
    }

    public void playControl(){
//        mService.playControl();
        if (mModel.getState() == MusicPlayerModel.STATE_PLAYING){
            pause();
        }else {
            playAndSeek();
        }
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

    public void bindForegroundService(int id, Notification notification){
        mService.startForeground(id, notification);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "--onServiceConnected--");
            if (service != null) {
                mService = ((MusicPlayerService.PlayerBinder) service).getService();
                mService.setStateListener(mStateListener);

                Notification notification = mNotificationView.displayNotification();
                bindForegroundService(NotificationView.NOTIFICATION_ID, notification);

                isBound = true;
                // update ui
                syncUIwithModel();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    public void playAndSeek(){
        play();
        mService.seek(mModel.getCurrentPosition());
    }
    public void play(){
        updateControlState(true);
        mService.play(mModel);
    }
    public void pause(){
        updateControlState(false);
        mService.pause();
    }

    public void seekTo(int currentPosition){
        mModel.setCurrentPosition(currentPosition);
        mService.seek(currentPosition);
    }

    public void next(){
        mModel.next();
        mService.play(mModel);
        syncUIwithModel();
    }

    public void previous(){
        mModel.previous();
        mService.play(mModel);
        syncUIwithModel();
    }

    public void speedUp(){

    }

    public void speedDown(){

    }

    public void scrollToCurrent(){
        mView.scrollTo(mModel.getCurrentIndex());
    }

    public boolean isSeekBarFromUser() {
        return isSeekBarFromUser;
    }

    /**
     * update seekbar progress if fromUser
     * @param progress progress
     * @param fromUser is touched by user
     */
    public void updateSeekBarFromUser(int progress, boolean fromUser){
        isSeekBarFromUser = fromUser;
        if (isSeekBarFromUser) {
            mModel.setCurrentPosition(progress);
        }
    }

    private void updateControlState(boolean isPlaying){
        if (isPlaying){
            mModel.setState(MusicPlayerModel.STATE_PLAYING);
            mView.setMusicBtnPause();
            mNotificationView.setMusicBtnPause();
        }else {
            mModel.setState(MusicPlayerModel.STATE_PAUSE);
            mView.setMusicBtnPlay();
            mNotificationView.setMusicBtnPlay();
        }
    }

    private MusicPlayerService.MServiceStateListener mStateListener = new MusicPlayerService.MServiceStateListener() {
        @Override
        public void onStateUpdate(boolean isPlaying, int currentPosition, int totalDuration) {
//            Log.d(TAG, "isPlaying: " + isPlaying +", currentPosition: "+ currentPosition + ", totalDuration: "+totalDuration);
            updateControlState(isPlaying);

            if (!isPlaying) return; //paused position and duration may 0
            mModel.setCurrentPosition(currentPosition);
            mModel.setTotalDuration(totalDuration);
            SpUtil.putObject(mView.getContext(), mModel);
//            Log.d(TAG, "isSeekBarFromUser = " + isSeekBarFromUser);
            if (!isSeekBarFromUser) {
                mView.updateProgress(currentPosition, totalDuration);
            }
        }

        @Override
        public void onCurrentMusic(int index) {
            if (null == mModel || mModel.getMusicList() == null || mModel.getMusicList().size() < 1)return;
            // 列表播放时更新 index
            mModel.setCurrentIndex(index);
            File file = new File(mModel.getMusicList().get(index));
            mView.setMusicBarName(file.getName());
        }
    };

    public boolean isBound() {
        return isBound;
    }

    public void scanMusic() {
        mView.setFreshing(true);
        mModel.scanMusic();
        mView.setItems(mModel);
        mView.setFreshing(false);
    }

    public void stopAndFinish(int min){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                pause();
                unBindService();
                ((Activity)mView.getContext()).finish();
            }
        },min * 60 * 1000);
    }
    @Override
    public void onResume() {
        startService();
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStop() {
        unBindService();
    }
}