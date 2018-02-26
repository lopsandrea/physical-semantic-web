package it.poliba.sisinflab.www18;


import android.app.Fragment;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import org.physical_web.physicalweb.R;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 */
public class DemoInfoFragment extends ListFragment {

    public DemoInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_demo_info, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ArrayList<TreeMap<String,String>> list = (ArrayList<TreeMap<String, String>>) getActivity().getIntent().getSerializableExtra(getString(R.string.beacon_properties_key));

        SimpleAdapter adapter = new SimpleAdapter(getActivity(), list,
                R.layout.list_item_beacon_property,
                new String[] { "line1","line2" },
                new int[] {R.id.line_a, R.id.line_b});

        setListAdapter(adapter);
    }

}
