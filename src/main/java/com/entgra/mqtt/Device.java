/*
 *  Copyright (c) 2019, Entgra (pvt) Ltd. (http://entgra.io)
 *
 *  All Rights Reserved.
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited.
 *  Proprietary and confidential.
 */

package com.entgra.mqtt;

public class Device {
    private String deviceIdentifier;
    private String deviceName;
    private String macAddress;
    private String type;
    private String floorId;
    private String token;
    private String fwVersion;
    private String manufacturer;
    private String model;
    private String machineCategory;
    private String serialNo;
    private String linePlacementId;
    private String linePlacementX;
    private String linePlacementY;

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getLinePlacementId() {
        return linePlacementId;
    }

    public void setLinePlacementId(String linePlacementId) {
        this.linePlacementId = linePlacementId;
    }

    public String getLinePlacementX() {
        return linePlacementX;
    }

    public void setLinePlacementX(String linePlacementX) {
        this.linePlacementX = linePlacementX;
    }

    public String getLinePlacementY() {
        return linePlacementY;
    }

    public void setLinePlacementY(String linePlacementY) {
        this.linePlacementY = linePlacementY;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMachineCategory() {
        return machineCategory;
    }

    public void setMachineCategory(String machineCategory) {
        this.machineCategory = machineCategory;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFloorId() {
        return floorId;
    }

    public void setFloorId(String floorId) {
        this.floorId = floorId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFwVersion() {
        return fwVersion;
    }

    public void setFwVersion(String fwVersion) {
        this.fwVersion = fwVersion;
    }
}
