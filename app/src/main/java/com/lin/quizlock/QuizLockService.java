package com.lin.quizlock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

public class QuizLockService extends Service {
    public static final String ACTION_SHOW_NOW = "com.lin.quizlock.SHOW_NOW";
    public static final String ACTION_SCREEN_OFF = "com.lin.quizlock.SCREEN_OFF";
    public static final String ACTION_SCREEN_ON = "com.lin.quizlock.SCREEN_ON";
    public static final String ACTION_RELOAD = "com.lin.quizlock.RELOAD";
    public static final String ACTION_QUIZ_DONE = "com.lin.quizlock.QUIZ_DONE";

    private static final int NOTI_ID = 1001;
    private static final String CHANNEL_ID = "quizlock_fg";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScreenReceiver screenReceiver;
    private boolean screenOn = true;
    private boolean quizShowing = false;
    private long intervalMs = Prefs.DEFAULT_INTERVAL_MIN * 60_000L;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!Prefs.isEnabled(QuizLockService.this)) {
                stopSelf();
                return;
            }
            if (screenOn && !quizShowing) {
                showQuiz();
            }
            // next schedule only after dismiss; if still showing, wait and recheck
            if (!quizShowing) {
                scheduleNext();
            }
        }
    };

    public static void start(Context context) {
        Intent i = new Intent(context, QuizLockService.class);
        i.setAction(ACTION_RELOAD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, QuizLockService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTI_ID, buildNotification());
        screenReceiver = new ScreenReceiver();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, f);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                screenOn = pm.isInteractive();
            } else {
                //noinspection deprecation
                screenOn = pm.isScreenOn();
            }
        }
        reloadInterval();
        scheduleNext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTI_ID, buildNotification());
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_SHOW_NOW:
                    showQuiz();
                    break;
                case ACTION_QUIZ_DONE:
                    onQuizDone();
                    break;
                case ACTION_SCREEN_OFF:
                    screenOn = false;
                    handler.removeCallbacks(tick);
                    break;
                case ACTION_SCREEN_ON:
                    screenOn = true;
                    if (!quizShowing) scheduleNext();
                    break;
                case ACTION_RELOAD:
                    reloadInterval();
                    if (!quizShowing) scheduleNext();
                    break;
            }
        }
        return START_STICKY;
    }

    private void reloadInterval() {
        intervalMs = Prefs.getIntervalMinutes(this) * 60_000L;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTI_ID, buildNotification());
    }

    private void scheduleNext() {
        handler.removeCallbacks(tick);
        if (!Prefs.isEnabled(this)) return;
        handler.postDelayed(tick, intervalMs);
    }

    private void showQuiz() {
        if (quizShowing) return;
        quizShowing = true;
        handler.removeCallbacks(tick);
        Intent i = new Intent(this, QuizActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(i);
        } catch (Exception e) {
            quizShowing = false;
            e.printStackTrace();
            scheduleNext();
        }
    }

    private void onQuizDone() {
        quizShowing = false;
        scheduleNext();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.service_channel),
                    NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription("保持学习锁屏计时运行");
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, open,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );
        int min = Prefs.getIntervalMinutes(this);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_running, min))
                .setSmallIcon(R.drawable.ic_stat)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
