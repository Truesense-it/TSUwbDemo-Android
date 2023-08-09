package it.truesense.uwbdemo.model;

import it.truesense.uwbdemo.utils.Utils;
import java.io.Serializable;

public class UwbDevCfg implements Serializable {
    public byte[] chipFwVersion;
    public byte[] chipId;
    public byte[] deviceMacAddress;
    public byte[] mwVersion;
    public short specVerMajor;
    public short specVerMinor;
    public byte supportedDeviceRangingRoles;
    public int supportedUwbProfileIds;

    public UwbDevCfg() {
        this.chipId = new byte[2];
        this.chipFwVersion = new byte[2];
        this.mwVersion = new byte[3];
    }

    public UwbDevCfg(short s, short s2, byte[] bArr, byte[] bArr2, byte[] bArr3, int i, byte b, byte[] bArr4) {
        this.specVerMajor = s;
        this.specVerMinor = s2;
        this.chipId = bArr;
        this.chipFwVersion = bArr2;
        this.mwVersion = bArr3;
        this.supportedUwbProfileIds = i;
        this.supportedDeviceRangingRoles = b;
        this.deviceMacAddress = bArr4;
    }

    public short getSpecVerMajor() {
        return this.specVerMajor;
    }

    public void setSpecVerMajor(short s) {
        this.specVerMajor = s;
    }

    public short getSpecVerMinor() {
        return this.specVerMinor;
    }

    public void setSpecVerMinor(short s) {
        this.specVerMinor = s;
    }

    public byte[] getChipId() {
        return this.chipId;
    }

    public void setChipId(byte[] bArr) {
        this.chipId = bArr;
    }

    public byte[] getChipFwVersion() {
        return this.chipFwVersion;
    }

    public void setChipFwVersion(byte[] bArr) {
        this.chipFwVersion = bArr;
    }

    public byte[] getMwVersion() {
        return this.mwVersion;
    }

    public void setMwVersion(byte[] bArr) {
        this.mwVersion = bArr;
    }

    public int getSupportedUwbProfileIds() {
        return this.supportedUwbProfileIds;
    }

    public void setSupportedUwbProfileIds(int i) {
        this.supportedUwbProfileIds = i;
    }

    public byte getSupportedDeviceRangingRoles() {
        return this.supportedDeviceRangingRoles;
    }

    public void setSupportedDeviceRangingRoles(byte b) {
        this.supportedDeviceRangingRoles = b;
    }

    public byte[] getDeviceMacAddress() {
        return this.deviceMacAddress;
    }

    public void setDeviceMacAddress(byte[] bArr) {
        this.deviceMacAddress = bArr;
    }

    public byte[] toByteArray() {
        return Utils.concat(Utils.concat(Utils.concat(Utils.concat(Utils.concat(Utils.concat(Utils.concat(Utils.concat((byte[]) null, Utils.shortToByteArray(this.specVerMajor)), Utils.shortToByteArray(this.specVerMinor)), this.chipId), this.chipFwVersion), this.mwVersion), Utils.intToByteArray(this.supportedUwbProfileIds)), Utils.byteToByteArray(this.supportedDeviceRangingRoles)), this.deviceMacAddress);
    }

    public static UwbDevCfg fromByteArray(byte[] bArr) {
        UwbDevCfg uwbDevCfg = new UwbDevCfg();
        uwbDevCfg.setSpecVerMajor(Utils.byteArrayToInt16(Utils.slice(bArr, 2, 0)));
        uwbDevCfg.setSpecVerMinor(Utils.byteArrayToInt16(Utils.slice(bArr, 2, 2)));
        uwbDevCfg.setChipId(Utils.slice(bArr, 2, 4));
        uwbDevCfg.setChipFwVersion(Utils.slice(bArr, 2, 6));
        uwbDevCfg.setMwVersion(Utils.slice(bArr, 3, 8));
        uwbDevCfg.setSupportedUwbProfileIds(Utils.byteArrayToInt32(Utils.slice(bArr, 4, 11)));
        uwbDevCfg.setSupportedDeviceRangingRoles(Utils.byteArrayToInt8(Utils.slice(bArr, 1, 15)));
        uwbDevCfg.setDeviceMacAddress(Utils.slice(bArr, 2, 16));
        return uwbDevCfg;
    }
}
