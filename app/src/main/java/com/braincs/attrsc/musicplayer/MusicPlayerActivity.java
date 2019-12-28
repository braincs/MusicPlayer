package com.braincs.attrsc.musicplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.braincs.attrsc.musicplayer.presenter.MusicPlayerPresenter;
import com.braincs.attrsc.musicplayer.utils.SpUtil;
import com.braincs.attrsc.musicplayer.utils.TimeUtil;
import com.braincs.attrsc.musicplayer.view.MusicPlayerActivityView;
import com.braincs.attrsc.musicplayer.view.MusicPlayerView;

public class MusicPlayerActivity extends AppCompatActivity implements MusicPlayerActivityView {
    private final static String TAG = MusicPlayerActivity.class.getSimpleName();
    private final static String NOTIFICATION_CHANNEL_ID = "braincs.MusicPlayerService";
    private final static int NOTIFICATION_ID = 1;

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
    private PendingIntent contentIntent;
    private RemoteViews notificationView;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

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

    @Override
    protected void onResume() {
        super.onResume();
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

    //region Notification
    private void initNotification(){
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationView = new RemoteViews(getPackageName(), R.layout.layout_music_notification_bar);
        contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(context, MusicPlayerActivity.class), PendingIntent.FLAG_ONE_SHOT);
        Intent intentPlayPause = new Intent(this, MusicPlayerActivity.NotificationReceiver.class);
        intentPlayPause.setAction("PLAY_PAUSE");
        intentPlayPause.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingPlayPauseIntent = PendingIntent.getBroadcast(this, 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentNext = new Intent(this, MusicPlayerActivity.NotificationReceiver.class);
        intentNext.setAction("NEXT");
        intentNext.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(this, 1, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPrevious = new Intent(this, MusicPlayerActivity.NotificationReceiver.class);
        intentPrevious.setAction("PREVIOUS");
        intentPrevious.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingPreviousIntent = PendingIntent.getBroadcast(this, 2, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationView.setOnClickPendingIntent(R.id.noti_player_play, pendingPlayPauseIntent);
        notificationView.setOnClickPendingIntent(R.id.noti_player_previous, pendingPreviousIntent);
        notificationView.setOnClickPendingIntent(R.id.noti_player_next, pendingNextIntent);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "MusicPlayerService", NotificationManager.IMPORTANCE_NONE);
//            chan.setLightColor(Color.BLUE);
//            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            assert manager != null;
//            manager.createNotificationChannel(chan);
//        }
    }
//
//    private void startMyOwnForeground(){
//        notificationBuilder = new NotificationCompat.Builder(this);
//        Notification notification = notificationBuilder.setOngoing(true)
//                .setSmallIcon(R.drawable.player_icon)
//                .setContentTitle("App is running in background")
//                .setCategory(Notification.CATEGORY_SERVICE)
//                .setContentIntent(contentIntent)
//                .setContent(notificationView)
//                .build();
//        presenter.bindForegroundService(NOTIFICATION_ID, notification);
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.O)
//    private void startMyOwnForeground8_0(){
//        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
//        Notification notification = notificationBuilder.setOngoing(true)
//                .setSmallIcon(R.drawable.player_icon)
//                .setContentTitle("App is running in background")
//                .setPriority(NotificationManager.IMPORTANCE_MAX)
//                .setCategory(Notification.CATEGORY_SERVICE)
//                .setContentIntent(contentIntent)
//                .setContent(notificationView)
//                .build();
//        presenter.bindForegroundService(NOTIFICATION_ID, notification);
//    }
    //endregion

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
}
