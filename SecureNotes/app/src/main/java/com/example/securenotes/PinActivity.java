package com.example.securenotes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class PinActivity extends AppCompatActivity {

    private EditText etPin;
    private TextView tvStatus;
    private String savedPin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        etPin = findViewById(R.id.et_pin);
        tvStatus = findViewById(R.id.tv_pin_status);
        Button btnConfirm = findViewById(R.id.btn_confirm_pin);

        try {
            savedPin = loadStoredPin();
        } catch (Exception e) {
            tvStatus.setText("Errore sicurezza: " + e.getMessage());
        }

        btnConfirm.setOnClickListener(v -> {
            String inputPin = etPin.getText().toString().trim();

            if (inputPin.length() < 4) {
                tvStatus.setText("PIN troppo corto");
                return;
            }

            if (savedPin == null) {
                // Primo avvio → salva il PIN
                savePin(inputPin);
                tvStatus.setText("PIN salvato. Accesso...");
                goToDashboard();
            } else {
                // Verifica
                if (inputPin.equals(savedPin)) {
                    goToDashboard();
                } else {
                    tvStatus.setText("PIN errato");
                }
            }
        });
    }

    private String loadStoredPin() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).getString("user_pin", null);
    }

    private void savePin(String pin) {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            EncryptedSharedPreferences.create(
                    this,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).edit().putString("user_pin", pin).apply();
        } catch (Exception e) {
            tvStatus.setText("Errore salvataggio PIN");
        }
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
