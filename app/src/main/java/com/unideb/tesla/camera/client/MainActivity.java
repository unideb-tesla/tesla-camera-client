package com.unideb.tesla.camera.client;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String TESLA_CAMERA_CLIENT_MULTICAST_LOCK = "tesla_camera_client_multicast_lock";
    public static final String TIME_SYNCHRONIZATION_DELAY_INTENT_FILTER = "com.unideb.tesla.timesync.TIME_SYNCHRONIZATION_DELAY";

    public static final ClientLocationListener CLIENT_LOCATION_LISTENER = new ClientLocationListener();
    private LocationManager locationManager;
    private boolean isLocationUpdateRequested;

    private WifiManager.MulticastLock multicastLock;
    private PowerManager.WakeLock wl;
    private boolean isWakeLockAcquired;

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

            if (intent.getAction().equals(TIME_SYNCHRONIZATION_DELAY_INTENT_FILTER)) {

                long delay = intent.getLongExtra("time_synchronization_delay", 0);

                clientSharedPreferences.saveTimeSynchronizationDelay(delay);

                if (serviceRunning) {

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.settings_menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.action_settings){

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;

        }

        return super.onOptionsItemSelected(item);

    }

    private void initialize(){

        // initialize permission handler
        permissionHandler = new PermissionHandler(new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
                Manifest.permission.CAMERA,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, this);
        permissionHandler.askForPermissions();

        // initialize shared preferences handler
        clientSharedPreferences = new ClientSharedPreferences(this);

        // initialize broadcast receiver
        registerReceiver(broadcastReceiver, new IntentFilter(TIME_SYNCHRONIZATION_DELAY_INTENT_FILTER));

        // initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Location permissions not provided!", Toast.LENGTH_SHORT).show();

        } else if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER ) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            Toast.makeText(this, "Location providers are not available!", Toast.LENGTH_SHORT).show();

        } else {

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);

            String provider = locationManager.getBestProvider(criteria, true);

            locationManager.requestLocationUpdates(provider, 5000, 0, CLIENT_LOCATION_LISTENER);

            isLocationUpdateRequested = true;

        }

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

            // multicast lock
            initMulticastLock();

            // wake lock
            PowerManager pm = (PowerManager)getApplicationContext().getSystemService(getApplicationContext().POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "myapp:my_wake_lock");
            wl.acquire();
            isWakeLockAcquired = true;

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
        serviceIntent.putExtra("multicast_address", clientSharedPreferences.getBroadcastAddress());
        serviceIntent.putExtra("multicast_port", clientSharedPreferences.getBroadcastPort());
        serviceIntent.putExtra("webapp_address", clientSharedPreferences.getWebappUrl());
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    private void finishService(){

        unbindService(serviceConnection);
        stopService(serviceIntent);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if(isLocationUpdateRequested){
            locationManager.removeUpdates(CLIENT_LOCATION_LISTENER);
            isLocationUpdateRequested = false;
        }

        if(isWakeLockAcquired) {

            wl.release();
            isWakeLockAcquired = false;

        }

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
