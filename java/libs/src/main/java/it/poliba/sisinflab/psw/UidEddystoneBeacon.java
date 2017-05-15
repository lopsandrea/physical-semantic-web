package it.poliba.sisinflab.psw;

import org.physical_web.collection.EddystoneBeacon;

/**
 * Physical Semantic Web - Eddystone-UID beacon class. This class represents the
 * Eddystone-UID broadcasting format enhanced for the Physical Semantic Web.
 */
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
