package com.lin.quizlock;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Fullscreen quiz UI. Used instead of SYSTEM_ALERT_WINDOW overlay on
 * XGIMI / GMUI where appops rejects TYPE_APPLICATION_OVERLAY.
 */
public class QuizActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Question current;
    private TextView tvSubject;
    private TextView tvQuestion;
    private TextView tvFeedback;
    private Button[] buttons;
    private boolean finishingCorrect;
    private boolean notifiedDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.overlay_quiz);

        tvSubject = findViewById(R.id.tv_subject);
        tvQuestion = findViewById(R.id.tv_question);
        tvFeedback = findViewById(R.id.tv_feedback);
        buttons = new Button[]{
                findViewById(R.id.btn_opt0),
                findViewById(R.id.btn_opt1),
                findViewById(R.id.btn_opt2),
                findViewById(R.id.btn_opt3)
        };

        bindQuestion(QuestionBank.next());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!finishingCorrect) {
            bindQuestion(QuestionBank.next());
        }
    }

    private void bindQuestion(Question q) {
        current = q;
        if (q == null) return;
        tvSubject.setText(q.subject);
        tvQuestion.setText(q.prompt);
        tvFeedback.setVisibility(View.GONE);
        String[] labels = {"A. ", "B. ", "C. ", "D. "};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            buttons[i].setEnabled(true);
            buttons[i].setText(labels[i] + q.options[i]);
            buttons[i].setOnClickListener(v -> onAnswer(idx));
        }
        buttons[0].requestFocus();
    }

    private void onAnswer(int idx) {
        if (current == null || finishingCorrect) return;
        if (idx == current.correctIndex) {
            finishingCorrect = true;
            tvFeedback.setTextColor(getResources().getColor(R.color.correct_green));
            tvFeedback.setText(R.string.correct);
            tvFeedback.setVisibility(View.VISIBLE);
            for (Button b : buttons) b.setEnabled(false);
            handler.postDelayed(() -> {
                notifyDone();
                finish();
            }, 900);
        } else {
            tvFeedback.setTextColor(getResources().getColor(R.color.wrong_red));
            tvFeedback.setText(R.string.wrong);
            tvFeedback.setVisibility(View.VISIBLE);
            for (Button b : buttons) b.setEnabled(false);
            handler.postDelayed(() -> bindQuestion(QuestionBank.next()), 900);
        }
    }

    private void notifyDone() {
        if (notifiedDone) return;
        notifiedDone = true;
        Intent i = new Intent(this, QuizLockService.class);
        i.setAction(QuizLockService.ACTION_QUIZ_DONE);
        startService(i);
    }

    @Override
    public void onBackPressed() {
        // Block BACK — must answer correctly to leave
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        // If destroyed without a correct-answer finish (process kill / task clear),
        // clear the service flag so a later tick can show again.
        if (!isChangingConfigurations()) {
            notifyDone();
        }
        super.onDestroy();
    }
}
