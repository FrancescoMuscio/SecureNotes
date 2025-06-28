package com.example.securenotes;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Configuration;


public class SecureNotesApplication extends Application implements Configuration.Provider {

    @Override
    public void onCreate() {
        super.onCreate();

        // Applica il tema salvato
        ThemeUtils.applyThemeFromPreferences(this);

        // Monitora lo stato per timeout e reautenticazione
        registerActivityLifecycleCallbacks(new AppLifecycleTracker());

        Log.d("SecureNotesApp", "Applicazione avviata e WorkManager configurato.");
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }
}

