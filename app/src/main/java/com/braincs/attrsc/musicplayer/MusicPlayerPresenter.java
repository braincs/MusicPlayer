package com.braincs.attrsc.musicplayer;

/**
 * Created by Shuai
 * 17/12/2019.
 */
public class MusicPlayerPresenter {
    private MusicPlayerView mView;
    private MusicPlayerService playerService;

    public MusicPlayerPresenter(MusicPlayerView mView, MusicPlayerService playerService) {
        this.mView = mView;
        this.playerService = playerService;
    }

    public void play(){
//        playerService.play();
    }

    public void pause(){
        playerService.pause();
    }

    public void seekTo(int pos){

    }

    public void next(){

    }

    public void previous(){

    }

    public void speedUp(){

    }

    public void speedDown(){

    }
}
