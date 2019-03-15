package com.unideb.tesla.camera.client;

import com.unideb.tesla.camera.dto.TeslaMessage;

import java.util.Arrays;

public class Triplet {

    private int intervalIndex;
    private TeslaMessage teslaMessage;
    private byte[] MAC;

    public Triplet(int intervalIndex, TeslaMessage teslaMessage, byte[] MAC) {
        this.intervalIndex = intervalIndex;
        this.teslaMessage = teslaMessage;
        this.MAC = MAC;
    }

    public int getIntervalIndex() {
        return intervalIndex;
    }

    public void setIntervalIndex(int intervalIndex) {
        this.intervalIndex = intervalIndex;
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

    @Override
    public String toString() {
        return "Triplet{" +
                "intervalIndex=" + intervalIndex +
                ", teslaMessage=" + teslaMessage +
                ", MAC=" + Arrays.toString(MAC) +
                '}';
    }

}