package it.poliba.sisinflab.psw;

import org.physical_web.collection.EddystoneBeacon;

public class UidEddystoneBeacon extends EddystoneBeacon {

    private String mMac;
    private String mOnto;
    private String mId;

    protected UidEddystoneBeacon(byte flags, byte txPower, String mac, String onto, String id) {
        super(flags, txPower, mac);
        mMac = mac;
        mOnto = onto;
        mId = id;
    }

    public String getOntologyID() {
        return mOnto;
    }

    public String getInstanceID() {
        return mId;
    }

    public String getRemoteDeviceMac() {
        return mMac;
    }
}
