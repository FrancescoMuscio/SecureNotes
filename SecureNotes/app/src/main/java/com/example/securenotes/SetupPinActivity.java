package com.example.securenotes;

import android.content.Intent;
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

public class SetupPinActivity extends AppCompatActivity {

    private EditText etPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        etPin = findViewById(R.id.et_pin_setup);
        Button btnSetPin = findViewById(R.id.btn_set_pin);

        btnSetPin.setOnClickListener(v -> {
            String pin = etPin.getText().toString().trim();
            if (pin.length() < 4) {
                Toast.makeText(this, "PIN troppo corto", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                getEncryptedPrefs().edit().putString("user_pin", pin).apply();
                Toast.makeText(this, "PIN impostato", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Errore durante il salvataggio", Toast.LENGTH_SHORT).show();
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
