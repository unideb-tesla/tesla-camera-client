package com.unideb.tesla.camera.dto;

import java.io.Serializable;
import java.util.Arrays;

public class DisclosureSchedule implements Serializable {

    private long intervalDuration;
    private long startTime;
    private int intervalIndex;
    private int keychainLength;
    private int disclosureDelay;
    private byte[] keyCommitment;

    public DisclosureSchedule(long intervalDuration, long startTime, int intervalIndex, int keychainLength, int disclosureDelay, byte[] keyCommitment) {
        this.intervalDuration = intervalDuration;
        this.startTime = startTime;
        this.intervalIndex = intervalIndex;
        this.keychainLength = keychainLength;
        this.disclosureDelay = disclosureDelay;
        this.keyCommitment = keyCommitment;
    }

    public long getIntervalDuration() {
        return intervalDuration;
    }

    public void setIntervalDuration(long intervalDuration) {
        this.intervalDuration = intervalDuration;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getIntervalIndex() {
        return intervalIndex;
    }

    public void setIntervalIndex(int intervalIndex) {
        this.intervalIndex = intervalIndex;
    }

    public int getKeychainLength() {
        return keychainLength;
    }

    public void setKeychainLength(int keychainLength) {
        this.keychainLength = keychainLength;
    }

    public int getDisclosureDelay() {
        return disclosureDelay;
    }

    public void setDisclosureDelay(int disclosureDelay) {
        this.disclosureDelay = disclosureDelay;
    }

    public byte[] getKeyCommitment() {
        return keyCommitment;
    }

    public void setKeyCommitment(byte[] keyCommitment) {
        this.keyCommitment = keyCommitment;
    }

    @Override
    public String toString() {
        return "DisclosureSchedule{" +
                "intervalDuration=" + intervalDuration +
                ", startTime=" + startTime +
                ", intervalIndex=" + intervalIndex +
                ", keychainLength=" + keychainLength +
                ", disclosureDelay=" + disclosureDelay +
                ", keyCommitment=" + Arrays.toString(keyCommitment) +
                '}';
    }

}