/*
 * Copyright 2017 SisInfLab @ Polytechnic University of Bari - All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.poliba.sisinflab.psw;

import org.physical_web.collection.EddystoneBeacon;

import java.util.Arrays;

/**
 * Physical Semantic Web - Eddystone beacon class.
 * This class represents the Eddystone broadcasting format enhanced for the Physical Semantic Web.
 */
public class PswEddystoneBeacon extends EddystoneBeacon {

    public static final byte PSW_UID_FRAME_TYPE = 0x01;
    public static final byte PSW_URL_FRAME_TYPE = 0x11;

    private PswEddystoneBeacon(byte flags, byte txPower, String url) {
        super(flags, txPower, url);
    }

    public static boolean isPswUrlFrame(byte[] serviceData) {
        return serviceData != null && serviceData.length > 0 &&
            serviceData[0] == PSW_URL_FRAME_TYPE;
    }

    public static boolean isPswUidFrame(byte[] serviceData) {
        return serviceData != null && serviceData.length > 0 &&
            serviceData[0] == PSW_UID_FRAME_TYPE;
    }

    public static UidEddystoneBeacon parseUidFromServiceData(byte[] serviceData) {
        if (serviceData != null && serviceData.length > 2) {
            byte flags = (byte) (serviceData[0] & 0x0f);
            byte txPower = serviceData[1];

            byte[] bOnto = Arrays.copyOfRange(serviceData, 2, 6);
            byte[] bIns = Arrays.copyOfRange(serviceData, 6, 12);
            byte[] bMac = Arrays.copyOfRange(serviceData, 12, 18);

            if (bOnto == null || bIns == null || bMac == null) {
                return null;
            }



            return new UidEddystoneBeacon(flags, txPower, bytesToHex(bMac), new String(bOnto), new String(bIns));
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for(byte b: bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
