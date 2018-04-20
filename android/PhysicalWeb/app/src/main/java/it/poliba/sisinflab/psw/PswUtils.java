package it.poliba.sisinflab.psw;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.physical_web.collection.PhysicalWebCollection;
import org.physical_web.collection.PwPair;
import org.physical_web.collection.PwsResult;
import org.physical_web.collection.UrlDevice;
import org.physical_web.physicalweb.Log;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.poliba.sisinflab.psw.owl.KBManager;
import it.poliba.sisinflab.www18.DemoWineActivity;

public class PswUtils extends Utils {

    public static final String PSW_URL_DEVICE_TYPE = "psw-url";
    public static final String PSW_UID_DEVICE_TYPE = "psw-uid";

    /**
     * Get the saved setting for enabling Fatbeacon.
     * @param context The context for the SharedPreferences.
     * @return The enable Physical Semantic Web setting.
     */
    public static boolean isPswEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.psw_key), false);
    }

    public static boolean isResolvableDevice(UrlDevice urlDevice) {
        String type = urlDevice.optExtraString(TYPE_KEY, "");
        return type.equals(BLE_DEVICE_TYPE)
            || type.equals(SSDP_DEVICE_TYPE)
            || type.equals(MDNS_PUBLIC_DEVICE_TYPE)
            || type.equals(PSW_URL_DEVICE_TYPE);
    }

    public static boolean isPswDevice(UrlDevice urlDevice) {
        String type = urlDevice.optExtraString(TYPE_KEY, "");
        return type.equals(PSW_UID_DEVICE_TYPE)
            || type.equals(PSW_URL_DEVICE_TYPE);
    }

    public static boolean isPswUidDevice(UrlDevice urlDevice) {
        String type = urlDevice.optExtraString(TYPE_KEY, "");
        return type.equals(PSW_UID_DEVICE_TYPE);
    }

    public static PwsResult getPSWResult(File file, PwsResult pwsResult, boolean load) {
        KBManager mng = DemoWineActivity.getKBManager();
        if (mng != null)
            return mng.getPSWResult(file, pwsResult, load);
        else
            return pwsResult;
    }

    public static String getResourceIRI(PwsResult pwsResult) {
        try {
            return pwsResult.getExtraString(PswDevice.PSW_IRI_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getBeaconUrl(PwsResult pwsResult) {
        try {
            return pwsResult.getExtraString(PswDevice.PSW_BEACON_URL_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getOntologyID(UrlDevice urlDevice) {
        try {
            return urlDevice.getExtraString(PswDevice.PSW_UID_ONTO_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getInstanceID(UrlDevice urlDevice) {
        try {
            return urlDevice.getExtraString(PswDevice.PSW_UID_INST_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getRemoteDeviceMac(UrlDevice urlDevice) {
        try {
            return urlDevice.getExtraString(PswDevice.PSW_UID_MAC_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getRemoteDeviceMacHex(UrlDevice urlDevice) {
        try {
            String mac = urlDevice.getExtraString(PswDevice.PSW_UID_MAC_KEY);
            String hex = "";
            for(int i=0; i<mac.length(); i++) {
                if (i % 2 == 0 && i>0)
                    hex = hex + ":" + mac.substring(i, i+1);
                else
                    hex = hex + mac.substring(i, i+1);
            }
            return hex.toUpperCase();
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getPswUidFragmentName(UrlDevice urlDevice) {
        String mac = getRemoteDeviceMac(urlDevice);
        String onto = getOntologyID(urlDevice);
        String id = getInstanceID(urlDevice);

        return mac + "-" + onto + "-" + id;
    }

    public static PwPair getTopRankedPwPairByGroupId(
        PhysicalWebCollection pwCollection, String groupId, Context context) {
        // This does the same thing as the PhysicalWebCollection method, only it uses our custom
        // getGroupId method.
        for (PwPair pwPair : pwCollection.getGroupedPwPairsSortedByRank(
            new PwPairSemanticBasedComparator(context))) {
            if (getGroupId(pwPair.getPwsResult()).equals(groupId)) {
                return pwPair;
            }
        }
        return null;
    }

    private static double getRssiDistance(UrlDevice urlDevice) {
        try {
            int rssi = Utils.getRssi(urlDevice);
            int MAX_RSSI = -42;
            int MIN_RSSI = -100;

            return (double)(MAX_RSSI - rssi)/(MAX_RSSI - MIN_RSSI);
        } catch (Exception e) {
            return 1;
        }
    }

    public static double getRank(PwsResult pwsResult, UrlDevice urlDevice, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        double beta = Double.parseDouble(preferences.getString(context.getString(R.string.psw_weight_key), "50"))/100.0;
        double alfa = 1 - beta;

        String resource = PswUtils.getResourceIRI(pwsResult);
        double geo_distance = PswUtils.getRssiDistance(urlDevice);
        double sem_rank = 1;
        if (resource != null && DemoWineActivity.getKBManager() != null) {
            sem_rank = DemoWineActivity.getKBManager().getRank(resource);
        }
        double distance = alfa * geo_distance + beta * sem_rank;

        // User's Preferences
        if (DemoWineActivity.getDatabase().isVisited(pwsResult.getRequestUrl()))
            distance = distance * 0.95;

        if (DemoWineActivity.getDatabase().isFavourite(pwsResult.getRequestUrl()))
            distance = distance * 0.9;
        else if (DemoWineActivity.getDatabase().isSpam(pwsResult.getRequestUrl()))
            distance = distance * 1.1;

        if (distance > 1)
            distance = 1;

        return distance;
    }

    public static String getOWL(UrlDevice urlDevice) {
        try {
            if (isPswUidDevice(urlDevice)) {
                File owl = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + getPswUidFragmentName(urlDevice) + ".owl");
                if (owl.exists())
                    return IOUtils.toString(new FileInputStream(owl), StandardCharsets.UTF_8);
            } else {
                Uri url = Uri.parse(urlDevice.getUrl());
                File owl = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + url.getLastPathSegment() + ".owl");
                if (owl.exists())
                    return IOUtils.toString(new FileInputStream(owl), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getOWL(InputStream input) {
        try {
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Compares PwPairs considering semantic-based distance, geographical distance and user preferences
    public static class PwPairSemanticBasedComparator implements Comparator<PwPair> {
        private Set<String> mFavorites = mFavoriteUrls;
        public Map<String, Double> mCachedDistances;
        Context mContext;

        public PwPairSemanticBasedComparator(Context context) {
            mCachedDistances = new HashMap<>();
            mContext = context;
        }

        public double getDistance(PwsResult pwsResult, UrlDevice urlDevice) {
            if (mCachedDistances.containsKey(urlDevice.getId())) {
                return mCachedDistances.get(urlDevice.getId());
            }

            double distance = PswUtils.getRank(pwsResult, urlDevice, mContext);
            mCachedDistances.put(urlDevice.getId(), distance);
            return distance;
        }

        @Override
        public int compare(PwPair lhs, PwPair rhs) {
            if (lhs == null || rhs == null)
                return 0;

            String lSite = lhs.getPwsResult().getSiteUrl();
            String rSite = rhs.getPwsResult().getSiteUrl();

            /*if (mFavorites.contains(lSite) == mFavorites.contains(rSite)) {
                return Double.compare(getDistance(lhs.getPwsResult(), lhs.getUrlDevice()),
                    getDistance(rhs.getPwsResult(), rhs.getUrlDevice()));
            } else {
                if (mFavorites.contains(lSite)) {
                    return -1;
                }
                return 1;
            }*/

            return Double.compare(getDistance(lhs.getPwsResult(), lhs.getUrlDevice()),
                    getDistance(rhs.getPwsResult(), rhs.getUrlDevice()));
        }
    }

    public static Intent createNavigateToUrlIntent(PwsResult pwsResult) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getBeaconUrl(pwsResult)));
        return intent;
    }

    public static boolean isObsolete(File file, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long cacheTime = Integer.parseInt(preferences.getString(context.getString(R.string.psw_cache_key), "60"))*1000;
        long diff = System.currentTimeMillis() - file.lastModified();
        if (diff > cacheTime) {
            // delete current files
            file.delete();
            if(file.exists())
                context.deleteFile(file.getName());

            return true;
        } else {
            return false;
        }
    }
}
