package com.unideb.tesla.camera.client;

import org.apache.commons.lang3.SerializationUtils;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class TeslaUtils {

    public static final String HASHING_ALGORITHM_SHA256 = "SHA-256";

    public static int determineIntervalIndex(byte[] disclosedKey, int intervalIndexOfDisclosedKey, byte[] keyToReach, int lengthOfChain) throws NoSuchAlgorithmException, InvalidKeyException {

        byte[] tmpKey = Arrays.copyOf(disclosedKey, disclosedKey.length);

        int steps = 0;

        while (!Arrays.equals(tmpKey, keyToReach) && intervalIndexOfDisclosedKey + steps < lengthOfChain) {

            steps++;

            MessageDigest messageDigest = MessageDigest.getInstance(HASHING_ALGORITHM_SHA256);
            messageDigest.update(tmpKey);
            tmpKey = messageDigest.digest();

        }

        // if we exceed the length of the chain, it means that the key was invalid
        if (intervalIndexOfDisclosedKey + steps >= lengthOfChain) {
            throw new InvalidKeyException();
        }

        return intervalIndexOfDisclosedKey + steps;

    }

    public static int estimateServerIntervalIndex(long startTime, long intervalDuration, int intervalIndex, long timeDifference) {

        long now = System.currentTimeMillis();
        long serverTimeUpperBound = now + timeDifference;
        int passedIntervals = (int) Math.floor((serverTimeUpperBound - startTime) * 1.0 / intervalDuration);

        return intervalIndex + passedIntervals;

    }

    public static boolean isSafePacket(int estimatedServerIntervalIndex, int packetIntervalIndex, int disclosureDelay) {

        return estimatedServerIntervalIndex < packetIntervalIndex + disclosureDelay;

    }

    public static boolean isValidTriplet(Triplet triplet, byte[] key) throws NoSuchAlgorithmException {

        // compute MAC
        byte[] messageBytes = SerializationUtils.serialize(triplet.getTeslaMessage());
        byte[] macToCheck;

        try {
            macToCheck = CryptoUtils.computeMac(messageBytes, key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }

        // compare MACs
        return Arrays.equals(macToCheck, triplet.getMAC());

    }

}