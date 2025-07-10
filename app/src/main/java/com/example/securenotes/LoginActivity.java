package com.example.securenotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import com.scottyab.rootbeer.RootBeer;


public class LoginActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnUsePin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Controllo della presneza di root nel dispositivo
        RootBeer rootBeer = new RootBeer(this);
        if (rootBeer.isRooted()) {
            new Handler(Looper.getMainLooper()).post(() -> {
                new AlertDialog.Builder(this)
                        .setTitle("Dispositivo non sicuro")
                        .setMessage("Il dispositivo è rootato. L'applicazione non può essere eseguita per motivi di sicurezza.")
                        .setCancelable(false)
                        .setPositiveButton("Esci", (dialog, which) -> finishAffinity())
                        .show();
            });
            return;
        }

        // Se il pin non esiste viene forzato il setup di esso
        try {
            if (!isPinSet()) {
                startActivity(new Intent(this, SetupPinActivity.class));
                finish();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Errore accesso protetto", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
            Intent intent = new Intent(this, PinActivity.class);
            startActivityForResult(intent, 1001);
        });
    }

    private boolean isPinSet() throws GeneralSecurityException, IOException {
        SharedPreferences prefs = getEncryptedPrefs();
        return prefs.getString("user_pin", null) != null;
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        goToDashboard();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Intent intent = new Intent(LoginActivity.this, PinActivity.class);
                            startActivityForResult(intent, 1001);
                        } else if(errorCode == BiometricPrompt.ERROR_USER_CANCELED ||    // Chiude il prompt andando "indietro"
                                errorCode == BiometricPrompt.ERROR_LOCKOUT){          // Troppi tentativi errati
                                    finishAffinity();
                        } else{
                            Toast.makeText(LoginActivity.this, "Errore: " + errString, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Toast.makeText(LoginActivity.this, "Impronta non riconosciuta", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Accesso biometrico")
                .setSubtitle("Sblocca SecureNotes")
                .setNegativeButtonText("Usa Pin")
                .build();

        prompt.authenticate(promptInfo);
    }

    private void goToDashboard() {
        AppLifecycleTracker.clearReauthFlag();
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            goToDashboard();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private SharedPreferences getEncryptedPrefs() throws GeneralSecurityException, IOException {
        MasterKey key = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                "secure_prefs",
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
