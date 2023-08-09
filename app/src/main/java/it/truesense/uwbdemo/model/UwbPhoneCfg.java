package it.truesense.uwbdemo.model;

import it.truesense.uwbdemo.utils.Utils;
import java.io.Serializable;

public class UwbPhoneCfg implements Serializable {
    byte channel;
    byte deviceRangingRole;
    byte[] phoneMacAddress;
    byte preambleIndex;
    byte profileId;
    int sessionId;
    short specVerMajor;
    short specVerMinor;

    public UwbPhoneCfg() {
    }

    public UwbPhoneCfg(short verMaj, short verMin, int sessId, byte preambleIndex, byte channel, byte profileId, byte rangeRole, byte[] phoneMacAddr) {
        this.specVerMajor = verMaj;
        this.specVerMinor = verMin;
        this.sessionId = sessId;
        this.preambleIndex = preambleIndex;
        this.channel = channel;
        this.profileId = profileId;
        this.deviceRangingRole = rangeRole;
        this.phoneMacAddress = phoneMacAddr;
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

    public int getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(int i) {
        this.sessionId = i;
    }

    public byte getPreambleIndex() {
        return this.preambleIndex;
    }

    public void setPreambleIndex(byte b) {
        this.preambleIndex = b;
    }

    public byte getChannel() {
        return this.channel;
    }

    public void setChannel(byte b) {
        this.channel = b;
    }

    public byte getProfileId() {
        return this.profileId;
    }

    public void setProfileId(byte b) {
        this.profileId = b;
    }

    public byte getDeviceRangingRole() {
        return this.deviceRangingRole;
    }

    public void setDeviceRangingRole(byte b) {
        this.deviceRangingRole = b;
    }

    public byte[] getPhoneMacAddress() {
        return this.phoneMacAddress;
    }

    public void setPhoneMacAddress(byte[] bArr) {
        this.phoneMacAddress = bArr;
    }

    public byte[] toByteArray() {
        return Utils.concat(Utils.concat(Utils.concat(Utils.concat(Utils.concat(Utils.concat(Utils.concat(Utils.concat((byte[]) null, Utils.shortToByteArray(this.specVerMajor)), Utils.shortToByteArray(this.specVerMinor)), Utils.intToByteArray(this.sessionId)), Utils.byteToByteArray(this.preambleIndex)), Utils.byteToByteArray(this.channel)), Utils.byteToByteArray(this.profileId)), Utils.byteToByteArray(this.deviceRangingRole)), this.phoneMacAddress);
    }

    public static UwbPhoneCfg fromByteArray(byte[] bArr) {
        UwbPhoneCfg uwbPhoneCfg = new UwbPhoneCfg();
        uwbPhoneCfg.setSpecVerMajor(Utils.byteArrayToInt16(Utils.slice(bArr, 2, 0)));
        uwbPhoneCfg.setSpecVerMinor(Utils.byteArrayToInt16(Utils.slice(bArr, 2, 2)));
        uwbPhoneCfg.setSessionId(Utils.byteArrayToInt16(Utils.slice(bArr, 4, 4)));
        uwbPhoneCfg.setPreambleIndex(Utils.byteArrayToInt8(Utils.slice(bArr, 1, 8)));
        uwbPhoneCfg.setChannel(Utils.byteArrayToInt8(Utils.slice(bArr, 1, 9)));
        uwbPhoneCfg.setProfileId(Utils.byteArrayToInt8(Utils.slice(bArr, 1, 10)));
        uwbPhoneCfg.setDeviceRangingRole(Utils.byteArrayToInt8(Utils.slice(bArr, 1, 11)));
        uwbPhoneCfg.setPhoneMacAddress(Utils.slice(bArr, 2, 12));
        return uwbPhoneCfg;
    }


}
