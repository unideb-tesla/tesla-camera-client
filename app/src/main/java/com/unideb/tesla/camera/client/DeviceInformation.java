package com.unideb.tesla.camera.client;

public class DeviceInformation {

    private String mac;
    private String brand;
    private String device;
    private String model;
    private String sdk;

    public DeviceInformation(String mac, String brand, String device, String model, String sdk) {
        this.mac = mac;
        this.brand = brand;
        this.device = device;
        this.model = model;
        this.sdk = sdk;
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

    @Override
    public String toString() {
        return "DeviceInformation{" +
                "mac='" + mac + '\'' +
                ", brand='" + brand + '\'' +
                ", device='" + device + '\'' +
                ", model='" + model + '\'' +
                ", sdk='" + sdk + '\'' +
                '}';
    }

}
