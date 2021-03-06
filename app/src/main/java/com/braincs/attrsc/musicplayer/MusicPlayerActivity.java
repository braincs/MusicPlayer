package com.braincs.attrsc.musicplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.braincs.attrsc.musicplayer.presenter.MusicPlayerPresenter;
import com.braincs.attrsc.musicplayer.utils.Constants;
import com.braincs.attrsc.musicplayer.utils.MarioResourceUtil;
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
    private RelativeLayout appLayout;
    private ImageButton btnPlayerPrevious;
    private ImageButton btnPlayerBack;
    private ImageButton btnPlayerForward;
    private ImageButton btnPlayerNext;
    private Toolbar toolbar;
    private RelativeLayout headView;
    private TextView headviewTitle;
    private ImageView ivAvatar;

//    private PendingIntent contentIntent;
//    private RemoteViews notificationView;
//    private NotificationCompat.Builder notificationBuilder;
//    private NotificationManager notificationManager;

    public static class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;
            if (intent.getAction().equalsIgnoreCase("PLAY_PAUSE")) {
                presenter.playControl();
            } else if (intent.getAction().equalsIgnoreCase("NEXT")) {
                presenter.next();
            } else if (intent.getAction().equalsIgnoreCase("PREVIOUS")) {
                presenter.previous();
            } else if (intent.getAction().equalsIgnoreCase("PLAYER_CLOSE")) {
                presenter.shutdown();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this; // theme 切换必须使用Activity的上下文
        updateCurrentTheme();
        setContentView(R.layout.activity_main);

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
        if (model == null) {
            model = new MusicPlayerModel( Constants.MUSIC_DIRECTORY);
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

    private void initModelAdapter() {
        smoothScroller = new LinearSmoothScroller(context) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        modelAdapter = new MusicPlayerModelAdapter(model, musicListOnClickListener);
        lvMusic.setAdapter(modelAdapter);
    }

    private void initView() {
        setToolBar();

        //background
        appLayout = findViewById(R.id.app_layout);

        //button
        btnPlayerPlay = findViewById(R.id.player_play);
        btnPlayerPrevious = findViewById(R.id.player_previous);
        btnPlayerBack = findViewById(R.id.player_back);
        btnPlayerForward = findViewById(R.id.player_forward);
        btnPlayerNext = findViewById(R.id.player_next);

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

        //avatar view
        View parent = navigationView.getHeaderView(0);
        headView = parent.findViewById(R.id.ll_headview);
        headviewTitle = parent.findViewById(R.id.tv_avatar_title);
        ivAvatar = parent.findViewById(R.id.iv_avatar);
    }

    private SwipeRefreshLayout.OnRefreshListener pullDownFreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            // Showing refresh animation before making http call
            presenter.scanMusic();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart();
    }

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
    protected void onStop() {
        super.onStop();
        presenter.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestory();
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
                if (selectedDuration[0] < 1) return;
                // start timer
                Toast.makeText(context, "stop after " + selectedDuration[0] + " mins", Toast.LENGTH_SHORT).show();
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
                case R.id.menu_theme:
                    presenter.swapTheme();
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
            presenter.updateSeekBarFromUser(seekBar.getProgress(), false);
            presenter.seekTo(seekBar.getProgress());

//            isSeekBarTouching = false;
        }
    };

    private MusicPlayerModelAdapter.OnItemClickListener musicListOnClickListener = new MusicPlayerModelAdapter.OnItemClickListener() {

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
                    presenter.scrollToCurrent(true);
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
                tvTime.setText(TimeUtil.milliSec2TimeStr(progress));
                String dur = TimeUtil.milliSec2TimeStr(total);
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
    public void setItems(final MusicPlayerModel model) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                modelAdapter.updateModel(model);
                modelAdapter.notifyDataSetChanged(); //fresh dataSet
            }
        });
    }

    @Override
    public void setFreshing(boolean isFreshing) {
        mSwipeRefreshLayout.setRefreshing(isFreshing);
    }

    @Override
    public void scrollTo(final int position, final boolean isSmooth) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecyclerView.LayoutManager layoutManager = lvMusic.getLayoutManager();

                if (isSmooth) {
                    smoothScroller.setTargetPosition(position);
                    if (layoutManager != null) {
                        layoutManager.startSmoothScroll(smoothScroller);
                    }
                } else {
                    if (layoutManager != null)
                        layoutManager.scrollToPosition(position);
                }
            }
        });
    }

    @Override
    public void updateTimerLeft(final String time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MenuItem timerItem = navigationView.getMenu().findItem(R.id.menu_timer);
                if (timerItem != null) {
                    timerItem.setTitle("Stop Timer   " + time);
                }
            }
        });

    }

    private void updateCurrentTheme() {
        switch ((int) SpUtil.get(context, Constants.SP_KEY_THEME_TAG, 1)) {
            case 1:
                setTheme(R.style.MusicPlayerTheme_Day);
                break;
            case -1:
                setTheme(R.style.MusicPlayerTheme_Night);
                break;
        }
        //recreate
    }


    private int getCurrentThemeButtonName() {
        switch ((int) SpUtil.get(context, Constants.SP_KEY_THEME_TAG, 1)) {
            case 1:
                return R.string.menu_theme_day;
            default:
                return R.string.menu_theme_night;
        }
    }

    @Override
    public void themeUpdate() {
        updateCurrentTheme();

        MarioResourceUtil helper = MarioResourceUtil.getInstance(getContext());
        helper.setBackgroundResourceByAttr(appLayout, R.attr.custom_attr_app_bg);
        helper.setBackgroundResourceByAttr(drawerLayout, R.attr.custom_attr_app_bg);
        helper.setBackgroundResourceByAttr(navigationView, R.attr.custom_attr_app_navigation_layout_bg);
        helper.setBackgroundResourceByAttr(headView, R.attr.custom_attr_app_navigation_head_view_bg);
        helper.setBackgroundResourceByAttr(ivAvatar, R.attr.custom_attr_app_navigation_layout_bg);
//        helper.setBackgroundResourceByAttr(mStatusBar, R.attr.custom_attr_app_title_layout_bg);
//        helper.setBackgroundResourceByAttr(mTitleLayout, R.attr.custom_attr_app_title_layout_bg);
//
//        helper.setBackgroundResourceByAttr(tvMusicName, R.attr.custom_attr_btn_bg);
//        helper.setTextColorByAttr(tvMusicName, R.attr.custom_attr_btn_text_color);
//        helper.setBackgroundResourceByAttr(mBtnTurnNight, R.attr.custom_attr_btn_bg);
//        helper.setTextColorByAttr(mBtnTurnNight, R.attr.custom_attr_btn_text_color);
//
        helper.setAlphaByAttr(btnPlayerPlay, R.attr.custom_attr_user_photo_alpha);
        helper.setAlphaByAttr(btnPlayerBack, R.attr.custom_attr_user_photo_alpha);
        helper.setAlphaByAttr(btnPlayerForward, R.attr.custom_attr_user_photo_alpha);
        helper.setAlphaByAttr(btnPlayerNext, R.attr.custom_attr_user_photo_alpha);
        helper.setAlphaByAttr(btnPlayerPrevious, R.attr.custom_attr_user_photo_alpha);
        helper.setAlphaByAttr(ivAvatar, R.attr.custom_attr_user_photo_alpha);
//
        helper.setTextColorByAttr(tvMusicName, R.attr.custom_attr_music_bar_text_color);
        helper.setTextColorByAttr(headviewTitle, R.attr.custom_color_head_view_title_text_color);
//        helper.setTextColorByAttr(mRemark, R.attr.custom_attr_remark_text_color);
//
        //update menu theme button
        Drawable menuTheme = helper.getDrawableByAttr(R.attr.custom_attr_menu_theme);
        MenuItem theme = navigationView.getMenu().findItem(R.id.menu_theme);
        theme.setIcon(menuTheme);
        theme.setTitle(getCurrentThemeButtonName());

        helper.setToolbarTextColorByAttr(toolbar, R.attr.custom_attr_music_bar_text_color);
    }

    private void setToolBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        ab.setHomeAsUpIndicator(R.drawable.toolbar_navigation_list);
        ab.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        toolbar.setOnClickListener(new View.OnClickListener() {
            long lastClickTime = -1L;
            @Override
            public void onClick(View v) {
                long clickTime = System.currentTimeMillis();
                if (lastClickTime != -1 && clickTime - lastClickTime < Constants.DOUBLE_CLICK_TIME_DELTA){
                    // double clicked
//                    Log.d(TAG, "--toolbar double clicked--");
                    presenter.scrollToTop(true);
                } else {
                    // single clicked

                }
                lastClickTime = clickTime;
            }
        });
    }
}
