package com.entgra.mqtt;

import java.util.List;

/**
 * This class is use to wrap and send device configuration data
 * to the device
 */
public class DeviceConfiguration {

    private String deviceId;
    private String deviceType;
    private String tenantDomain;
    private List<ConfigurationEntry> configurationEntries;
    private String accessToken;
    private String refreshToken;
    private String deviceOwner;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public List<ConfigurationEntry> getConfigurationEntries() {
        return configurationEntries;
    }

    public void setConfigurationEntries(
            List<ConfigurationEntry> configurationEntries) {
        this.configurationEntries = configurationEntries;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getDeviceOwner() {
        return deviceOwner;
    }

    public void setDeviceOwner(String deviceOwner) {
        this.deviceOwner = deviceOwner;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
