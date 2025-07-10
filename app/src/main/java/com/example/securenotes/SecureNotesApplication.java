package com.example.securenotes;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Configuration;


public class SecureNotesApplication extends Application implements Configuration.Provider {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeUtils.applyThemeFromPreferences(this);
        registerActivityLifecycleCallbacks(new AppLifecycleTracker());
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build();
    }
}

