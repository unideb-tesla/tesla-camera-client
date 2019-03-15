package com.unideb.tesla.camera.dto;

import java.io.Serializable;

public class TeslaMessage implements Serializable {

    private String message;

    public TeslaMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "TeslaMessage{" +
                "message='" + message + '\'' +
                '}';
    }

}