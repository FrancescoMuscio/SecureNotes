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

    public static void authenticate(Activity activity, AuthCallback callback) {
        BiometricManager manager = BiometricManager.from(activity);
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt prompt = new BiometricPrompt((FragmentActivity) activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            Intent intent = new Intent(activity, PinActivity.class);
                            activity.startActivityForResult(intent, 999);
                        } else {
                            callback.onFailure();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Ignora
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticazione richiesta")
                .setSubtitle("Usa impronta o PIN")
                .setNegativeButtonText("Usa PIN")
                .build();

        if (manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            prompt.authenticate(info);
        } else {
            Intent intent = new Intent(activity, PinActivity.class);
            activity.startActivityForResult(intent, 999);
        }
    }
}


