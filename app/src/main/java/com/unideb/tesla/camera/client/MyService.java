package com.unideb.tesla.camera.client;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
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
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.unideb.tesla.camera.dto.DisclosureSchedule;
import com.unideb.tesla.camera.dto.Packet;

import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MyService extends IntentService {

    private boolean isRunning;
    private MulticastSocket multicastSocket;
    private byte[] udpBuffer;
    private DatagramPacket datagramPacket;

    // TESLA stuff
    private DisclosureSchedule disclosureSchedule;
    private byte[][] keys;
    private int latestKeyIndex;
    private long timeDifference;
    private List<Triplet> buffer;

    // Camera stuff
    /*
    public static final int CAMREA_REQUEST_CODE = 12345;

    private TextureView textureView;
     */
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private CameraManager cameraManager;
    private Size size;
    private String cameraId;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private int pictureNumber = 0;

    // binderino
    private Messenger messenger;

    public MyService() {
        super("MyService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Log.d("ONHANDLEINTENT", "ESKETIT!!!");

        /*
        while(isRunning){

            try {

                multicastSocket.receive(datagramPacket);
                byte[] data = datagramPacket.getData();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        Log.d("SERVICE", "AFTER WHILE");
        */

        // initialize
        /*try {
            initialize();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }*/

        while (isRunning) {
            try {
                run();
            } catch (IOException e) {
                if (!(e instanceof SocketTimeoutException)) {
                    e.printStackTrace();
                }
            }
        }

        Log.d("ONHANDLEINTENT", "NOT RUNNING BIATCH!!!");

    }

    @Override
    public void onCreate() {

        Log.d("ONCREATE", "KEK");

        super.onCreate();

        isRunning = true;

        try {

            multicastSocket = new MulticastSocket(9999);
            multicastSocket.joinGroup(InetAddress.getByName("230.1.2.3"));
            multicastSocket.setSoTimeout(1000);

        } catch (IOException e) {
            e.printStackTrace();
        }

        udpBuffer = new byte[1024];

        datagramPacket = new DatagramPacket(udpBuffer, udpBuffer.length);

        // TODO: NO!!!
        timeDifference = 100;

        buffer = new ArrayList<>();

        openBackgroundHandler();
        setupCamera();
        openCamera();

        Log.d("SERVICE", "ISRUNNING IS TRUE");

    }

    @Override
    public void onDestroy() {

        Log.d("SERVICE", "GECIFOS");

        super.onDestroy();

        isRunning = false;

        try {

            multicastSocket.leaveGroup(InetAddress.getByName("230.1.2.3"));
            multicastSocket.close();
            multicastSocket = null;

        } catch (IOException e) {
            e.printStackTrace();
        }

        udpBuffer = null;

        closeBackgroundHandler();

        Log.d("SERVICE", "ISRUNNING IS FALSE");

    }

    // TESLA stuff

    private void initialize() throws IOException {

        // initialize for UDP communication
        multicastSocket = new MulticastSocket(9999);
        multicastSocket.joinGroup(InetAddress.getByName("230.1.2.3."));

        // initialize triple buffer
        buffer = new ArrayList<>();

        // initialize UDP buffer
        udpBuffer = new byte[1024];

    }

    private void run() throws IOException {

        Log.d("DELAY", Long.toString(timeDifference));

        // receive bytes
        byte[] receivedBytes = receiveBytes();

        // deserialize bytes to Object
        Object object = SerializationUtils.deserialize(receivedBytes);

        // cast object to DisclosureSchedule or Packet
        if (object instanceof DisclosureSchedule) {

            DisclosureSchedule disclosureSchedule = (DisclosureSchedule) object;
            handleDisclosureSchedule(disclosureSchedule);

        } else if (object instanceof Packet) {

            Packet packet = (Packet) object;
            handlePacket(packet);

        }

    }

    private byte[] receiveBytes() throws IOException {

        // create datagram packet for receiving data
        DatagramPacket datagramPacket = new DatagramPacket(udpBuffer, udpBuffer.length);

        // receive packet
        multicastSocket.receive(datagramPacket);

        // get bytes
        return datagramPacket.getData();

    }

    private void handleDisclosureSchedule(DisclosureSchedule disclosureSchedule) {

        // save the disclosure schedule for later uses
        this.disclosureSchedule = disclosureSchedule;

        // init keys
        keys = new byte[disclosureSchedule.getKeychainLength()][32];        // 32 byte = 256 bit
        for (int i = 0; i < disclosureSchedule.getKeychainLength(); i++) {
            keys[i] = null;
        }

        // get the disclosed key index
        int disclosedKeyIndex = disclosureSchedule.getIntervalIndex() - disclosureSchedule.getDisclosureDelay();

        // store the key and its index
        keys[disclosedKeyIndex] = disclosureSchedule.getKeyCommitment();
        latestKeyIndex = disclosedKeyIndex;

        // clear buffer
        buffer.clear();

    }

    private void handlePacket(Packet packet) {

        // if we didn't get a disclosure schedule yet, throw away all packets
        if (disclosureSchedule == null) {
            return;
        }

        // determine interval index
        int packetIntervalIndex = 0;

        try {
            packetIntervalIndex = TeslaUtils.determineIntervalIndex(packet.getDisclosedKey(), disclosureSchedule.getIntervalIndex(), disclosureSchedule.getKeyCommitment(), disclosureSchedule.getKeychainLength());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return;
        }

        // estimate server's interval index
        int estimatedServerIntervalIndex = TeslaUtils.estimateServerIntervalIndex(disclosureSchedule.getStartTime(), disclosureSchedule.getIntervalDuration(), disclosureSchedule.getIntervalIndex(), timeDifference);

        // if packet is not safe, throw it away
        if (!TeslaUtils.isSafePacket(estimatedServerIntervalIndex, packetIntervalIndex, disclosureSchedule.getDisclosureDelay())) {
            return;
        }

        // save important information in buffer
        Triplet tripletToSave = new Triplet(packetIntervalIndex, packet.getTeslaMessage(), packet.getMAC());
        buffer.add(tripletToSave);

        // authenticate previously stored triplets

        // get disclosed interval index
        int disclosedIntervalIndex = packetIntervalIndex - disclosureSchedule.getDisclosureDelay();

        if (disclosedIntervalIndex > latestKeyIndex) {

            // store the disclosed key and its index
            keys[disclosedIntervalIndex] = packet.getDisclosedKey();
            latestKeyIndex = disclosedIntervalIndex;

            // collect triplets to authenticate
            List<Triplet> tripletsToValidate = new ArrayList<>();

            for (Triplet triplet : buffer) {
                if (triplet.getIntervalIndex() == disclosedIntervalIndex) {
                    tripletsToValidate.add(triplet);
                }
            }

            // validate triplets
            List<Triplet> validTriplets = new ArrayList<>();

            for (Triplet triplet : tripletsToValidate) {
                try {
                    if (TeslaUtils.isValidTriplet(triplet, packet.getDisclosedKey())) {
                        validTriplets.add(triplet);
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

            // remove triplets from buffer
            buffer.removeAll(tripletsToValidate);

            // handle valid messages
            handleValidTriplets(validTriplets);

        }

    }

    private void handleValidTriplets(List<Triplet> triplets) {

        for (Triplet triplet : triplets) {

            Log.d("VALID TRIPLET", triplet.getTeslaMessage().getMessage());

        }

        captureImage();

    }

    // binderino

    class IncomingHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {

            if(msg.what == 69){

                // TODO: what?
                Log.d("HANDLER", "KECIFEI");

                timeDifference = msg.arg1;

            }else {
                super.handleMessage(msg);
            }

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Log.d("ONBIND", "BIND MADAFAKA!!!");

        messenger = new Messenger(new IncomingHandler());

        return messenger.getBinder();

    }


    // Camera stuff

    private void openBackgroundHandler() {

        handlerThread = new HandlerThread("camera_app");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

    }

    private void setupCamera() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

            try {

                String[] cameraIds = cameraManager.getCameraIdList();

                for (String cameraId : cameraIds) {

                    CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {

                        this.cameraId = cameraId;

                        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                        size = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];

                        Log.d("WIDTH", Integer.toString(size.getWidth()));
                        Log.d("HEIGHT", Integer.toString(size.getHeight()));

                        imageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.JPEG, 2);
                        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

                    }

                }

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

    }

    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

            // save image temporary
            Image image = imageReader.acquireLatestImage();

            File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "mypic_" + pictureNumber + ".jpg");
            Log.d("FILE", mFile.getAbsolutePath());

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            pictureNumber++;

            // POST it

            Uri fileUri = Uri.fromFile(mFile);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), mFile);

            MultipartBody.Part body = MultipartBody.Part.createFormData("picture", mFile.getName(), requestFile);

            String descriptionString = "Hello this is a simple bullshit!";

            RequestBody description = RequestBody.create(MultipartBody.FORM, descriptionString);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.0.109:9000/")
                    .build();

            UploadService uploadService = retrofit.create(UploadService.class);

            Call<ResponseBody> call = uploadService.upload(description, body);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d("POST", "SUCCESS!!!");
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("POST", "FAILED!!!");
                }
            });

        }

    };

    private void openCamera() {

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){

                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMREA_REQUEST_CODE);

            }else{

                try {

                    cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }

        }
        */

        try {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d("FUCK", "SHIEEEEEEEEEET");
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

        /*
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        final Surface surface = new Surface(surfaceTexture);
        */

        Surface surface = new Surface(new SurfaceTexture(1));

        try {

            cameraDevice.createCaptureSession(Collections.singletonList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {

                    /*
                    try {

                        CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.addTarget(surface);
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    */

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

    public void captureImage(){

        createCaptureRequest();

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

    private void closeBackgroundHandler(){

        handlerThread.quit();
        handlerThread = null;
        backgroundHandler = null;

    }

}
