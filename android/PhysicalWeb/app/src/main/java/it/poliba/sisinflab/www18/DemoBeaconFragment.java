package it.poliba.sisinflab.www18;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.physical_web.collection.PhysicalWebCollection;
import org.physical_web.collection.PwPair;
import org.physical_web.physicalweb.BluetoothSite;
import org.physical_web.physicalweb.Log;
import org.physical_web.physicalweb.PermissionCheck;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.UrlDeviceDiscoveryService;
import org.physical_web.physicalweb.Utils;
import org.physical_web.physicalweb.WifiDirectConnect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import it.poliba.sisinflab.psw.PswDeviceDiscoveryService;
import it.poliba.sisinflab.psw.PswUtils;
import it.poliba.sisinflab.www18.data.BeaconAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DemoBeaconFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DemoBeaconFragment extends Fragment implements UrlDeviceDiscoveryService.UrlDeviceDiscoveryListener {

    final static long MIN_UPDATE_DELAY = 10000;

    private static final long FIRST_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(2);
    private static final long SECOND_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final long THIRD_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private boolean mSecondScanComplete;
    private boolean mFirstTime;
    private Handler mHandler;

    // The display of gathered urls happens as follows
    // 0. Begin scan
    // 1. Sort and show all urls (mFirstScanTimeout)
    // 2. Sort and show all new urls beneath the first set (mSecondScanTimeout)
    // 3. Show each new url at bottom of list as it comes in
    // 4. Stop scanning (mThirdScanTimeout)

    // Run when the FIRST_SCAN_MILLIS has elapsed.
    private Runnable mFirstScanTimeout = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "running first scan timeout");
            if (!mGroupIdQueue.isEmpty()) {
                emptyGroupIdQueue();
            }
        }
    };

    // Run when the SECOND_SCAN_MILLIS has elapsed.
    private Runnable mSecondScanTimeout = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "running second scan timeout");
            emptyGroupIdQueue();
            mSecondScanComplete = true;
        }
    };

    // Run when the THIRD_SCAN_MILLIS has elapsed.
    private Runnable mThirdScanTimeout = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "running third scan timeout");
            mDiscoveryServiceConnection.disconnect();
        }
    };

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private long lastUpdate = 0;

    private BeaconAdapter adapter;
    private DiscoveryServiceConnection mDiscoveryServiceConnection;
    private List<String> mGroupIdQueue;
    private PhysicalWebCollection mPwCollection = null;
    private WifiDirectConnect mWifiDirectConnect;
    private BluetoothSite mBluetoothSite;
    private PswDeviceDiscoveryService mDiscoveryService;
    private String TAG = DemoBeaconFragment.class.getName();

    public DemoBeaconFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DemoBeaconFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DemoBeaconFragment newInstance() {
        DemoBeaconFragment fragment = new DemoBeaconFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDiscoveryServiceConnection = new DiscoveryServiceConnection();
        mWifiDirectConnect = new WifiDirectConnect(getActivity());
        mBluetoothSite = new BluetoothSite(getActivity());

        mGroupIdQueue = new ArrayList<>();
        mHandler = new Handler();

        //Utils.restoreFavorites(getActivity());
        //Utils.restoreBlocked(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mFirstTime = true;

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_demo_beacon_discovery, container, false);

        if (adapter == null || adapter.getItemCount()==0) {
            RecyclerView rv = (RecyclerView) view.findViewById(R.id.rv);

            LinearLayoutManager llm = new LinearLayoutManager(getActivity().getBaseContext());
            llm.setOrientation(LinearLayoutManager.VERTICAL);

            adapter = new BeaconAdapter();
            rv.setLayoutManager(llm);
            //rv.setHasFixedSize(true);
            rv.setAdapter(adapter);
        }

        ImageButton fab = (ImageButton) view.findViewById(R.id.fabDemoBeacons);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetAdapter();
                restartScan();
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshDiscoveryLayout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.www18_accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                restartScan();
            }
        });

        return view;
    }

    private void stopScanningDisplay() {
        // Cancel the scan timeout callback if still active or else it may fire later.
        mHandler.removeCallbacks(mFirstScanTimeout);
        mHandler.removeCallbacks(mSecondScanTimeout);
        mHandler.removeCallbacks(mThirdScanTimeout);

        // Change the display appropriately
        //setRefreshWidgetInvisible();
    }

    private void startScanningDisplay(long scanStartTime, boolean hasResults) {
        // Start the scanning animation only if we don't haven't already been scanning
        // for long enough
        Log.d(TAG, "startScanningDisplay " + scanStartTime + " " + hasResults);
        long elapsedMillis = new Date().getTime() - scanStartTime;
        if (elapsedMillis < FIRST_SCAN_TIME_MILLIS
                || (elapsedMillis < SECOND_SCAN_TIME_MILLIS && !hasResults)) {
            adapter.clear();
        } else {
            //setRefreshWidgetInvisible();
        }

        // Schedule the timeouts
        mSecondScanComplete = false;
        long firstDelay = Math.max(FIRST_SCAN_TIME_MILLIS - elapsedMillis, 0);
        long secondDelay = Math.max(SECOND_SCAN_TIME_MILLIS - elapsedMillis, 0);
        long thirdDelay = Math.max(THIRD_SCAN_TIME_MILLIS - elapsedMillis, 0);
        mHandler.postDelayed(mFirstScanTimeout, firstDelay);
        mHandler.postDelayed(mSecondScanTimeout, secondDelay);
        mHandler.postDelayed(mThirdScanTimeout, thirdDelay);
    }

    @Override
    public void onUrlDeviceDiscoveryUpdate() {
        long deltaUpd = System.currentTimeMillis() - lastUpdate;
        if (deltaUpd > MIN_UPDATE_DELAY) {

            for (PwPair pwPair : mPwCollection.getPwPairsSortedByRank(
                    new PswUtils.PwPairSemanticBasedComparator(this.getActivity()))) {

                adapter.addItem(pwPair, getContext());


                /*String groupId = Utils.getGroupId(pwPair.getPwsResult());
                Log.d(TAG, "groupid to add " + groupId);
                if (adapter.containsGroupId(groupId)) {
                    adapter.updateItem(pwPair);
                } else if (!mGroupIdQueue.contains(groupId)
                        && !Utils.isBlocked(pwPair)) {
                    mGroupIdQueue.add(groupId);
                }*/
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });

            lastUpdate = System.currentTimeMillis();
        }

        if(mGroupIdQueue.isEmpty()) {
            return;
        }

        // Since this callback is given on a background thread and we want
        // to update the list adapter (which can only be done on the UI thread)
        // we have to interact with the adapter on the UI thread.
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                emptyGroupIdQueue();
            }
        });

        adapter.setItems(mPwCollection.getPwPairs(), getContext());
    }

    /**
     * The connection to the service that discovers urls.
     */

    private class DiscoveryServiceConnection implements ServiceConnection {

        private boolean mRequestCachedUrlDevices;

        @Override
        public synchronized void onServiceConnected(ComponentName className, IBinder service) {
            // Get the service
            PswDeviceDiscoveryService.PswLocalBinder localBinder =
                    (PswDeviceDiscoveryService.PswLocalBinder) service;
            mDiscoveryService = localBinder.getServiceInstance();

            // Start the scanning display
            mDiscoveryService.addCallback(DemoBeaconFragment.this);
            if (!mRequestCachedUrlDevices) {
                mDiscoveryService.restartScan();
            }
            mPwCollection = mDiscoveryService.getPwCollection();
            // Make sure cached results get placed in the mGroupIdQueue.
            onUrlDeviceDiscoveryUpdate();
            startScanningDisplay(mDiscoveryService.getScanStartTime(), mDiscoveryService.hasResults());
            restartScan();
        }

        @Override
        public synchronized void onServiceDisconnected(ComponentName className) {
            // onServiceDisconnected gets called when the connection is unintentionally disconnected,
            // which should never happen for us since this is a local service
            mDiscoveryService = null;
        }

        public synchronized void connect(boolean requestCachedUrlDevices) {
            if (mDiscoveryService != null) {
                return;
            }

            mRequestCachedUrlDevices = requestCachedUrlDevices;
            Intent intent = new Intent(getActivity(), PswDeviceDiscoveryService.class);
            getActivity().startService(intent);
            getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
        }

        public synchronized void disconnect() {
            if (mDiscoveryService == null) {
                return;
            }

            mDiscoveryService.removeCallback(DemoBeaconFragment.this);
            mDiscoveryService = null;
            getActivity().unbindService(this);

            stopScanningDisplay();
        }
    }

    private void startScanning() {
        adapter.clear();
        Log.d(TAG, "startScanning");
        emptyGroupIdQueue();
    }

    private void emptyGroupIdQueue() {
        List<PwPair> pwPairs = new ArrayList<>();

        for (String groupId : mGroupIdQueue) {
            Log.d(TAG, "groupid " + groupId);
           pwPairs.add(PswUtils.getTopRankedPwPairByGroupId(mPwCollection, groupId));
        }

        if (mPwCollection != null)
            pwPairs = mPwCollection.getPwPairs();

        //Collections.sort(pwPairs, new Utils.PwPairRelevanceComparator());
        Collections.sort(pwPairs, new PswUtils.PwPairSemanticBasedComparator(getActivity()));
        for (PwPair pwPair : pwPairs) {
            adapter.addItem(pwPair, getContext());
        }
        mGroupIdQueue.clear();
    }

    @Override
    public void onResume() {
        super.onResume();

        mFirstTime = false;

        /*if (!PermissionCheck.getInstance().isCheckingPermissions()) {
            restartScan();
        }*/
    }

    public void resetAdapter() {
        mPwCollection.clear();
        mGroupIdQueue.clear();
        adapter.notifyDataSetChanged();
    }

    public void restartScan() {
        if (mPwCollection != null) {
            //mPwCollection.getPwPairs().clear();
            mPwCollection.clear();
            mPwCollection.cancelAllRequests();
        }

        adapter.clear();
        //emptyGroupIdQueue();

        Log.d(TAG, "startScanning");
        if (mDiscoveryServiceConnection != null) {
            mDiscoveryServiceConnection.connect(true);
        }

        if(mDiscoveryService != null)
            mDiscoveryService.restartScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDiscoveryServiceConnection.disconnect();
    }

}
