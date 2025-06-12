package com.example.securenotes;

import android.app.Activity;
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
        BiometricManager biometricManager = BiometricManager.from(activity);
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt prompt = new BiometricPrompt(
                activity,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Intent intent = new Intent(activity, PinActivity.class);
                            activity.startActivityForResult(intent, 999);
                        } else {
                            callback.onFailure();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // lasciamo che l’utente riprovi
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticazione richiesta")
                .setSubtitle("Biometria o PIN")
                .setNegativeButtonText("Usa PIN")
                .build();

        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            prompt.authenticate(info);
        } else {
            Intent intent = new Intent(activity, PinActivity.class);
            activity.startActivityForResult(intent, 999);
        }
    }
}
