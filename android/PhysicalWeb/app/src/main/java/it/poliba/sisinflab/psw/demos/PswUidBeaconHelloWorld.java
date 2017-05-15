package it.poliba.sisinflab.psw.demos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import org.physical_web.demos.Demo;
import org.physical_web.physicalweb.R;
import org.semanticweb.owlapi.model.IRI;

import java.util.UUID;

import it.poliba.sisinflab.psw.PswUidBroadcastService;
import it.poliba.sisinflab.psw.owl.KBManager;

public class PswUidBeaconHelloWorld implements Demo {
    private static final String TAG = PswUidBeaconHelloWorld.class.getSimpleName();
    private static boolean mIsDemoStarted = false;
    private Context mContext;

    public PswUidBeaconHelloWorld(Context context) {
        mContext = context;
    }

    @Override
    public String getSummary() {
        return mContext.getString(R.string.psw_uid_demo_summary);
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.psw_uid_demo_title);
    }

    @Override
    public boolean isDemoStarted() {
        return mIsDemoStarted;
    }

    @Override
    public void startDemo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.psw_resource_demo_title)
            .setItems(R.array.owl_resource_array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int pos) {

                    int resource = -1;

                    switch (pos) {
                        case 0:
                            resource = R.raw.augustinerkirche;
                            break;
                        case 1:
                            resource = R.raw.karlskirche;
                            break;
                        case 2:
                            resource = R.raw.parlamento;
                            break;
                    }

                    IRI iri = KBManager.loadIndividual(mContext.getResources().openRawResource(resource));

                    Intent intent = new Intent(mContext, PswUidBroadcastService.class);
                    intent.putExtra(PswUidBroadcastService.TITLE_KEY, iri.getFragment());
                    intent.putExtra(PswUidBroadcastService.RESOURCE_KEY, resource);

                    String mac = android.provider.Settings.Secure.getString(mContext.getContentResolver(), "bluetooth_address").replace(":","");
                    intent.putExtra(PswUidBroadcastService.MAC_KEY, mac);

                    String ontology = iri.getNamespace();
                    String onto_id = UUID.nameUUIDFromBytes(ontology.getBytes()).toString().substring(0, 4);
                    intent.putExtra(PswUidBroadcastService.ONTO_KEY, onto_id);

                    String instance = iri.getFragment();
                    String instance_id = UUID.nameUUIDFromBytes(instance.getBytes()).toString().substring(0, 6);
                    intent.putExtra(PswUidBroadcastService.INSTANCE_KEY, instance_id);

                    mContext.startService(intent);
                }
            });
        builder.create().show();

        mIsDemoStarted = true;
    }

    @Override
    public void stopDemo() {
        mContext.stopService(new Intent(mContext, PswUidBroadcastService.class));
        mIsDemoStarted = false;
    }
}
