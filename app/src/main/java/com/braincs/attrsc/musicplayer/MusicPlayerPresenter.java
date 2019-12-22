package com.braincs.attrsc.musicplayer;

import com.braincs.attrsc.musicplayer.utils.SpUtil;

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
        syncUIwithModel();
    }

    private void syncUIwithModel() {
        mView.setMusicBarName(new File(mModel.getMusicList().get(mModel.getCurrentIndex())).getName());
        if (mModel.getState() == MusicPlayerModel.STATE_PLAYING){
            mView.setMusicBtnPause();
        }else {
            mView.setMusicBtnPlay();
        }
        mView.updateProgress(mModel.getCurrentPosition(), mModel.getTotalDuration());
        mView.setItems(mModel);
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

    public void playAndSeek(){
        play();
        mService.seek(mModel.getCurrentPosition());
    }
    public void play(){
        updateControlState(true);
        mService.playList(mModel.getMusicList(), mModel.getCurrentIndex());
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
        mService.playList(mModel.getMusicList(), mModel.next());
        syncUIwithModel();
    }

    public void previous(){
        mService.playList(mModel.getMusicList(), mModel.previous());
        syncUIwithModel();
    }

    public void speedUp(){

    }

    public void speedDown(){

    }

    private void updateControlState(boolean isPlaying){
        if (isPlaying){
            mModel.setState(MusicPlayerModel.STATE_PLAYING);
            mView.setMusicBtnPause();
        }else {
            mModel.setState(MusicPlayerModel.STATE_PAUSE);
            mView.setMusicBtnPlay();
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
            mView.updateProgress(currentPosition, totalDuration);
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
}
