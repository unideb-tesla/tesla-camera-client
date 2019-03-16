package com.unideb.tesla.camera.client;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHandler {

    public static final int TESLA_CAMERA_CLIENT_PERMISSIONS_CODE = 0;

    private String[] requiredPermissions;
    private Activity activity;

    public PermissionHandler(String[] requiredPermissions, Activity activity) {
        this.requiredPermissions = requiredPermissions;
        this.activity = activity;
    }

    public void askForPermissions(){

        // collect permission array to ask for
        String[] permissionsToAskFor = collectPermissionsToAskFor();

        // ask for them
        if(permissionsToAskFor != null) {
            ActivityCompat.requestPermissions(activity, permissionsToAskFor, TESLA_CAMERA_CLIENT_PERMISSIONS_CODE);
        }

    }

    public boolean allPermissionsGranted(){
        return collectPermissionsToAskFor() == null;
    }

    private String[] collectPermissionsToAskFor(){

        List<String> permissionsList = new ArrayList<>();

        for(String permission : requiredPermissions){
            if(!isGrantedPermission(permission)){
                permissionsList.add(permission);
            }
        }

        String[] permissionsToAskFor = permissionsList.size() == 0 ? null : permissionsList.toArray(new String[permissionsList.size()]);

        return permissionsToAskFor;

    }

    private boolean isGrantedPermission(String permission){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;

    }

}
