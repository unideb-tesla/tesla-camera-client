package com.unideb.tesla.camera.client;

public class DeviceRequest {

    private String mac;
    private String brand;
    private String device;
    private String model;
    private String sdk;
    private String name;
    private int frequency;

    public DeviceRequest(String mac, String brand, String device, String model, String sdk, String name, int frequency) {
        this.mac = mac;
        this.brand = brand;
        this.device = device;
        this.model = model;
        this.sdk = sdk;
        this.name = name;
        this.frequency = frequency;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSdk() {
        return sdk;
    }

    public void setSdk(String sdk) {
        this.sdk = sdk;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    @Override
    public String toString() {
        return "DeviceRequest{" +
                "mac='" + mac + '\'' +
                ", brand='" + brand + '\'' +
                ", device='" + device + '\'' +
                ", model='" + model + '\'' +
                ", sdk='" + sdk + '\'' +
                ", name='" + name + '\'' +
                ", frequency=" + frequency +
                '}';
    }

}
