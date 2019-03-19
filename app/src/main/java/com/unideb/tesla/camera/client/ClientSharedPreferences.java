package com.unideb.tesla.camera.client;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ClientSharedPreferences {

    public static final String SHARED_PREFERENCE_KEY_TIME_SYNCHRONIZATION_DELAY = "time_synchronization_delay";
    public static final String SHARED_PREFERENCE_KEY_WEBAPP_URL = "webapp_url";
    public static final String SHARED_PREFERENCE_KEY_BROADCAST_ADDRESS = "broadcast_address";
    public static final String SHARED_PREFERENCE_KEY_BROADCAST_PORT = "broadcast_port";

    private Activity activity;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public ClientSharedPreferences(Activity activity) {

        this.activity = activity;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
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

    public String getWebappUrl(){

        return sharedPreferences.getString(SHARED_PREFERENCE_KEY_WEBAPP_URL, "http://localhost:8080/");

    }

    public String getBroadcastAddress(){

        return sharedPreferences.getString(SHARED_PREFERENCE_KEY_BROADCAST_ADDRESS, "localhost");

    }

    public int getBroadcastPort(){

        return Integer.parseInt(sharedPreferences.getString(SHARED_PREFERENCE_KEY_BROADCAST_PORT, "9999"));

    }

}
