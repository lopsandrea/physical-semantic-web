package it.poliba.sisinflab.owleditor;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.semanticweb.owlapi.model.IRI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
            case 1:
                mAdapter.clear();
                localRequest = new OWLItem(OWLItem.OWL_INDIVIDUAL, IRI.create(getString(R.string.owl_request_default_name)));
                mAdapter.notifyDataSetChanged();
                return true;
            default:
                return false;
        }
    }

    //Linear layout holding the Save submenu
    boolean fabExpanded = false;
    private LinearLayout reqList;
    private LinearLayout layoutFabClass;
    private LinearLayout layoutFabNot;
    private LinearLayout layoutFabAllValues;
    private LinearLayout layoutFabMin;
    private LinearLayout layoutFabMax;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.owl_builder_layout, container, false);
        getActivity().getActionBar().setTitle(mTitle);
        setListAdapter(mAdapter);

        reqList = (LinearLayout) rootView.findViewById(R.id.req_list);
        layoutFabClass = (LinearLayout) rootView.findViewById(R.id.layoutFabClass);
        layoutFabNot = (LinearLayout) rootView.findViewById(R.id.layoutFabNot);
        layoutFabAllValues = (LinearLayout) rootView.findViewById(R.id.layoutFabAllValues);
        layoutFabMin = (LinearLayout) rootView.findViewById(R.id.layoutFabMin);
        layoutFabMax = (LinearLayout) rootView.findViewById(R.id.layoutFabMax);
        closeSubMenusFab();

        setFabMenuAction((ImageButton)rootView.findViewById(R.id.fabMiniClass), OWLItem.OWL_CLASS);
        setFabMenuAction((ImageButton)rootView.findViewById(R.id.fabMiniNot), OWLItem.OWL_COMPLEMENT_OF);
        setFabMenuAction((ImageButton)rootView.findViewById(R.id.fabMiniAllValues), OWLItem.OWL_ALL_VALUES_FROM);
        setFabMenuAction((ImageButton)rootView.findViewById(R.id.fabMiniMax), OWLItem.OWL_MAX_CARDINALITY);
        setFabMenuAction((ImageButton)rootView.findViewById(R.id.fabMiniMin), OWLItem.OWL_MIN_CARDINALITY);

        ImageButton fab = (ImageButton)rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                /*AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                builder.create().show();*/

                if (fabExpanded)
                    closeSubMenusFab();
                else
                    openSubMenusFab();
            }
        });

        return rootView;
    }

    private void setFabMenuAction(ImageButton button, final int type) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args = new Bundle();
                args.putParcelable(getString(R.string.owl_request_key), localRequest);
                args.putInt(getString(R.string.owl_type_key), type);

                Fragment f = null;
                switch (type) {
                    case OWLItem.OWL_CLASS:
                        args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_CLASS);
                        f = new OWLClassFragment();
                        break;
                    case OWLItem.OWL_ALL_VALUES_FROM:
                        args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_ALL_VALUES_FROM);
                        f = new OWLObjectPropertyFragment();
                        break;
                    case OWLItem.OWL_MIN_CARDINALITY:
                        args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_MIN_CARDINALITY);
                        f = new OWLObjectPropertyFragment();
                        break;
                    case OWLItem.OWL_MAX_CARDINALITY:
                        args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_MAX_CARDINALITY);
                        f = new OWLObjectPropertyFragment();
                        break;
                    case OWLItem.OWL_COMPLEMENT_OF:
                        args.putInt(getString(R.string.owl_type_key), OWLItem.OWL_COMPLEMENT_OF);
                        f = new OWLClassFragment();
                        break;

                    default:
                        return;
                }

                f.setArguments(args);
                showFragment(f);
                closeSubMenusFab();
            }
        });
    }

    private void closeSubMenusFab(){
        reqList.setAlpha((float) 1.0);
        reqList.setClickable(true);
        layoutFabClass.setVisibility(View.INVISIBLE);
        layoutFabNot.setVisibility(View.INVISIBLE);
        layoutFabAllValues.setVisibility(View.INVISIBLE);
        layoutFabMin.setVisibility(View.INVISIBLE);
        layoutFabMax.setVisibility(View.INVISIBLE);
        fabExpanded = false;
    }

    private void openSubMenusFab(){
        reqList.setAlpha((float) 0.2);
        reqList.setClickable(false);
        layoutFabClass.setVisibility(View.VISIBLE);
        layoutFabNot.setVisibility(View.VISIBLE);
        layoutFabAllValues.setVisibility(View.VISIBLE);
        layoutFabMax.setVisibility(View.VISIBLE);
        layoutFabMin.setVisibility(View.VISIBLE);
        fabExpanded = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mAdapter = new OWLExpressionAdapter(getActivity());
        localRequest = getArguments().getParcelable(getString(R.string.owl_request_key));

        String path = Environment.getExternalStorageDirectory().toString();
        File owl = new File(path + "/owleditor", localRequest.getIRI().getFragment() + ".owl");
        if (owl.exists())
            loadUserRequest(owl);

        for (OWLItem item : localRequest.getFiller())
            mAdapter.addItem(item);

        mTitle = localRequest.getIRI().getFragment(); //New OWL Request
    }

    private void loadUserRequest(File file) {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            String owl = convertStreamToString(fin);
            OWLVisitor visitor = new OWLVisitor(owl);

            if (visitor.getOWLIndividuals().size()>0)
                localRequest = visitor.getOWLIndividuals().get(0);

            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
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
