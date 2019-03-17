package com.unideb.tesla.camera.client;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String TESLA_CAMERA_CLIENT_MULTICAST_LOCK = "tesla_camera_client_multicast_lock";
    public static final String TIME_SYNCHRONIZATION_DELAY_INTENT_FILTER = "com.unideb.tesla.timesync.TIME_SYNCHRONIZATION_DELAY";

    public static final String DEFAULT_MULTICAST_ADDRESS = "230.1.2.3";
    public static final int DEFAULT_MULTICAST_PORT = 9999;
    public static final String DEFAULT_WEBAPP_ADDRESS = "http://192.168.0.109:8080/";

    private WifiManager.MulticastLock multicastLock;

    private boolean serviceRunning = false;
    private Intent serviceIntent;

    // messenger for communicating with the service
    private Messenger messenger;

    // AFTER REFACTOR:
    private Button serviceButton;

    private PermissionHandler permissionHandler;
    private ClientSharedPreferences clientSharedPreferences;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(TIME_SYNCHRONIZATION_DELAY_INTENT_FILTER)){

                long delay = intent.getLongExtra("time_synchronization_delay", 0);

                clientSharedPreferences.saveTimeSynchronizationDelay(delay);

                if(serviceRunning){

                    // send message to service
                    Message message = Message.obtain(null, TeslaService.TIME_SYNCHRONIZATION_DELAY_UPDATE, (int) delay, 0);
                    try {
                        messenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                }

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceButton = findViewById(R.id.serviceButton);

        refreshUi();

        initialize();

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

        // initialize broadcast receiver
        registerReceiver(broadcastReceiver, new IntentFilter(TIME_SYNCHRONIZATION_DELAY_INTENT_FILTER));

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

        if(!serviceRunning) {

            // check permissions
            if (!permissionHandler.allPermissionsGranted()) {

                permissionHandler.askForPermissions();

                Toast.makeText(this, "Please allow the required permissions!", Toast.LENGTH_LONG).show();

                return;

            }

            // check if time synchronization has been made
            if (!clientSharedPreferences.isTimeSynchronizationDelayKeyExists()) {

                Toast.makeText(this, "Please perform time synchronization with the corresponding application!", Toast.LENGTH_LONG).show();

                return;

            }

            // TODO: is device registered?

            // multicast lock
            initMulticastLock();

            // init service
            initService();

            // start service
            startService(serviceIntent);
            serviceRunning = true;

        }else{

            finishService();
            serviceRunning = false;

        }

        // refresh ui
        refreshUi();

    }

    private void initService(){

        serviceIntent = new Intent(this, TeslaService.class);
        serviceIntent.putExtra("time_synchronization_delay", clientSharedPreferences.getTimeSynchronizationDelay());
        serviceIntent.putExtra("multicast_address", DEFAULT_MULTICAST_ADDRESS);
        serviceIntent.putExtra("multicast_port", DEFAULT_MULTICAST_PORT);
        serviceIntent.putExtra("webapp_address", DEFAULT_WEBAPP_ADDRESS);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    private void finishService(){

        unbindService(serviceConnection);
        stopService(serviceIntent);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        releaseMulticastLock();

        unregisterReceiver(broadcastReceiver);

        if(serviceRunning){
            finishService();
            serviceRunning = false;
        }

    }

    // bind to service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
        }
    };

    @Override
    protected void onStart() {

        super.onStart();
        refreshUi();

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
