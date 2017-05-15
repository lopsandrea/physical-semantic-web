package it.poliba.sisinflab.owleditor;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class OWLClassFragment extends ListFragment {

    private static final String TAG = OWLClassFragment.class.getSimpleName();
    private OWLExpressionAdapter mAdapter;
    private String mTitle;
    private OWLItem localRequest;
    private int owlType;

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

        mAdapter = new OWLExpressionAdapter(getActivity());

        OWLItem item = null;
        if (getArguments() != null) {
            item = getArguments().getParcelable(getString(R.string.owl_item_key));
            localRequest = getArguments().getParcelable(getString(R.string.owl_request_key));
            owlType = getArguments().getInt(getString(R.string.owl_type_key));
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

        ArrayList<OWLItem> items = ((OWLEditorActivity)getActivity()).mVisitor.getOWLSubClassOf(root.getIRI());
        for (OWLItem item : items) {
            mAdapter.addItem(item);
        }

        if (items.size() == 0) {
            Toast.makeText(getActivity(), getString(R.string.no_subclasses), Toast.LENGTH_SHORT).show();
            getActivity().getFragmentManager().popBackStack();
        }
    }

    private void buildOWLTree() {
        mTitle = "Class Hierarchy";

        ArrayList<OWLItem> items = ((OWLEditorActivity)getActivity()).mVisitor.getOWLSubClassOf();
        for (OWLItem item : items) {
            mAdapter.addItem(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        OWLItem selected = mAdapter.getItem(position);
        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.owl_item_key), selected);
        args.putParcelable(getString(R.string.owl_request_key), localRequest);
        args.putInt(getString(R.string.owl_type_key), owlType);

        OWLClassFragment nextFrag = new OWLClassFragment();
        nextFrag.setArguments(args);

        ((OWLEditorActivity)getActivity()).showFragment(nextFrag, TAG, true);
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
                OWLItem selected = mAdapter.getItem(position);
                selected.setType(owlType);
                if (!localRequest.getFiller().contains(selected)) {
                    localRequest.getFiller().add(selected);
                    Toast.makeText(getActivity(), selected.getIRI().getFragment() + " added!", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getActivity(), selected.getIRI().getFragment() + " already selected!", Toast.LENGTH_SHORT).show();

                return true;
            }
        });
    }

}
