package com.braincs.attrsc.musicplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.braincs.attrsc.musicplayer.presenter.MusicPlayerPresenter;
import com.braincs.attrsc.musicplayer.utils.SpUtil;
import com.braincs.attrsc.musicplayer.utils.TimeUtil;
import com.braincs.attrsc.musicplayer.view.MusicPlayerActivityView;

public class MusicPlayerActivity extends AppCompatActivity implements MusicPlayerActivityView {
    private final static String TAG = MusicPlayerActivity.class.getSimpleName();

    private Context context;
    private ImageButton btnPlayerPlay;
    private MusicPlayerModel model;
    private static MusicPlayerPresenter presenter;
    private TextView tvTime;
    private TextView tvDuration;
    private TextView tvMusicName;
    private SeekBar pbMusic;
    private RecyclerView lvMusic;
    private MusicPlayerModelAdapter modelAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private RecyclerView.SmoothScroller smoothScroller;
    private NotificationReceiver myReceiver;
//    private PendingIntent contentIntent;
//    private RemoteViews notificationView;
//    private NotificationCompat.Builder notificationBuilder;
//    private NotificationManager notificationManager;

    public static class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() ==null) return;
            if (intent.getAction().equalsIgnoreCase("PLAY_PAUSE")){
                presenter.playControl();
            }else if (intent.getAction().equalsIgnoreCase("NEXT")){
                presenter.next();
            }else if (intent.getAction().equalsIgnoreCase("PREVIOUS")){
                presenter.previous();
            }else if (intent.getAction().equalsIgnoreCase("PLAYER_CLOSE")){
                presenter.shutdown();
            }else if (intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)){
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d(TAG, "Headset is unplugged");
                        if (presenter.isPlaying())
                            presenter.pause();
                        break;
                    case 1:
                        Log.d(TAG, "Headset is plugged");
                        break;
                    default:
                        Log.d(TAG, "I have no idea what the headset state is");
                }
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this.getApplicationContext();

        getPermissions();

        initView();
//        mp3Files = MediaUtil.getAllMediaMp3Files();
//        Log.d(TAG, Arrays.toString(mp3Files.toArray()));
//        currentPos = 0;

        initModelPresenter();

        initModelAdapter();

        myReceiver = new NotificationReceiver();
    }

    private void initModelPresenter() {
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
        presenter = new MusicPlayerPresenter(MusicPlayerActivity.this, model);
        Log.d(TAG, model.toString());
    }

    private void initModelAdapter(){
        smoothScroller = new LinearSmoothScroller(context) {
            @Override protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
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
        tvMusicName.setOnClickListener(playerClickListener);

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

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        presenter.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
        presenter.onStop();
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
                presenter.stopAndFinish(selectedDuration[0]);
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
//                    Toast.makeText(context, "Timer is clicked!", Toast.LENGTH_SHORT).show();
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
//            Log.d(TAG, "--onProgressChanged--" +"progress = " + progress + ", fromuser = " + fromUser);
            presenter.updateSeekBarFromUser(progress, fromUser);

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
//            Log.d(TAG, "--onStartTrackingTouch--");
//            isSeekBarTouching = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "seek to: " + seekBar.getProgress());
            presenter.updateSeekBarFromUser(seekBar.getProgress(),false);
            presenter.seekTo(seekBar.getProgress());

//            isSeekBarTouching = false;
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

                case R.id.tv_music_name:
                    presenter.scrollToCurrent();
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
//                }
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

    @Override
    public void scrollTo(int position){
        RecyclerView.LayoutManager layoutManager = lvMusic.getLayoutManager();

        smoothScroller.setTargetPosition(position);
        if (layoutManager != null) {
            layoutManager.startSmoothScroll(smoothScroller);
        }
    }
}
