package it.poliba.sisinflab.www18;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.physical_web.physicalweb.R;

import java.util.ArrayList;
import java.util.TreeMap;

import it.poliba.sisinflab.www18.data.BeaconStoredAdapter;
import it.poliba.sisinflab.www18.database.StoredBeaconData;

/**
 * A simple {@link Fragment} subclass.
 */
public class DemoFavouriteFragment extends Fragment {

    ArrayList<TreeMap<String,String>> list;
    BeaconStoredAdapter adapter;

    public DemoFavouriteFragment() {
        // Required empty public constructor
    }

    public static DemoFavouriteFragment newInstance() {
        DemoFavouriteFragment fragment = new DemoFavouriteFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_demo_beacon_stored, container, false);

        SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.www18_accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                list = DemoWineActivity.getDatabase().getAllFavourite();
                adapter.update(list);
                adapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        RecyclerView rv = (RecyclerView)view.findViewById(R.id.rv_stored);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity().getBaseContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        while (DemoWineActivity.getDatabase() == null) {}

        list = DemoWineActivity.getDatabase().getAllFavourite();
        adapter = new BeaconStoredAdapter(list, StoredBeaconData.BeaconEntry.FAVOURITE);

        rv.setLayoutManager(llm);
        rv.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

}
