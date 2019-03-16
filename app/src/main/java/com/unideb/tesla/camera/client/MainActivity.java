package com.unideb.tesla.camera.client;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String TESLA_CAMERA_CLIENT_MULTICAST_LOCK = "tesla_camera_client_multicast_lock";

    private WifiManager.MulticastLock multicastLock;

    private boolean serviceRunning = false;
    private Intent serviceIntent;

    // binderino
    private Messenger messenger;
    private boolean isBound;
    private int delay = 0;
    private boolean isUnbound = false;

    // AFTER REFACTOR:
    private Button serviceButton;

    private PermissionHandler permissionHandler;
    private ClientSharedPreferences clientSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceButton = findViewById(R.id.serviceButton);

        refreshUi();

        initialize();

        initService();

    }

    private void initialize(){

        // initialize permission handler
        permissionHandler = new PermissionHandler(new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
                Manifest.permission.CAMERA
        }, this);
        permissionHandler.askForPermissions();

        // initialize shared preferences handler
        clientSharedPreferences = new ClientSharedPreferences(this);

    }

    private void refreshUi(){

        // service button
        if(!serviceRunning){
            serviceButton.setText(R.string.start_button_text);
        }else{
            serviceButton.setText(R.string.stop_button_text);
        }

    }

    public void serviceButtonHandler(View view){

        // TODO: check requirements

        // check permissions
        if(!permissionHandler.allPermissionsGranted()){

            permissionHandler.askForPermissions();

            Toast.makeText(this, "Please allow the required permissions!", Toast.LENGTH_LONG).show();

            return;

        }

        // check if time synchronization has been made
        if(!clientSharedPreferences.isTimeSynchronizationDelayKeyExists()){

            Toast.makeText(this, "Please perform time synchronization with the corresponding application!", Toast.LENGTH_LONG).show();

            return;

        }

        // multicast lock
        initMulticastLock();

        // TODO: start it?

        // refresh ui
        refreshUi();

    }

    private void initService(){

        serviceIntent = new Intent(this, MyService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    private void finishService(){

        unbindService(serviceConnection);
        isUnbound = true;
        stopService(serviceIntent);
        // serviceIntent = null;

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        releaseMulticastLock();

    }

    // binderino
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("SERVICECONNECTION", "BINDERINO");
            messenger = new Messenger(service);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("SERVICECONNECTION", "UNBINDERONI");
            messenger = null;
            isBound = false;
        }
    };

    @Override
    protected void onStart() {

        super.onStart();

        refreshUi();

        Log.d("ONSTART", "WE ARE STARTING!");

        // bindService(new Intent(this, MyService.class), serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {

        super.onStop();

        /*
        if(isBound){

            unbindService(serviceConnection);
            isBound = false;

        }
        */

        // finishService();

    }

    public void delay(View view){

        if(!isBound){
            return;
        }

        Message message = Message.obtain(null, 69, delay, 0);

        try {
            messenger.send(message);
            delay += 25;
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void serviceButton(View view){

        Log.d("BUTTON", "SERVICE");

        if(!serviceRunning){

            if(isUnbound){
                bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
                isUnbound = false;
            }

            startService(serviceIntent);
            serviceRunning = true;

        }else{

            finishService();
            serviceRunning = false;

        }

        /*
        if(!serviceRunning){

            serviceIntent = new Intent(this, MyService.class);
            startService(serviceIntent);
            serviceRunning = true;

        }else{

            Log.d("SERVICE", "ITS RUNNING, LETS STOP!");

            // unbindService(serviceConnection);
            // isBound = false;

            stopService(serviceIntent);
            serviceIntent = null;
            serviceRunning = false;

        }
        */

    }

    private void initMulticastLock(){

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock(TESLA_CAMERA_CLIENT_MULTICAST_LOCK);
        multicastLock.acquire();

    }

    private void releaseMulticastLock(){

        if(multicastLock != null){

            multicastLock.release();
            multicastLock = null;

        }

    }

}
