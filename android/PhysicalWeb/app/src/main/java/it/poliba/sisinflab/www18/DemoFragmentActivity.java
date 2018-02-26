package it.poliba.sisinflab.www18;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.SettingsFragment;

public class DemoFragmentActivity extends Activity {

    private static final int CONTENT_VIEW_ID = 10101010;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setTitle("Beacon Properties");

        FrameLayout frame = new FrameLayout(this);
        frame.setId(CONTENT_VIEW_ID);
        setContentView(frame, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        String frag = this.getIntent().getStringExtra(getString(R.string.fragment_name_key));
        Fragment newFragment = null;

        if (frag.equalsIgnoreCase(SettingsFragment.class.getName())) {
            newFragment = new SettingsFragment();
        } else if (frag.equalsIgnoreCase(DemoInfoFragment.class.getName())) {
            newFragment = new DemoInfoFragment();
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(CONTENT_VIEW_ID, newFragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // If the action bar up button was pressed
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
