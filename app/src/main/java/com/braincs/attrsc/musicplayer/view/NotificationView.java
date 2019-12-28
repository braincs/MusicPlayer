package com.braincs.attrsc.musicplayer.view;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.braincs.attrsc.musicplayer.MusicPlayerActivity;
import com.braincs.attrsc.musicplayer.MusicPlayerModel;
import com.braincs.attrsc.musicplayer.R;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Shuai
 * 28/12/2019.
 */
public class NotificationView implements MusicPlayerNotificationView {
    public final static String NOTIFICATION_CHANNEL_ID = "braincs.MusicPlayerService";
    public final static int NOTIFICATION_ID = 1;

    // service context
    private Context context;
    private PendingIntent contentIntent;
    private RemoteViews notificationView;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    public NotificationView(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationView = new RemoteViews(context.getPackageName(), R.layout.layout_music_notification_bar);
        contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MusicPlayerActivity.class), PendingIntent.FLAG_ONE_SHOT);
        Intent intentPlayPause = new Intent(context, MusicPlayerActivity.NotificationReceiver.class);
        intentPlayPause.setAction("PLAY_PAUSE");
        intentPlayPause.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingPlayPauseIntent = PendingIntent.getBroadcast(context, 0, intentPlayPause, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentNext = new Intent(context, MusicPlayerActivity.NotificationReceiver.class);
        intentNext.setAction("NEXT");
        intentNext.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(context, 1, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPrevious = new Intent(context, MusicPlayerActivity.NotificationReceiver.class);
        intentPrevious.setAction("PREVIOUS");
        intentPrevious.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingPreviousIntent = PendingIntent.getBroadcast(context, 2, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationView.setOnClickPendingIntent(R.id.noti_player_play, pendingPlayPauseIntent);
        notificationView.setOnClickPendingIntent(R.id.noti_player_previous, pendingPreviousIntent);
        notificationView.setOnClickPendingIntent(R.id.noti_player_next, pendingNextIntent);
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public void updateProgress(int progress, int total) {

    }

    @Override
    public void setMusicBtnPlay() {
        setControlBtnPlaying(false);
    }

    @Override
    public void setMusicBtnPause() {
        setControlBtnPlaying(true);
    }

    @Override
    public void setMusicBarName(String name) {
        // update notification bar
        notificationView.setTextViewText(R.id.tv_not_music_name, name);
        postInvalidate();
    }

    @Override
    public void setItems(MusicPlayerModel model) {

    }

    @Override
    public Notification displayNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            return startMyOwnForeground8_0();
        else
            return startMyOwnForeground();
    }

    private Notification startMyOwnForeground() {
        notificationBuilder = new NotificationCompat.Builder(context);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.player_icon)
                .setContentTitle("App is running in background")
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .setContent(notificationView)
                .build();
        return notification;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification startMyOwnForeground8_0() {
        notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.player_icon)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .setContent(notificationView)
                .build();
        return notification;
    }

    private void setControlBtnPlaying(boolean isPlaying) {
        notificationView.setImageViewResource(R.id.noti_player_play,
                isPlaying ? R.drawable.notification_pause : R.drawable.notification_play);
        postInvalidate();
    }

    private void postInvalidate() {
        notificationBuilder.setContent(notificationView);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}
