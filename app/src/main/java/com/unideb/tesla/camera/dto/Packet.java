package com.unideb.tesla.camera.dto;

import java.io.Serializable;
import java.util.Arrays;

public class Packet implements Serializable {

    private TeslaMessage teslaMessage;
    private byte[] MAC;
    private byte[] disclosedKey;

    public Packet(TeslaMessage teslaMessage, byte[] MAC, byte[] disclosedKey) {
        this.teslaMessage = teslaMessage;
        this.MAC = MAC;
        this.disclosedKey = disclosedKey;
    }

    public TeslaMessage getTeslaMessage() {
        return teslaMessage;
    }

    public void setTeslaMessage(TeslaMessage teslaMessage) {
        this.teslaMessage = teslaMessage;
    }

    public byte[] getMAC() {
        return MAC;
    }

    public void setMAC(byte[] MAC) {
        this.MAC = MAC;
    }

    public byte[] getDisclosedKey() {
        return disclosedKey;
    }

    public void setDisclosedKey(byte[] disclosedKey) {
        this.disclosedKey = disclosedKey;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "teslaMessage=" + teslaMessage +
                ", MAC=" + Arrays.toString(MAC) +
                ", disclosedKey=" + Arrays.toString(disclosedKey) +
                '}';
    }

}