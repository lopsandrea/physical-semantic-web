package it.poliba.sisinflab.owleditor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import org.semanticweb.owlapi.model.IRI;

import java.io.File;

public class OWLEditorActivity extends FragmentActivity {

    String TAG = OWLEditorActivity.class.getSimpleName();

    OWLVisitor mVisitor;

    Fragment f = null;
    public static boolean saved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        saved = true;

        int style = R.style.AppTheme;
        String fragment = getIntent().getStringExtra(getString(R.string.owl_fragment_key));
        if (fragment != null) {
            buildVisitor();

            if (fragment.equals(OWLIndividualFragment.class.getSimpleName())) {
                f = new OWLIndividualFragment();
            } else if (fragment.equals(OWLBuilderFragment.class.getSimpleName())) {
                OWLItem mRequest = new OWLItem(OWLItem.OWL_INDIVIDUAL, IRI.create(getString(R.string.owl_request_default_name)));
                f = new OWLBuilderFragment();
                Bundle args = new Bundle();
                args.putParcelable(getString(R.string.owl_request_key), mRequest);
                f.setArguments(args);
            } else
                finish();

            setCustomTheme(style);
            showFragment(f, fragment, true);
            getIntent().removeExtra(getString(R.string.owl_fragment_key));
        } else
            finish();
    }

    private void setCustomTheme(int style) {
        setTheme(style);
        setContentView(R.layout.owl_editor_activity);
    }

    @SuppressLint("CommitTransaction")
    public void showFragment(Fragment newFragment, String fragmentTag, boolean addToBackStack) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.owl_editor_activity_container, newFragment, fragmentTag);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 1) {
            if (f instanceof OWLBuilderFragment && !saved) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Warning");
                alertDialogBuilder
                        .setMessage("Request not saved! Are you sure you want to quit?")
                        .setCancelable(false)
                        .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                finish();
                            }
                        })
                        .setNegativeButton("No",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else
                finish();
        } else
            super.onBackPressed();
    }

    private void buildVisitor() {
        String owl = getIntent().getStringExtra(getString(R.string.owl_string_key));
        if (owl != null && owl.length() > 0)
            mVisitor = new OWLVisitor(owl);
        else
            finish();
    }

    public void saveOWLRequest(String reqName, OWLItem request) {
        if (request.getFiller().size() > 0 && mVisitor != null) {
            String path = Environment.getExternalStorageDirectory().toString();
            File owl = new File(path + "/owleditor", request.getIRI().getFragment() + ".owl");
            mVisitor.saveOWLRequest(request.getIRI().getFragment(), request, owl);
            Toast.makeText(this, "Request Saved!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, owl.getPath() + " saved!");
            saved = true;
        } else
            Toast.makeText(this, "Empty Request!", Toast.LENGTH_SHORT).show();

    }

}
