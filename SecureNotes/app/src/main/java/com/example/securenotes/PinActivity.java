package com.example.securenotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class PinActivity extends AppCompatActivity {

    private EditText etPin;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        etPin = findViewById(R.id.et_pin);
        tvStatus = findViewById(R.id.tv_pin_status);
        Button btnConfirm = findViewById(R.id.btn_confirm_pin);

        btnConfirm.setOnClickListener(v -> {
            String enteredPin = etPin.getText().toString().trim();

            try {
                SharedPreferences prefs = getEncryptedPrefs();
                String savedPin = prefs.getString("user_pin", null);

                if (savedPin == null) {
                    tvStatus.setText("Nessun PIN salvato. Torna al login.");
                    return;
                }

                if (enteredPin.equals(savedPin)) {
                    AppLifecycleTracker.clearReauthFlag(); // evita doppia autenticazione
                    startActivity(new Intent(this, DashboardActivity.class));
                    finish();
                } else {
                    tvStatus.setText("PIN errato");
                }

            } catch (Exception e) {
                Toast.makeText(this, "Errore sicurezza", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private SharedPreferences getEncryptedPrefs() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}

