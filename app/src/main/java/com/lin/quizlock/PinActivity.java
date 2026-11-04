package com.lin.quizlock;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PinActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);
        EditText et = findViewById(R.id.et_pin);
        TextView err = findViewById(R.id.tv_pin_error);
        Button ok = findViewById(R.id.btn_pin_ok);

        Runnable check = () -> {
            if (Prefs.checkPin(this, et.getText().toString())) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
            } else {
                err.setVisibility(TextView.VISIBLE);
                et.setText("");
                et.requestFocus();
            }
        };
        ok.setOnClickListener(v -> check.run());
        et.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                check.run();
                return true;
            }
            return false;
        });
        et.requestFocus();
    }
}
