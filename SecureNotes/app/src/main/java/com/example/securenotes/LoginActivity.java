package com.example.securenotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnUsePin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tvStatus = findViewById(R.id.tv_status);
        btnUsePin = findViewById(R.id.btn_use_pin);

        BiometricManager biometricManager = BiometricManager.from(this);

        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt();
        } else {
            tvStatus.setText("Biometria non disponibile");
            btnUsePin.setVisibility(Button.VISIBLE);
        }

        btnUsePin.setOnClickListener(v -> {
            try {
                SharedPreferences prefs = getEncryptedPrefs();
                String savedPin = prefs.getString("user_pin", null);

                if (savedPin == null) {
                    startActivity(new Intent(this, SetupPinActivity.class));
                } else {
                    startActivity(new Intent(this, PinActivity.class));
                }

                finish();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Errore nel controllo PIN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                goToDashboard();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                fallbackToPinOrSetup();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginActivity.this, "Autenticazione fallita", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Accesso biometrico")
                .setSubtitle("Sblocca SecureNotes")
                .setNegativeButtonText("Usa PIN")
                .build();

        prompt.authenticate(promptInfo);
    }

    private void fallbackToPinOrSetup() {
        try {
            SharedPreferences prefs = getEncryptedPrefs();
            String savedPin = prefs.getString("user_pin", null);

            if (savedPin == null) {
                startActivity(new Intent(this, SetupPinActivity.class));
            } else {
                startActivity(new Intent(this, PinActivity.class));
            }

            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore nel fallback", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToDashboard() {
        try {
            SharedPreferences prefs = getEncryptedPrefs();
            String savedPin = prefs.getString("user_pin", null);

            if (savedPin == null) {
                startActivity(new Intent(this, SetupPinActivity.class));
            } else {
                startActivity(new Intent(this, DashboardActivity.class));
            }

            AppLifecycleTracker.clearReauthFlag();
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore autenticazione", Toast.LENGTH_SHORT).show();
        }
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

