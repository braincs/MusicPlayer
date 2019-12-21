package com.braincs.attrsc.musicplayer;

import android.util.Log;

import java.io.File;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public class MusicPlayerPresenter {
    private static final String TAG = MusicPlayerPresenter.class.getSimpleName();
    private MusicPlayerView mView;
    private MusicPlayerService mService;
    private MusicPlayerModel mModel;

    public MusicPlayerPresenter(MusicPlayerView mView, MusicPlayerService mService, MusicPlayerModel model) {
        this.mView = mView;
        this.mService = mService;
        this.mModel = model;

        this.mService.setStateListener(mStateListener);
    }

    public void playpause(){
//        mService.playpause();
        if (mModel.getState() == MusicPlayerModel.STATE_PLAYING){
            mService.pause();
        }else {
            mService.playList(mModel.getMusicList(), mModel.getCurrentIndex());
        }
    }

    public void pause(){
        mService.pause();
    }

    public void seekTo(int currentPosition){
        mModel.setCurrentPosition(currentPosition);
        mService.seek(currentPosition);
    }

    public void next(){
        mService.playList(mModel.getMusicList(), mModel.next());
    }

    public void previous(){
        mService.playList(mModel.getMusicList(), mModel.previous());
    }

    public void speedUp(){

    }

    public void speedDown(){

    }

    private MusicPlayerService.MServiceStateListener mStateListener = new MusicPlayerService.MServiceStateListener() {
        @Override
        public void onStateUpdate(boolean isPlaying, int currentPosition, int totalDuration) {
//            Log.d(TAG, "isPlaying: " + isPlaying +", currentPosition: "+ currentPosition + ", totalDuration: "+totalDuration);
            if (isPlaying){
                mModel.setState(MusicPlayerModel.STATE_PLAYING);
                mView.setMusicBtnPause();
            }else {
                mModel.setState(MusicPlayerModel.STATE_IDLE);
                mView.setMusicBtnPlay();
            }

            mModel.setCurrentPosition(currentPosition);
            mModel.setTotalDuration(totalDuration);
            mView.updateProgress(currentPosition, totalDuration);
        }

        @Override
        public void onCurrentMusic(String path) {
            mView.setMusicBarName(new File(path).getName());
        }
    };
}
