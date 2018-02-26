package it.poliba.sisinflab.www18.data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.physical_web.collection.PwPair;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import it.poliba.sisinflab.psw.PswUtils;
import it.poliba.sisinflab.www18.DemoFragmentActivity;
import it.poliba.sisinflab.www18.DemoInfoFragment;
import it.poliba.sisinflab.www18.DemoWineActivity;
import it.poliba.sisinflab.www18.database.BeaconDatabase;
import it.poliba.sisinflab.www18.database.StoredBeaconData;

public class BeaconStoredAdapter extends RecyclerView.Adapter<BeaconStoredAdapter.BeaconViewHolder>{

    Context mContext;
    ArrayList<TreeMap<String,String>> items;
    BeaconDatabase db;
    DateFormat df;
    String mType;

    public BeaconStoredAdapter(ArrayList<TreeMap<String,String>> list, String type){
        items = list;
        db = DemoWineActivity.getDatabase();
        df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        mType = type;
    }

    @Override
    public BeaconViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_item_demo_beacon_stored_layout, viewGroup, false);
        BeaconViewHolder pvh = new BeaconViewHolder(v);
        mContext = viewGroup.getContext();
        return pvh;
    }

    @Override
    public void onBindViewHolder(BeaconViewHolder personViewHolder, int i) {
        personViewHolder.beaconTitle.setText(items.get(i).get(StoredBeaconData.BeaconEntry.COLUMN_NAME_TITLE));
        personViewHolder.beaconUrl.setText(items.get(i).get(StoredBeaconData.BeaconEntry.COLUMN_NAME_URL));

        Date ts = new Date(Long.parseLong(items.get(i).get(StoredBeaconData.BeaconEntry.COLUMN_NAME_TS)));
        personViewHolder.beaconTs.setText(df.format(ts));

        if(mType.equals(StoredBeaconData.BeaconEntry.SPAM))
            personViewHolder.beaconImg.setImageDrawable(mContext.getDrawable(R.drawable.ic_not_interested_wine_24dp));
        else if(mType.equals(StoredBeaconData.BeaconEntry.HISTORY))
            personViewHolder.beaconImg.setImageDrawable(mContext.getDrawable(R.drawable.ic_history_wine_24dp));

        personViewHolder.cv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

                // set dialog message
                alertDialogBuilder
                        .setTitle("Delete Preference")
                        .setMessage("This action cannot be undone. Continue?")
                        .setCancelable(true)
                        .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if(mType.equals(StoredBeaconData.BeaconEntry.FAVOURITE))
                                    db.removeFavourite(items.get(i).get(StoredBeaconData.BeaconEntry.COLUMN_NAME_URL));
                                else if(mType.equals(StoredBeaconData.BeaconEntry.SPAM))
                                    db.removeSpam(items.get(i).get(StoredBeaconData.BeaconEntry.COLUMN_NAME_URL));
                                else if(mType.equals(StoredBeaconData.BeaconEntry.HISTORY))
                                    db.removeVisited(items.get(i).get(StoredBeaconData.BeaconEntry.COLUMN_NAME_URL));

                                items.remove(i);
                                notifyDataSetChanged();
                            }})
                        .setNegativeButton("No", null);

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        else
            return items.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void update(ArrayList<TreeMap<String,String>> list) {
        clear();
        items = list;
    }

    public void clear() {
        if (items != null)
            items.clear();
    }

    public static class BeaconViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView beaconTitle;
        TextView beaconUrl;
        TextView beaconTs;

        ImageView beaconImg;

        BeaconViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv_stored);
            beaconTitle = (TextView)itemView.findViewById(R.id.beacon_title);
            beaconUrl = (TextView)itemView.findViewById(R.id.beacon_url);
            beaconTs = (TextView)itemView.findViewById(R.id.beacon_ts);

            beaconImg = (ImageView) itemView.findViewById(R.id.beacon_stored_img);
        }
    }

}
