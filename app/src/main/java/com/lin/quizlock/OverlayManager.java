package com.lin.quizlock;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class OverlayManager {
    private final Context appContext;
    private final WindowManager windowManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private View overlayView;
    private Question current;
    private Runnable onDismissed;

    public OverlayManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
    }

    public boolean isShowing() {
        return overlayView != null;
    }

    public void show(Question question, Runnable onDismissed) {
        mainHandler.post(() -> showInternal(question, onDismissed));
    }

    private void showInternal(Question question, Runnable onDismissed) {
        this.onDismissed = onDismissed;
        if (overlayView != null) {
            bindQuestion(question);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(appContext);
        overlayView = inflater.inflate(R.layout.overlay_quiz, null);
        overlayView.setFocusableInTouchMode(true);
        overlayView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true; // block BACK
            }
            return false;
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.CENTER;
        // Keep focusable so D-pad works (do NOT set FLAG_NOT_FOCUSABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        try {
            windowManager.addView(overlayView, lp);
            bindQuestion(question);
            overlayView.post(() -> {
                Button b0 = overlayView.findViewById(R.id.btn_opt0);
                if (b0 != null) b0.requestFocus();
            });
        } catch (Exception e) {
            overlayView = null;
            e.printStackTrace();
        }
    }

    private int overlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        //noinspection deprecation
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void bindQuestion(Question q) {
        current = q;
        if (overlayView == null) return;
        TextView subject = overlayView.findViewById(R.id.tv_subject);
        TextView prompt = overlayView.findViewById(R.id.tv_question);
        TextView feedback = overlayView.findViewById(R.id.tv_feedback);
        Button[] buttons = new Button[]{
                overlayView.findViewById(R.id.btn_opt0),
                overlayView.findViewById(R.id.btn_opt1),
                overlayView.findViewById(R.id.btn_opt2),
                overlayView.findViewById(R.id.btn_opt3)
        };
        subject.setText(q.subject);
        prompt.setText(q.prompt);
        feedback.setVisibility(View.GONE);
        String[] labels = {"A. ", "B. ", "C. ", "D. "};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            buttons[i].setText(labels[i] + q.options[i]);
            buttons[i].setOnClickListener(v -> onAnswer(idx, feedback, buttons));
        }
        buttons[0].requestFocus();
    }

    private void onAnswer(int idx, TextView feedback, Button[] buttons) {
        if (current == null) return;
        if (idx == current.correctIndex) {
            feedback.setTextColor(appContext.getResources().getColor(R.color.correct_green));
            feedback.setText(R.string.correct);
            feedback.setVisibility(View.VISIBLE);
            for (Button b : buttons) b.setEnabled(false);
            mainHandler.postDelayed(this::dismiss, 900);
        } else {
            feedback.setTextColor(appContext.getResources().getColor(R.color.wrong_red));
            feedback.setText(R.string.wrong);
            feedback.setVisibility(View.VISIBLE);
            for (Button b : buttons) b.setEnabled(false);
            mainHandler.postDelayed(() -> {
                for (Button b : buttons) b.setEnabled(true);
                bindQuestion(QuestionBank.next());
            }, 900);
        }
    }

    public void dismiss() {
        mainHandler.post(() -> {
            if (overlayView != null) {
                try {
                    windowManager.removeView(overlayView);
                } catch (Exception ignored) {
                }
                overlayView = null;
            }
            Runnable cb = onDismissed;
            onDismissed = null;
            if (cb != null) cb.run();
        });
    }
}
