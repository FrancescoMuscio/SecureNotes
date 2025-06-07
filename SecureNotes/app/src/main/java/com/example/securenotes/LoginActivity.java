package com.example.securenotes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;


import androidx.annotation.NonNull;

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

        btnUsePin.setOnClickListener(v -> openPinFallback());

        checkBiometricSupportAndAuthenticate();
    }

    private void checkBiometricSupportAndAuthenticate() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                showBiometricPrompt();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
            default:
                tvStatus.setText("Biometria non disponibile");
                btnUsePin.setVisibility(Button.VISIBLE);
                break;
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                tvStatus.setText("Accesso effettuato");
                goToDashboard();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                tvStatus.setText("Autenticazione fallita");
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                tvStatus.setText("Errore: " + errString);
                btnUsePin.setVisibility(Button.VISIBLE);
            }
        };

        BiometricPrompt prompt = new BiometricPrompt(this, executor, callback);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticazione necessaria")
                .setSubtitle("Usa l'impronta o il volto")
                .setNegativeButtonText("Usa PIN")
                .build();

        prompt.authenticate(promptInfo);
    }

    private void openPinFallback() {
        // Avvia activity fallback per il PIN
        Intent intent = new Intent(this, PinActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
