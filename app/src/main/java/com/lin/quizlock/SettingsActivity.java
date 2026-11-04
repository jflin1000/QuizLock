package com.lin.quizlock;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private EditText etBank;
    private TextView tvBank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        CheckBox cb = findViewById(R.id.cb_enabled);
        EditText etInterval = findViewById(R.id.et_interval);
        EditText etPin = findViewById(R.id.et_new_pin);
        etBank = findViewById(R.id.et_bank_url);
        tvBank = findViewById(R.id.tv_bank_status);
        Button btnSave = findViewById(R.id.btn_save);
        Button btnRefresh = findViewById(R.id.btn_refresh_banks);
        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.btn_stop);
        TextView tvSaved = findViewById(R.id.tv_saved);

        cb.setChecked(Prefs.isEnabled(this));
        etInterval.setText(String.valueOf(Prefs.getIntervalMinutes(this)));
        etBank.setText(Prefs.getBankBaseUrl(this));
        etBank.setHint(Prefs.DEFAULT_BANK_BASE_URL);
        BankRepository.ensureLoaded(this);
        showBankStatus();

        btnSave.setOnClickListener(v -> {
            final int minutes;
            try {
                minutes = Integer.parseInt(etInterval.getText().toString().trim());
            } catch (Exception e) {
                Toast.makeText(this, "间隔请填写数字", Toast.LENGTH_SHORT).show();
                return;
            }
            String np = etPin.getText().toString().trim();
            if (!np.isEmpty() && np.length() != 4) {
                Toast.makeText(this, "密码须为4位数字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!applyBankUrl()) return;
            Prefs.setEnabled(this, cb.isChecked());
            Prefs.setIntervalMinutes(this, minutes);
            if (!np.isEmpty()) Prefs.setPin(this, np);
            tvSaved.setVisibility(TextView.VISIBLE);
            if (Prefs.isEnabled(this)) {
                QuizLockService.start(this);
            } else {
                QuizLockService.stop(this);
            }
            refreshBanks(true, false);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        });

        btnRefresh.setOnClickListener(v -> {
            if (!applyBankUrl()) return;
            tvBank.setText("正在从网络更新题库…");
            refreshBanks(true, true);
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

    private boolean applyBankUrl() {
        String url = etBank.getText().toString().trim();
        if (url.isEmpty()) {
            url = Prefs.DEFAULT_BANK_BASE_URL;
            etBank.setText(url);
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            Toast.makeText(this, "题库地址需以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show();
            return false;
        }
        Prefs.setBankBaseUrl(this, url);
        return true;
    }

    private void refreshBanks(boolean force, boolean toastWhenDone) {
        BankFetcher.kick(this, () -> runOnUiThread(() -> {
            if (isDestroyed()) return;
            showBankStatus();
            if (toastWhenDone) {
                Toast.makeText(this, "题库更新已结束", Toast.LENGTH_SHORT).show();
            }
        }), force);
    }

    private void showBankStatus() {
        String net = Prefs.getBankStatus(this);
        String line = BankRepository.summary();
        if (net.length() > 0) line = line + "\n" + net;
        tvBank.setText(line);
    }
}
