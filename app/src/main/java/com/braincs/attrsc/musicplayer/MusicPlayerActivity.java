package com.braincs.attrsc.musicplayer;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.braincs.attrsc.musicplayer.utils.TimeUtil;

import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity implements MusicPlayerView{
    private final static String TAG = MusicPlayerActivity.class.getSimpleName();

    private Intent intent;
    private MusicPlayerService mPlayer;
    private boolean isBound = false;
    private List<String> mp3Files;
    private int currentPos;
    private ImageButton btnPlayerPlay;
    private MusicPlayerModel model;
    private MusicPlayerPresenter presenter;
    private TextView tvTime;
    private TextView tvDuration;
    private TextView tvMusicName;
    private ProgressBar pbMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intent = new Intent(this, MusicPlayerService.class);

        getPermissions();
        startPlayer();
        initView();
//        mp3Files = MediaUtil.getAllMediaMp3Files();
//        Log.d(TAG, Arrays.toString(mp3Files.toArray()));
//        currentPos = 0;

        model = new MusicPlayerModel("Music");
    }


    private void initView() {
        //button
        btnPlayerPlay = findViewById(R.id.player_play);
        ImageButton btnPlayerPrevious = findViewById(R.id.player_previous);
        ImageButton btnPlayerBack = findViewById(R.id.player_back);
        ImageButton btnPlayerForward = findViewById(R.id.player_forward);
        ImageButton btnPlayerNext = findViewById(R.id.player_next);

        btnPlayerPlay.setOnClickListener(playerClickListener);
        btnPlayerPrevious.setOnClickListener(playerClickListener);
        btnPlayerBack.setOnClickListener(playerClickListener);
        btnPlayerForward.setOnClickListener(playerClickListener);
        btnPlayerNext.setOnClickListener(playerClickListener);

        //textview
        tvTime = findViewById(R.id.tv_music_curpos);
        tvDuration = findViewById(R.id.tv_music_duration);
        tvMusicName = findViewById(R.id.tv_music_name);

        //progress bar
        pbMusic = findViewById(R.id.pb_music);
    }

    private void startPlayer() {
        bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "--onServiceConnected--");
            if (service != null) {
                mPlayer = ((MusicPlayerService.PlayerBinder) service).getService();
                isBound = true;
                presenter = new MusicPlayerPresenter(MusicPlayerActivity.this, mPlayer, model);
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
        unBindService();
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

    public void unBindService() {
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
                    presenter.previous();
                    break;

                case R.id.player_back:
                    // slow down playpause speed

                    break;

                case R.id.player_play:
                    // playpause music / pause music
                    presenter.playpause();
                    break;

                case R.id.player_forward:
                    // speed up music

                    break;

                case R.id.player_next:
                    // next music
                    presenter.next();

                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void updateProgress(final int progress, final int total) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbMusic.setProgress(progress);
                pbMusic.setMax(total);
                tvTime.setText(TimeUtil.int2TimeStr(progress));
                String dur = TimeUtil.int2TimeStr(total);
                tvDuration.setText(dur);
            }
        });
    }

    @Override
    public void setMusicBtnPlay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPlayerPlay.setImageDrawable(getDrawable(R.drawable.play));
            }
        });
    }

    @Override
    public void setMusicBtnPause() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPlayerPlay.setImageDrawable(getDrawable(R.drawable.pause));
            }
        });
    }

    @Override
    public void setMusicBarName(final String name) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMusicName.setText(name);
            }
        });
    }
}
