package com.example.securenotes;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;


public class AppLifecycleTracker implements Application.ActivityLifecycleCallbacks {

    private static int started = 0;
    private static boolean needsReauth = false;
    private static long lastBackgroundTime = 0;
    private static final long backgroundInterval = 10_000;

    public static boolean needsReauth() {
        return needsReauth;
    }

    public static void clearReauthFlag() {
        needsReauth = false;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (started == 0 && lastBackgroundTime > 0) {
            long elapsed = System.currentTimeMillis() - lastBackgroundTime;
            if (elapsed > backgroundInterval) {
                needsReauth = true;
            } else {
                needsReauth = false;
            }
            lastBackgroundTime = 0;
        }
        started++;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        started--;
        if (started == 0) {
            // app in background
            lastBackgroundTime = System.currentTimeMillis();
        }
    }

    public void onActivityCreated(Activity a, Bundle b) {}
    public void onActivityDestroyed(Activity a) {}
    public void onActivityPaused(Activity a) {}
    public void onActivityResumed(Activity a) {}
    public void onActivitySaveInstanceState(Activity a, Bundle b) {}
}

