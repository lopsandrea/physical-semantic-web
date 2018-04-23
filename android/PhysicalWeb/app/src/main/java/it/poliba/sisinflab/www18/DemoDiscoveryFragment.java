package it.poliba.sisinflab.www18;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.physical_web.collection.PhysicalWebCollection;
import org.physical_web.collection.PwPair;
import org.physical_web.physicalweb.Log;
import org.physical_web.physicalweb.PermissionCheck;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.SwipeDismissListViewTouchListener;
import org.physical_web.physicalweb.UrlDeviceDiscoveryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import it.poliba.sisinflab.psw.PswDeviceDiscoveryService;
import it.poliba.sisinflab.psw.PswUtils;
import it.poliba.sisinflab.www18.data.BeaconAdapter;

public class DemoDiscoveryFragment extends Fragment implements UrlDeviceDiscoveryService.UrlDeviceDiscoveryListener {

    private static final String TAG = DemoDiscoveryFragment.class.getSimpleName();
    private static final long FIRST_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1);
    private static final long SECOND_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(3);
    private static final long THIRD_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private List<String> mGroupIdQueue;
    private PhysicalWebCollection mPwCollection = null;
    private Handler mHandler;
    private boolean mSecondScanComplete;
    private boolean mFirstTime;
    private boolean mMissedEmptyGroupIdQueue = false;

    //private WifiDirectConnect mWifiDirectConnect;
    //private BluetoothSite mBluetoothSite;

    private DiscoveryServiceConnection mDiscoveryServiceConnection;
    private static SwipeRefreshLayout mSwipeRefreshLayout;
    private static BeaconAdapter adapter;
    private static RecyclerView rv;

    private Context mContext;

    public static DemoDiscoveryFragment newInstance() {
        DemoDiscoveryFragment fragment = new DemoDiscoveryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDiscoveryServiceConnection = new DiscoveryServiceConnection();
        //mWifiDirectConnect = new WifiDirectConnect(getActivity());
        //mBluetoothSite = new BluetoothSite(getActivity());

        mGroupIdQueue = new ArrayList<>();
        mHandler = new Handler();

        adapter = new BeaconAdapter();

        mContext = getActivity().getBaseContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mFirstTime = true;

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_demo_beacon_discovery, container, false);

        rv = (RecyclerView) view.findViewById(R.id.rv);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity().getBaseContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rv.setLayoutManager(llm);
        rv.setAdapter(adapter);

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

    public void startRefresh() {
        if (!mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(true);
            rv.setClickable(false);
            rv.setEnabled(false);
        }
    }

    public static void stopRefresh() {
        mSwipeRefreshLayout.setRefreshing(false);
        rv.setEnabled(true);
        rv.setClickable(true);
        adapter.sortItems();
        adapter.notifyDataSetChanged();
        Log.d(TAG, "stopRefresh");
    }

    public void restartScan() {
        if (mDiscoveryServiceConnection != null) {
            mDiscoveryServiceConnection.connect(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFirstTime && !PermissionCheck.getInstance().isCheckingPermissions()) {
            restartScan();
        }
        mFirstTime = false;
    }

    /**
     * The connection to the service that discovers urls.
     */
    private class DiscoveryServiceConnection implements ServiceConnection {
        private PswDeviceDiscoveryService mDiscoveryService;
        private boolean mRequestCachedUrlDevices;

        @Override
        public synchronized void onServiceConnected(ComponentName className, IBinder service) {
            // Get the service
            PswDeviceDiscoveryService.PswLocalBinder localBinder =
                    (PswDeviceDiscoveryService.PswLocalBinder) service;
            mDiscoveryService = localBinder.getServiceInstance();

            // Start the scanning display
            mDiscoveryService.addCallback(DemoDiscoveryFragment.this);
            if (!mRequestCachedUrlDevices) {
                mDiscoveryService.restartScan();
            }
            mPwCollection = mDiscoveryService.getPwCollection();
            // Make sure cached results get placed in the mGroupIdQueue.
            onUrlDeviceDiscoveryUpdate();
            startScanningDisplay(mDiscoveryService.getScanStartTime(), mDiscoveryService.hasResults());
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

            //mDiscoveryService.removeCallback(DemoDiscoveryFragment.this);
            mDiscoveryService = null;
            getActivity().unbindService(this);
            stopScanningDisplay();
        }
    }

    private void startScanningDisplay(long scanStartTime, boolean hasResults) {
        // Start the scanning animation only if we don't haven't already been scanning
        // for long enough
        Log.d(TAG, "startScanningDisplay " + scanStartTime + " " + hasResults);
        long elapsedMillis = new Date().getTime() - scanStartTime;
        if (elapsedMillis < FIRST_SCAN_TIME_MILLIS
                || (elapsedMillis < SECOND_SCAN_TIME_MILLIS && !hasResults)) {
            adapter.clear();
            startRefresh();
        } else {
            //mSwipeRefreshLayout.setRefreshing(false);
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

    private void stopScanningDisplay() {
        // Cancel the scan timeout callback if still active or else it may fire later.
        mHandler.removeCallbacks(mFirstScanTimeout);
        mHandler.removeCallbacks(mSecondScanTimeout);
        mHandler.removeCallbacks(mThirdScanTimeout);

        // Change the display appropriately
        //mSwipeRefreshLayout.setRefreshing(false);

        adapter.sortItems();
        adapter.notifyDataSetChanged();
    }

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
                //emptyGroupIdQueue();
                //mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    };

    // Run when the SECOND_SCAN_MILLIS has elapsed.
    private Runnable mSecondScanTimeout = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "running second scan timeout");
            //emptyGroupIdQueue();
            mSecondScanComplete = true;
            //mSwipeRefreshLayout.setRefreshing(false);
        }
    };

    // Run when the THIRD_SCAN_MILLIS has elapsed.
    private Runnable mThirdScanTimeout = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "running third scan timeout");
            mDiscoveryServiceConnection.disconnect();
            //mSwipeRefreshLayout.setRefreshing(false);
        }
    };

    private void emptyGroupIdQueue() {
        if (SwipeDismissListViewTouchListener.isLocked()) {
            mMissedEmptyGroupIdQueue = true;
            return;
        }

        //List<PwPair> pwPairs = new ArrayList<>();

        /*for (String groupId : mGroupIdQueue) {
            Log.d(TAG, "groupid " + groupId);
            //pwPairs.add(Utils.getTopRankedPwPairByGroupId(mPwCollection, groupId));
            pwPairs.add(PswUtils.getTopRankedPwPairByGroupId(mPwCollection, groupId));
        }*/

        //Collections.sort(pwPairs, new Utils.PwPairRelevanceComparator());

        /*Collections.sort(pwPairs, new PswUtils.PwPairSemanticBasedComparator(getActivity()));
        for (PwPair pwPair : pwPairs) {
            adapter.addItem(pwPair, mContext);
        }*/
        mGroupIdQueue.clear();

        adapter.sortItems();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onUrlDeviceDiscoveryUpdate() {
        /*for (PwPair pwPair : mPwCollection.getGroupedPwPairsSortedByRank(
                new PswUtils.PwPairSemanticBasedComparator(this.getActivity()))) {*/

        for (PwPair pwPair : mPwCollection.getPwPairs()) {
            adapter.addItem(pwPair, mContext);

            /*String groupId = Utils.getGroupId(pwPair.getPwsResult());
            Log.d(TAG, "groupid to add " + groupId);
            if (adapter.containsGroupId(groupId)) {
                adapter.addItem(pwPair);
            } else if (!mGroupIdQueue.contains(groupId)
                    && !Utils.isBlocked(pwPair)) {
                mGroupIdQueue.add(groupId);
            }*/
        }

        //adapter.sortItems();

        if(!mSecondScanComplete) { // removed: mGroupIdQueue.isEmpty() ||
            return;
        }
        // Since this callback is given on a background thread and we want
        // to update the list adapter (which can only be done on the UI thread)
        // we have to interact with the adapter on the UI thread.

        /*new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                emptyGroupIdQueue();
            }
        });*/
    }
}
