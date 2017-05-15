package it.poliba.sisinflab.owleditor;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
    OWLItem mRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int style = R.style.AppTheme;
        Fragment f = null;
        String fragment = getIntent().getStringExtra(getString(R.string.owl_fragment_key));
        if (fragment != null) {
            buildVisitor();

            if (fragment.equals(OWLIndividualFragment.class.getSimpleName())) {
                f = new OWLIndividualFragment();
            } else if (fragment.equals(OWLBuilderFragment.class.getSimpleName())) {
                mRequest = new OWLItem(OWLItem.OWL_INDIVIDUAL, IRI.create(getString(R.string.owl_request_default_name)));
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
        super.onBackPressed();
        if (getFragmentManager().getBackStackEntryCount() == 0)
            finish();
    }

    private void buildVisitor() {
        String owl = getIntent().getStringExtra(getString(R.string.owl_string_key));
        if (owl != null && owl.length() > 0)
            mVisitor = new OWLVisitor(owl);
        else
            finish();
    }

    public void saveOWLRequest(String reqName) {
        if (mRequest.getFiller().size() > 0 && mVisitor != null) {
            String path = Environment.getExternalStorageDirectory().toString();
            File owl = new File(path + "/owleditor", mRequest.getIRI().getFragment() + ".owl");
            mVisitor.saveOWLRequest(mRequest.getIRI().getFragment(), mRequest, owl);
            Toast.makeText(this, "Request Saved!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, owl.getPath() + " saved!");
        } else
            Toast.makeText(this, "Empty Request!", Toast.LENGTH_SHORT).show();

    }

}
