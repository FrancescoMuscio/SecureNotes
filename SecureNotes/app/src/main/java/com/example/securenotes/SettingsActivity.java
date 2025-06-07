package com.example.securenotes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SettingsActivity extends AppCompatActivity {

    private EditText etTimeout, etOldPin, etNewPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etTimeout = findViewById(R.id.et_timeout);
        etOldPin = findViewById(R.id.et_old_pin);
        etNewPin = findViewById(R.id.et_new_pin);

        Button btnSaveTimeout = findViewById(R.id.btn_save_timeout);
        Button btnChangePin = findViewById(R.id.btn_change_pin);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int currentTimeout = prefs.getInt("timeout_minutes", 3);
        etTimeout.setText(String.valueOf(currentTimeout));

        btnSaveTimeout.setOnClickListener(v -> {
            String value = etTimeout.getText().toString().trim();
            if (value.isEmpty()) {
                toast("Inserisci un numero");
                return;
            }

            int timeout = Integer.parseInt(value);
            if (timeout < 1) {
                toast("Il timeout deve essere almeno 1 minuto");
                return;
            }

            prefs.edit().putInt("timeout_minutes", timeout).apply();
            toast("Timeout aggiornato");
        });

        btnChangePin.setOnClickListener(v -> {
            try {
                SharedPreferences securePrefs = getEncryptedPrefs();
                String savedPin = securePrefs.getString("user_pin", null);
                String oldPin = etOldPin.getText().toString().trim();
                String newPin = etNewPin.getText().toString().trim();

                if (savedPin == null || !oldPin.equals(savedPin)) {
                    toast("PIN attuale errato");
                    return;
                }

                if (newPin.length() < 4) {
                    toast("Nuovo PIN troppo corto");
                    return;
                }

                securePrefs.edit().putString("user_pin", newPin).apply();
                toast("PIN aggiornato");
                etOldPin.setText("");
                etNewPin.setText("");
            } catch (Exception e) {
                toast("Errore durante il cambio PIN");
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

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
