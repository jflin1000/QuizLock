package com.lin.quizlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || context == null) return;
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)
                || "android.intent.action.QUICKBOOT_POWERON".equals(a)) {
            final PendingResult pending = goAsync();
            try {
                if (Prefs.isEnabled(context)) {
                    QuizLockService.start(context);
                }
                BankFetcher.kick(context, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pending.finish();
                        } catch (Exception ignored) {
                        }
                    }
                }, false);
            } catch (Exception e) {
                try {
                    pending.finish();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
