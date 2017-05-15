package it.poliba.sisinflab.owleditor;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.util.ArrayList;

public class OWLObjectPropertyFragment extends ListFragment {

    private static final String TAG = OWLObjectPropertyFragment.class.getSimpleName();
    private OWLExpressionAdapter mAdapter;
    private String mTitle;
    private OWLItem localRequest;
    private int owlType;
    private NumberPicker picker;

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setTitle(mTitle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.owl_list_layout, container, false);
        getActivity().getActionBar().setTitle(mTitle);
        setListAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        if (getArguments() != null) {
            localRequest = getArguments().getParcelable(getString(R.string.owl_request_key));
            owlType = getArguments().getInt(getString(R.string.owl_type_key));
        }

        mAdapter = new OWLExpressionAdapter(getActivity());
        buildOWLTree();
    }

    private void buildOWLTree() {
        mTitle = "Object Property Hierarchy";

        ArrayList<OWLItem> items = ((OWLEditorActivity)getActivity()).mVisitor.getOWLObjectProperties();
        for (OWLItem item : items) {
            item.setType(owlType);
            mAdapter.addItem(item);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
                final OWLItem selected = mAdapter.getItem(position);
                if (!localRequest.getFiller().contains(selected)) {

                    if (selected.getType() == OWLItem.OWL_MAX_CARDINALITY || selected.getType() == OWLItem.OWL_MIN_CARDINALITY) {
                        new AlertDialog.Builder(getActivity())
                            .setView(getNumberPickerLayout())
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    selected.setCardinality(picker.getValue());
                                    localRequest.getFiller().add(selected);
                                    Toast.makeText(getActivity(), selected.getIRI().getFragment() + " added!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    } else {
                        localRequest.getFiller().add(selected);
                        Toast.makeText(getActivity(), selected.getIRI().getFragment() + " added!", Toast.LENGTH_SHORT).show();
                    }

                } else
                    Toast.makeText(getActivity(), selected.getIRI().getFragment() + " already selected!", Toast.LENGTH_SHORT).show();

                return true;
            }
        });
    }

    private FrameLayout getNumberPickerLayout() {
        picker = new NumberPicker(getActivity());
        picker.setMinValue(0);
        picker.setMaxValue(100);

        FrameLayout layout = new FrameLayout(getActivity());
        layout.addView(picker, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER));

        return layout;
    }
}
