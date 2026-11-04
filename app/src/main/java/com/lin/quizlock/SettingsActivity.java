package com.lin.quizlock;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        CheckBox cb = findViewById(R.id.cb_enabled);
        EditText etInterval = findViewById(R.id.et_interval);
        EditText etPin = findViewById(R.id.et_new_pin);
        Button btnSave = findViewById(R.id.btn_save);
        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.btn_stop);
        TextView tvSaved = findViewById(R.id.tv_saved);

        cb.setChecked(Prefs.isEnabled(this));
        etInterval.setText(String.valueOf(Prefs.getIntervalMinutes(this)));

        btnSave.setOnClickListener(v -> {
            Prefs.setEnabled(this, cb.isChecked());
            try {
                int m = Integer.parseInt(etInterval.getText().toString().trim());
                Prefs.setIntervalMinutes(this, m);
            } catch (Exception e) {
                Toast.makeText(this, "间隔请填写数字", Toast.LENGTH_SHORT).show();
                return;
            }
            String np = etPin.getText().toString().trim();
            if (!np.isEmpty()) {
                if (np.length() != 4) {
                    Toast.makeText(this, "密码须为4位数字", Toast.LENGTH_SHORT).show();
                    return;
                }
                Prefs.setPin(this, np);
            }
            tvSaved.setVisibility(TextView.VISIBLE);
            if (Prefs.isEnabled(this)) {
                QuizLockService.start(this);
            } else {
                QuizLockService.stop(this);
            }
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        });

        btnStart.setOnClickListener(v -> {
            Prefs.setEnabled(this, true);
            cb.setChecked(true);
            QuizLockService.start(this);
            Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
        });

        btnStop.setOnClickListener(v -> {
            Prefs.setEnabled(this, false);
            cb.setChecked(false);
            QuizLockService.stop(this);
            Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show();
        });

        btnSave.requestFocus();
    }
}
