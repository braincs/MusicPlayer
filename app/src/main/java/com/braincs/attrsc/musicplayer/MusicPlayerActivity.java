package com.braincs.attrsc.musicplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.braincs.attrsc.musicplayer.utils.SpUtil;
import com.braincs.attrsc.musicplayer.utils.TimeUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MusicPlayerActivity extends AppCompatActivity implements MusicPlayerView{
    private final static String TAG = MusicPlayerActivity.class.getSimpleName();

    private Context context;
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
    private SeekBar pbMusic;
    private RecyclerView lvMusic;
    private MusicPlayerModelAdapter modelAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this.getApplicationContext();
        intent = new Intent(this, MusicPlayerService.class);
        timer = new Timer("stopTimer");

        getPermissions();
        startService();

        initView();
//        mp3Files = MediaUtil.getAllMediaMp3Files();
//        Log.d(TAG, Arrays.toString(mp3Files.toArray()));
//        currentPos = 0;

        initModel();

        initModelAdapter();
    }

    private void initModel() {
        model = SpUtil.getObject(context, MusicPlayerModel.class);
        if (model == null){
            model = new MusicPlayerModel("Music");
            /**
             * Showing Swipe Refresh animation on activity create
             * As animation won't start on onCreate, post runnable is used
             */
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    presenter.scanMusic();
                }
            });
        }
        Log.d(TAG, model.toString());
    }

    private void initModelAdapter(){
        modelAdapter = new MusicPlayerModelAdapter(model, musicListOnClickListener);
        lvMusic.setAdapter(modelAdapter);
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
        pbMusic.setOnSeekBarChangeListener(onSeekBarChangeListener);

        //listView
        lvMusic = findViewById(R.id.lv_music);
        mSwipeRefreshLayout = findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(pullDownFreshListener);

        //drawer view
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.getHeaderView(0).setOnClickListener(drawerHeaderViewOnClickListener);
        navigationView.setNavigationItemSelectedListener(navigationItemSelectedListener);
    }

    private SwipeRefreshLayout.OnRefreshListener pullDownFreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            // Showing refresh animation before making http call
            presenter.scanMusic();
        }
    };

    private void startService() {
        startService(intent);
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

    private void displayTimerSelector() {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(MusicPlayerActivity.this);
        builder.setTitle("Stop timer");

        // add a radio button list
        final String[] durations = {"none", "10 mins", "20 mins", "30 mins", "45 mins", "60 mins"};
        int checkedItem = 0; // none
        final int[] selectedDuration = {0};
        builder.setSingleChoiceItems(durations, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user checked an item
                if (which == 0) return;
                String durationStr = durations[which].split(" ")[0];
                selectedDuration[0] = Integer.parseInt(durationStr);
                Log.d(TAG, "selected duration = " + selectedDuration[0]);
            }
        });

        // add OK and Cancel buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user clicked OK
                // start timer
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        presenter.pause();
                        unBindService();
                        finish();
                    }
                },selectedDuration[0] * 60 * 1000);
            }
        });
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //region Click Listener
    private View.OnClickListener drawerHeaderViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            drawerLayout.closeDrawer(navigationView);
            Toast.makeText(context, "Header view onclick", Toast.LENGTH_SHORT).show();
        }
    };

    private NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_local_music:
                    Toast.makeText(context, "Home is clicked!", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.menu_settings:
                    Toast.makeText(context, "Settings is clicked!", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.menu_timer:
                    Toast.makeText(context, "Timer is clicked!", Toast.LENGTH_SHORT).show();
                    displayTimerSelector();
                    break;
                case R.id.menu_share:
                    Toast.makeText(context, "Share is clicked!", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.menu_about:
                    Toast.makeText(context, "About is clicked!", Toast.LENGTH_SHORT).show();
                    break;
            }
            drawerLayout.closeDrawer(navigationView);
            return false;
        }
    };



    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "seek to: " + seekBar.getProgress());
            presenter.seekTo(seekBar.getProgress());
        }
    };

    private MusicPlayerModelAdapter.OnItemClickListener musicListOnClickListener = new MusicPlayerModelAdapter.OnItemClickListener(){

        @Override
        public void onItemClick(View view, int position) {
            presenter.playList(position);
        }
    };

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
                    // playControl music / pause music
                    presenter.playControl();
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
    //endregion

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void updateProgress(final int progress, final int total) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbMusic.setProgress(0); // call these two methods before setting progress.
                pbMusic.setMax(total);
                pbMusic.setProgress(progress);
                pbMusic.refreshDrawableState();
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

    @Override
    public void setItems(MusicPlayerModel model) {
        modelAdapter.updateModel(model);
        modelAdapter.notifyDataSetChanged(); //fresh dataSet
    }

    @Override
    public void setFreshing(boolean isFreshing) {
        mSwipeRefreshLayout.setRefreshing(isFreshing);
    }
}
