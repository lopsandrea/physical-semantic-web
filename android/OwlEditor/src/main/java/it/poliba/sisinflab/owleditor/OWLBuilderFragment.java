package it.poliba.sisinflab.owleditor;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class OWLBuilderFragment extends ListFragment {

    private static final String TAG = OWLBuilderFragment.class.getSimpleName();
    private OWLExpressionAdapter mAdapter;
    private String mTitle;
    private OWLItem localRequest;

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setTitle(mTitle);
        mAdapter.clear();
        for (OWLItem item : localRequest.getFiller())
            mAdapter.addItem(item);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(getActivity().getFragmentManager().getBackStackEntryCount() == 1)
            inflater.inflate(R.menu.editor, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getOrder()) {
            case 0:
                ((OWLEditorActivity)getActivity()).saveOWLRequest("Test");
                return true;
            default:
                return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.owl_builder_layout, container, false);
        getActivity().getActionBar().setTitle(mTitle);
        setListAdapter(mAdapter);

        ImageButton fab = (ImageButton)rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.owl_element_title)
                    .setItems(R.array.owl_element_array, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int pos) {
                            Bundle args = new Bundle();
                            args.putParcelable(getString(R.string.owl_request_key), localRequest);

                            Fragment f = null;
                            switch (pos) {
                                case 0:
                                    args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_CLASS);
                                    f = new OWLClassFragment();
                                    break;
                                case 1:
                                    args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_ALL_VALUES_FROM);
                                    f = new OWLObjectPropertyFragment();
                                    break;
                                case 2:
                                    args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_MIN_CARDINALITY);
                                    f = new OWLObjectPropertyFragment();
                                    break;
                                case 3:
                                    args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_MAX_CARDINALITY);
                                    f = new OWLObjectPropertyFragment();
                                    break;
                                case 4:
                                    args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_COMPLEMENT_OF);
                                    f = new OWLClassFragment();
                                    break;

                                default:
                                    return;
                            }

                            f.setArguments(args);
                            showFragment(f);
                        }
                    });
                builder.create().show();
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mAdapter = new OWLExpressionAdapter(getActivity());

        localRequest = getArguments().getParcelable(getString(R.string.owl_request_key));
        for (OWLItem item : localRequest.getFiller())
            mAdapter.addItem(item);

        mTitle = localRequest.getIRI().getFragment(); //New OWL Request
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        OWLItem selected = mAdapter.getItem(position);
        if (selected.getType() == OWLItem.OWL_ALL_VALUES_FROM) {
            Bundle args = new Bundle();
            args.putParcelable(getString(R.string.owl_request_key), selected);

            OWLBuilderFragment nextFrag = new OWLBuilderFragment();
            nextFrag.setArguments(args);

            ((OWLEditorActivity) getActivity()).showFragment(nextFrag, TAG, true);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
                String name = mAdapter.getItem(position).getIRI().getFragment();
                localRequest.getFiller().remove(position);
                mAdapter.removeItem(position);
                mAdapter.notifyDataSetChanged();
                Toast.makeText(getActivity(), name + " removed!", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void showFragment(Fragment nextFrag) {
        ((OWLEditorActivity)getActivity()).showFragment(nextFrag, TAG, true);
    }

}
