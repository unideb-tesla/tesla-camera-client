package com.unideb.tesla.camera.client;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class ClientSharedPreferences {

    public static final String TESLA_CAMERA_CLIENT_SHARED_PREFERENCES_NAME = "tesla_camera_client_shared_preferences";
    public static final String SHARED_PREFERENCE_KEY_TIME_SYNCHRONIZATION_DELAY = "time_synchronization_delay";

    private Activity activity;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public ClientSharedPreferences(Activity activity) {

        this.activity = activity;

        sharedPreferences = activity.getSharedPreferences(TESLA_CAMERA_CLIENT_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

    }

    public boolean isTimeSynchronizationDelayKeyExists(){
        return sharedPreferences.contains(SHARED_PREFERENCE_KEY_TIME_SYNCHRONIZATION_DELAY);
    }

    public long getTimeSynchronizationDelay(){
        return sharedPreferences.getLong(SHARED_PREFERENCE_KEY_TIME_SYNCHRONIZATION_DELAY, 0);
    }

    public void saveTimeSynchronizationDelay(long delay){

        editor.putLong(SHARED_PREFERENCE_KEY_TIME_SYNCHRONIZATION_DELAY, delay);
        editor.commit();

    }

}
