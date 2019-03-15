package com.unideb.tesla.camera.client;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    public static final String HMAC_HASH_ALGORITHM_SHA256 = "HmacSHA256";

    public static byte[] computeMac(byte[] message, byte[] key) throws InvalidKeyException, NoSuchAlgorithmException {

        Mac mac = Mac.getInstance(HMAC_HASH_ALGORITHM_SHA256);

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());

        mac.init(secretKeySpec);

        mac.update(message);

        byte[] macBytes = mac.doFinal();

        return macBytes;

    }

}