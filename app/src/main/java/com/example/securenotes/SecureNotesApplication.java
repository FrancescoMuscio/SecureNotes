package com.example.securenotes;

import android.app.Application;

public class SecureNotesApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new AppLifecycleTracker());
    }
}
