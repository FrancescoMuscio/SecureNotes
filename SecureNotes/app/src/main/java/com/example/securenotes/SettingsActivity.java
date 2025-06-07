package com.example.securenotes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etTimeout = findViewById(R.id.et_timeout);
        Button btnSave = findViewById(R.id.btn_save_timeout);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int timeout = prefs.getInt("timeout_minutes", 3);
        etTimeout.setText(String.valueOf(timeout));

        btnSave.setOnClickListener(v -> {
            String value = etTimeout.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(this, "Inserisci un numero", Toast.LENGTH_SHORT).show();
                return;
            }

            int minutes = Integer.parseInt(value);
            prefs.edit().putInt("timeout_minutes", minutes).apply();
            Toast.makeText(this, "Timeout aggiornato", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
