package com.unideb.tesla.camera.client;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import java.util.Collections;

import static android.content.Context.CAMERA_SERVICE;

public class CameraHandler {

    private Context context;
    private ImageReader.OnImageAvailableListener onImageAvailableListener;

    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private CameraManager cameraManager;
    private Size size;
    private String cameraId;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;

    public CameraHandler(Context context, ImageReader.OnImageAvailableListener onImageAvailableListener) {
        this.context = context;
        this.onImageAvailableListener = onImageAvailableListener;
    }

    private void openBackgroundHandler() {

        handlerThread = new HandlerThread("camera_app");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

    }

    public void init(){

        openBackgroundHandler();
        setupCamera();
        openCamera();

    }

    public void close(){

        cameraCaptureSession.close();

        handlerThread.quit();
        handlerThread = null;
        backgroundHandler = null;

    }

    public void captureImage(){

        createCaptureRequest();

    }

    private void setupCamera() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

            try {

                String[] cameraIds = cameraManager.getCameraIdList();

                for (String cameraId : cameraIds) {

                    CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {

                        this.cameraId = cameraId;

                        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                        size = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];

                        imageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.JPEG, 2);
                        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

                    }

                }

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

    }

    private void openCamera() {

        try {

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            cameraDevice = camera;
            createCaptureSession();

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private void createCaptureSession(){

        try {

            cameraDevice.createCaptureSession(Collections.singletonList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {

                    cameraCaptureSession = session;

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void createCaptureRequest() {

        try {

            CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(imageReader.getSurface());

            cameraCaptureSession.capture(requestBuilder.build(), null, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

}
