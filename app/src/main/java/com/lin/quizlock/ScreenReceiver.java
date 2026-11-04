package com.lin.quizlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        Intent svc = new Intent(context, QuizLockService.class);
        switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_OFF:
                svc.setAction(QuizLockService.ACTION_SCREEN_OFF);
                context.startService(svc);
                break;
            case Intent.ACTION_SCREEN_ON:
            case Intent.ACTION_USER_PRESENT:
                svc.setAction(QuizLockService.ACTION_SCREEN_ON);
                context.startService(svc);
                break;
        }
    }
}
