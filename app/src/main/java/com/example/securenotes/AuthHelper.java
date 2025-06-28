package com.example.securenotes;

import android.content.Intent;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.Executor;


public class AuthHelper {

    public interface AuthCallback {
        void onSuccess();
        void onFailure();
    }

    public static void authenticate(FragmentActivity activity, AuthCallback callback) {
        BiometricManager manager = BiometricManager.from(activity);
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt prompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                callback.onSuccess();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                // Qualsiasi errore (incluso "Annulla") porta a fallimento
                callback.onFailure();
            }

            @Override
            public void onAuthenticationFailed() {
                // Ignora
            }
        });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticazione richiesta")
                .setSubtitle("Usa impronta")
                .setNegativeButtonText("Annulla")
                .build();

        if (manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            prompt.authenticate(info);
        } else {
            activity.startActivityForResult(new Intent(activity, PinActivity.class), 999);
        }
    }
}


