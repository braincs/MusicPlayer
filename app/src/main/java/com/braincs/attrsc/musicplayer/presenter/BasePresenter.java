package com.braincs.attrsc.musicplayer.presenter;

/**
 * Created by Shuai
 * 27/12/2019.
 */
public interface BasePresenter {

    void onStart();

    void onResume();

    void onPause();

    void onStop();

    void onDestory();
}
