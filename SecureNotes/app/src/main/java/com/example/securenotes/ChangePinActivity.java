package com.example.securenotes;

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

public class ChangePinActivity extends AppCompatActivity {

    private EditText etOldPin, etNewPin;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pin);

        etOldPin = findViewById(R.id.et_old_pin);
        etNewPin = findViewById(R.id.et_new_pin);
        tvStatus = findViewById(R.id.tv_change_pin_status);
        Button btnConfirm = findViewById(R.id.btn_confirm_change_pin);

        btnConfirm.setOnClickListener(v -> {
            try {
                String currentPin = getPrefs().getString("user_pin", null);
                String oldPin = etOldPin.getText().toString().trim();
                String newPin = etNewPin.getText().toString().trim();

                if (!oldPin.equals(currentPin)) {
                    tvStatus.setText("PIN attuale errato");
                    return;
                }
                if (newPin.length() < 4) {
                    tvStatus.setText("Nuovo PIN troppo corto");
                    return;
                }

                getPrefs().edit().putString("user_pin", newPin).apply();
                Toast.makeText(this, "PIN aggiornato", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                tvStatus.setText("Errore sicurezza");
            }
        });
    }

    private SharedPreferences getPrefs() throws GeneralSecurityException, IOException {
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
