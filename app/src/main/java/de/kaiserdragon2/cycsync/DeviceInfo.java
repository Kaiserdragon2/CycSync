package de.kaiserdragon2.cycsync;

public class DeviceInfo {
    private final String deviceName;
    private final String macAddress;

    public DeviceInfo(String deviceName, String macAddress) {
        this.deviceName = deviceName;
        this.macAddress = macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DeviceInfo) {
            DeviceInfo appInfo = (DeviceInfo) object;
            return this.getMacAddress().equals(appInfo.getMacAddress());
        }
        return false;
    }

}
