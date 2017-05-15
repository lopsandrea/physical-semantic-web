package it.poliba.sisinflab.owleditor;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

public class OWLIndividualFragment extends ListFragment {

    private static final String TAG = OWLIndividualFragment.class.getSimpleName();
    private OWLExpressionAdapter mAdapter;
    private String mTitle;

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
        setHasOptionsMenu(true);

        mAdapter = new OWLExpressionAdapter(getActivity());

        OWLItem item = null;
        if (getArguments() != null) {
            item = getArguments().getParcelable(getString(R.string.owl_item_key));
        }

        if (item != null) {
            getActivity().getIntent().removeExtra(getString(R.string.owl_item_key));
            buildOWLTree(item);
        } else {
            buildOWLTree();
        }
    }

    private void buildOWLTree(OWLItem root) {
        mTitle = root.getIRI().getFragment();

        ArrayList<OWLItem> items = root.getFiller();
        for (OWLItem item : items) {
            mAdapter.addItem(item);
        }
    }

    private void buildOWLTree() {
        mTitle = "OWL Named Individual";

        ArrayList<OWLItem> items = ((OWLEditorActivity)getActivity()).mVisitor.getOWLIndividuals();
        ((OWLEditorActivity)getActivity()).mVisitor.removeOntology();

        for (OWLItem item : items) {
            mAdapter.addItem(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setTitle(mTitle);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        OWLItem selected = mAdapter.getItem(position);
        if (selected.getFiller().size() > 0) {
            Bundle args = new Bundle();
            args.putParcelable(getString(R.string.owl_item_key), selected);

            OWLIndividualFragment nextFrag = new OWLIndividualFragment();
            nextFrag.setArguments(args);

            ((OWLEditorActivity)getActivity()).showFragment(nextFrag, TAG, true);
        }
    }

}
