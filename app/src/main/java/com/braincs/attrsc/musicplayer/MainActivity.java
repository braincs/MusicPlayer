package com.braincs.attrsc.musicplayer;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.braincs.attrsc.musicplayer.utils.MediaUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private Intent intent;
    private MusicPlayerService mPlayer;
    private boolean isBound = false;
    private List<String> mp3Files;
    private int currentPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intent = new Intent(this, MusicPlayerService.class);

        getPermissions();
        startPlayer();
        initView();
        mp3Files = MediaUtil.getAllMediaMp3Files();
        Log.d(TAG, Arrays.toString(mp3Files.toArray()));
        currentPos = 0;
//        String str = "img_RGB_LiveFailure_2019-09-12_16_25_55.jpg";
//        String[] split = str.split("\\.");
//        Log.d(TAG, Arrays.toString(split));

    }


    private void initView() {
        ImageButton btnPlayerPlay = findViewById(R.id.player_play);
        ImageButton btnPlayerPrevious = findViewById(R.id.player_previous);
        ImageButton btnPlayerBack = findViewById(R.id.player_back);
        ImageButton btnPlayerForward = findViewById(R.id.player_forward);
        ImageButton btnPlayerNext = findViewById(R.id.player_next);

        btnPlayerPlay.setOnClickListener(playerClickListener);
        btnPlayerPrevious.setOnClickListener(playerClickListener);
        btnPlayerBack.setOnClickListener(playerClickListener);
        btnPlayerForward.setOnClickListener(playerClickListener);
        btnPlayerNext.setOnClickListener(playerClickListener);
    }

    private void startPlayer() {
        bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mPlayer = ((MusicPlayerService.PlayerBinder) service).getService();
                isBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unBindService(null);
    }

    private void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
//                    || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    ) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.CAMERA
                }, 0);
            }
        }
    }

    public void play(String path) {
        Log.d(TAG, "play");
        mPlayer.play(path);
    }

    public void pause() {
        mPlayer.pause();
    }

    public void next() {
        currentPos++;
        if (currentPos >= mp3Files.size()) {
            currentPos = 0;
        }
        mPlayer.play(mp3Files.get(currentPos));
    }

    public void previous() {
        currentPos--;
        if (currentPos < 0) {
            currentPos = mp3Files.size() - 1;
        }
        mPlayer.play(mp3Files.get(currentPos));
    }

    public void unBindService(View view) {
        if (isBound) {
            unbindService(mConnection);
            isBound = false;
        }
    }

    private View.OnClickListener playerClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.player_previous:
                    // previous music
                    previous();
                    break;
                case R.id.player_back:
                    // slow down play speed

                    break;
                case R.id.player_play:
                    // play music / pause music
                    play(mp3Files.get(currentPos));
                    break;
                case R.id.player_forward:
                    // speed up music

                    break;
                case R.id.player_next:
                    // next music
                    next();

                    break;
                default:
                    break;
            }
        }
    };
}
