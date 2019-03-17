package com.unideb.tesla.camera.client;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unideb.tesla.camera.dto.DisclosureSchedule;
import com.unideb.tesla.camera.dto.Packet;

import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class TeslaService extends IntentService {

    public static final int TIME_SYNCHRONIZATION_DELAY_UPDATE = 11;

    // service related fields
    private boolean isRunning;
    private Context context;
    private Messenger messenger;

    // TESLA related fields
    private DisclosureSchedule disclosureSchedule;
    private byte[][] keys;
    private int latestKeyIndex;
    private long timeDifference;
    private List<Triplet> buffer;

    // UDP related fields
    private MulticastSocket multicastSocket;
    private String address;
    private int port;
    private byte[] udpBuffer;
    private DatagramPacket datagramPacket;

    // camera
    private CameraHandler cameraHandler;

    // device information
    private DeviceInformation deviceInformation;

    // webapp related fields
    private String webappAddress;
    private Retrofit retrofit;
    private DeviceService deviceService;
    private ImageService imageService;

    public TeslaService() {
        super("TeslaService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        // get the delay from the intent
        timeDifference = intent.getLongExtra("time_synchronization_delay", 0);
        address = intent.getStringExtra("multicast_address");
        port = intent.getIntExtra("multicast_port", 9999);
        webappAddress = intent.getStringExtra("webapp_address");

        // initialize
        initialize();

        // receive and handle messages
        while (isRunning) {
            try {
                run();
            } catch (IOException e) {
                if (!(e instanceof SocketTimeoutException)) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        // close everything

        // stop running
        isRunning = false;

        // close udp socket communication
        try {

            // TODO: read them from settings
            multicastSocket.leaveGroup(InetAddress.getByName(address));
            multicastSocket.close();
            multicastSocket = null;

        } catch (IOException e) {
            e.printStackTrace();
        }

        udpBuffer = null;
        datagramPacket = null;

        // reset TESLA buffer
        buffer.clear();
        buffer = null;

        // close camera
        cameraHandler.close();
        cameraHandler = null;

    }

    private class IncomingHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {

            if(msg.what == TIME_SYNCHRONIZATION_DELAY_UPDATE){

                long delay = msg.arg1;
                timeDifference = delay;

            }else{
                super.handleMessage(msg);
            }

        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        messenger = new Messenger(new IncomingHandler());
        return messenger.getBinder();

    }

    private void initialize(){

        // collect device information
        deviceInformation = collectDeviceInformation();

        // init retrofit
        initializeRetrofit();

        // check if device registered in db
        try {
            deviceSetup();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // TODO: set up GPS stuff

        // init udp socket communication
        try {

            multicastSocket = new MulticastSocket(port);
            multicastSocket.joinGroup(InetAddress.getByName(address));
            multicastSocket.setSoTimeout(1000);

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        udpBuffer = new byte[1024];
        datagramPacket = new DatagramPacket(udpBuffer, udpBuffer.length);

        // init TESLA buffer
        buffer = new ArrayList<>();

        // init camera
        cameraHandler = new CameraHandler(this, new ClientOnImageAvailableListener(imageService, deviceInformation.getMac()));
        cameraHandler.init();

        // service is running
        isRunning = true;

    }

    private void initializeRetrofit(){

        // gson for parsing data
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // create retrofit object
        retrofit = new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(webappAddress)
                .build();

        // create services
        deviceService = retrofit.create(DeviceService.class);
        imageService = retrofit.create(ImageService.class);

    }

    private DeviceInformation collectDeviceInformation(){

        String mac = NetworkUtils.getMacAddress();
        String brand = Build.BRAND;
        String device = Build.DEVICE;
        String model = Build.MODEL;
        String sdk = Integer.toString(Build.VERSION.SDK_INT);

        return new DeviceInformation(mac, brand, device, model, sdk);

    }

    private void deviceSetup() throws IOException {

        Call<DeviceRequest> deviceCheckCall = deviceService.get(deviceInformation.getMac());
        DeviceRequest deviceCheckResult = deviceCheckCall.execute().body();

        if(deviceCheckResult == null){

            // device doesn't exist in db, lets create it
            DeviceRequest deviceRequest = new DeviceRequest(deviceInformation.getMac(), deviceInformation.getBrand(), deviceInformation.getDevice(), deviceInformation.getModel(), deviceInformation.getSdk(), "", 0);

            Call<DeviceRequest> devicePutCall = deviceService.put(deviceRequest);
            devicePutCall.execute();

        }else{
            // TODO: should we update it?
        }

    }

    private void run() throws IOException {

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

        Log.d("HANDLE_PACKET", Long.toString(timeDifference));

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

        // TODO: stuff
        cameraHandler.captureImage();

    }

}
