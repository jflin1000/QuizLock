package com.lin.quizlock;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tv_status);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnTest = findViewById(R.id.btn_test);

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, PinActivity.class)));
        btnTest.setOnClickListener(v -> {
            Intent i = new Intent(this, QuizLockService.class);
            i.setAction(QuizLockService.ACTION_SHOW_NOW);
            startService(i);
            Toast.makeText(this, "正在出题…", Toast.LENGTH_SHORT).show();
        });

        BankRepository.ensureLoaded(this);
        BankFetcher.kick(this, () -> runOnUiThread(() -> {
            if (!isDestroyed()) updateStatus();
        }), false);
        if (Prefs.isEnabled(this)) {
            QuizLockService.start(this);
        }
        btnSettings.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        boolean en = Prefs.isEnabled(this);
        int min = Prefs.getIntervalMinutes(this);
        String net = Prefs.getBankStatus(this);
        tvStatus.setText("弹题：" + (en ? "已启用" : "已关闭")
                + "　间隔：" + min + " 分钟\n出题方式：全屏答题页（无需悬浮窗权限）\n"
                + "题库：" + BankRepository.summary()
                + (net.length() == 0 ? "" : "\n" + net));
    }
}
