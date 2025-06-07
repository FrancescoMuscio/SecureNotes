package com.example.securenotes;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class AppLifecycleTracker implements Application.ActivityLifecycleCallbacks {

    private static int started = 0;
    private static boolean wasInBackground = false;

    public static boolean needsReauth() {
        return wasInBackground;
    }

    public static void clearReauthFlag() {
        wasInBackground = false;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (started == 0) {
            wasInBackground = true;
        }
        started++;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        started--;
    }

    // altri metodi non necessari
    public void onActivityCreated(Activity a, Bundle b) {}
    public void onActivityDestroyed(Activity a) {}
    public void onActivityPaused(Activity a) {}
    public void onActivityResumed(Activity a) {}
    public void onActivitySaveInstanceState(Activity a, Bundle b) {}
}
