package com.braincs.attrsc.musicplayer;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public class MusicPlayerPresenter {
    private MusicPlayerView mView;
    private MusicPlayerService mService;
    private MusicPlayerModel mModel;

    public MusicPlayerPresenter(MusicPlayerView mView, MusicPlayerService mService, MusicPlayerModel model) {
        this.mView = mView;
        this.mService = mService;
        this.mModel = model;
    }

    public void play(){
//        mService.play();
        mService.playList(mModel.getMusicList(), mModel.getCurrentIndex());
        mView.setMusicBtnPause();
    }

    public void pause(){
        mService.pause();
        mView.setMusicBtnPlay();
    }

    public void seekTo(int pos){

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
}
