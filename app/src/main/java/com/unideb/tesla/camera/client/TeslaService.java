package com.unideb.tesla.camera.client;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.Nullable;
import android.util.Log;

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

public class TeslaService extends IntentService {

    // service related fields
    private boolean isRunning;
    private Context context;

    // TESLA related fields
    private DisclosureSchedule disclosureSchedule;
    private byte[][] keys;
    private int latestKeyIndex;
    private long timeDifference;
    private List<Triplet> buffer;

    // UDP related fields
    private MulticastSocket multicastSocket;
    private byte[] udpBuffer;
    private DatagramPacket datagramPacket;

    // camera
    private CameraHandler cameraHandler;

    public TeslaService() {
        super("TeslaService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        // get the delay from the intent
        timeDifference = intent.getLongExtra("time_synchronization_delay", 0);

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
            multicastSocket.leaveGroup(InetAddress.getByName("230.1.2.3"));
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

    private void initialize(){

        // service is running
        isRunning = true;

        // init udp socket communication
        try {

            // TODO: read them from settings
            multicastSocket = new MulticastSocket(9999);
            multicastSocket.joinGroup(InetAddress.getByName("230.1.2.3"));
            multicastSocket.setSoTimeout(1000);

        } catch (IOException e) {
            e.printStackTrace();
        }

        udpBuffer = new byte[1024];
        datagramPacket = new DatagramPacket(udpBuffer, udpBuffer.length);

        // init TESLA buffer
        buffer = new ArrayList<>();

        // init camera
        /*
        cameraHandler = new CameraHandler(this, new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                Image image = reader.acquireLatestImage();

                // TODO: mess around with image

                image.close();

            }
        });
        */
        cameraHandler = new CameraHandler(this, new ClientOnImageAvailableListener());
        cameraHandler.init();

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
