package com.unideb.tesla.camera.dto;

import java.io.Serializable;
import java.util.List;

public class TeslaMessage implements Serializable {

    private String message;
    private List<String> targetDevices;

    public TeslaMessage(String message, List<String> targetDevices) {
        this.message = message;
        this.targetDevices = targetDevices;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getTargetDevices() {
        return targetDevices;
    }

    public void setTargetDevices(List<String> targetDevices) {
        this.targetDevices = targetDevices;
    }

    @Override
    public String toString() {
        return "TeslaMessage{" +
                "message='" + message + '\'' +
                ", targetDevices=" + targetDevices +
                '}';
    }

}