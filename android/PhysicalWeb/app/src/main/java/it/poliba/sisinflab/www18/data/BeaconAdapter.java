package it.poliba.sisinflab.www18.data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.physical_web.collection.PwPair;
import org.physical_web.collection.UrlDevice;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import it.poliba.sisinflab.owleditor.OWLEditorActivity;
import it.poliba.sisinflab.owleditor.OWLIndividualFragment;
import it.poliba.sisinflab.psw.PswUtils;
import it.poliba.sisinflab.www18.DemoFragmentActivity;
import it.poliba.sisinflab.www18.DemoInfoFragment;
import it.poliba.sisinflab.www18.DemoWineActivity;
import it.poliba.sisinflab.www18.database.BeaconDatabase;

public class BeaconAdapter extends RecyclerView.Adapter<BeaconAdapter.BeaconViewHolder>{

    class BeaconItem {
        long timestamp;
        PwPair pwPair;

        public BeaconItem(long ts, PwPair pwPair) {
            timestamp = ts;
            this.pwPair = pwPair;
        }
    }

    Context mContext;
    List<BeaconItem> items;
    BeaconDatabase db;

    boolean fav;
    boolean spam;

    public BeaconAdapter(){
        items = new ArrayList<>();
        db = DemoWineActivity.getDatabase();
    }

    @Override
    public BeaconViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_item_demo_layout, viewGroup, false);
        BeaconViewHolder pvh = new BeaconViewHolder(v);
        mContext = viewGroup.getContext();
        return pvh;
    }

    private void showAnnotation(UrlDevice d) {
        String owlAnnotation = PswUtils.getOWL(d);
        Intent intent = new Intent(mContext, OWLEditorActivity.class);
        intent.putExtra(mContext.getString(R.string.owl_string_key), owlAnnotation);
        intent.putExtra(mContext.getString(R.string.owl_fragment_key), OWLIndividualFragment.class.getSimpleName());
        mContext.startActivity(intent);
    }

    private void showExplaination() {
        String owlAnnotation = null;
        try {
            owlAnnotation = IOUtils.toString(mContext.getResources().openRawResource(R.raw.wine_fake_exp_www18), StandardCharsets.UTF_8);
            Intent intent = new Intent(mContext, OWLEditorActivity.class);
            intent.putExtra(mContext.getString(R.string.owl_string_key), owlAnnotation);
            intent.putExtra(mContext.getString(R.string.owl_fragment_key), OWLIndividualFragment.class.getSimpleName());
            mContext.startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBindViewHolder(BeaconViewHolder personViewHolder, int i) {
        personViewHolder.beaconTitle.setText(items.get(i).pwPair.getPwsResult().getTitle());
        personViewHolder.beaconUrl.setText(items.get(i).pwPair.getPwsResult().getDescription());

        double rank = (1.0 - PswUtils.getRank(items.get(i).pwPair.getPwsResult(), items.get(i).pwPair.getUrlDevice(), mContext))*100;
        if (rank > 90)
            personViewHolder.beaconRankImg.setImageResource(R.drawable.wine_full);
        else if (rank > 50)
            personViewHolder.beaconRankImg.setImageResource(R.drawable.wine_medium);
        else
            personViewHolder.beaconRankImg.setImageResource(R.drawable.wine_empty);

        personViewHolder.rank.setText((int)rank + "%");
        personViewHolder.extra_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (PswUtils.isPswDevice(items.get(i).pwPair.getUrlDevice())) {
                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
                    builderSingle.setTitle("Show OWL annotation");

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item);
                    arrayAdapter.add("Beacon Description");
                    arrayAdapter.add("Rank Explaination");

                    builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            db.addVisited(items.get(i).pwPair.getPwsResult().getRequestUrl(), items.get(i).pwPair.getPwsResult().getTitle());
                            if (which == 0) {
                                showAnnotation(items.get(i).pwPair.getUrlDevice());
                            } else {
                                showExplaination();
                            }
                        }
                    });
                    builderSingle.show();
                } else {
                    db.addVisited(items.get(i).pwPair.getPwsResult().getRequestUrl(), items.get(i).pwPair.getPwsResult().getTitle());
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(items.get(i).pwPair.getPwsResult().getRequestUrl()));
                    view.getContext().startActivity(browserIntent);
                }
            }
        });

        personViewHolder.extra_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<TreeMap<String,String>> prop = getBeaconProperties(items.get(i).pwPair);
                Intent intent = new Intent(mContext, DemoFragmentActivity.class);
                Bundle b = new Bundle();
                b.putString(mContext.getString(R.string.fragment_name_key), DemoInfoFragment.class.getName());
                b.putSerializable(mContext.getString(R.string.beacon_properties_key), prop);
                intent.putExtras(b);
                mContext.startActivity(intent);
            }
        });

        fav = db.isFavourite(items.get(i).pwPair.getPwsResult().getRequestUrl());
        if (!fav)
            setIconColor(personViewHolder.extra_fav, android.R.color.darker_gray);

        personViewHolder.extra_fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fav) {
                    db.removeFavourite(items.get(i).pwPair.getPwsResult().getRequestUrl());
                    setIconColor(personViewHolder.extra_fav, android.R.color.darker_gray);
                    fav = false;
                } else {
                    db.addFavourite(items.get(i).pwPair.getPwsResult().getRequestUrl(), items.get(i).pwPair.getPwsResult().getTitle());
                    setIconColor(personViewHolder.extra_fav, R.color.www18_wine);
                    fav = true;
                }
            }
        });

        spam = db.isSpam(items.get(i).pwPair.getPwsResult().getRequestUrl());
        if (!spam)
            setIconColor(personViewHolder.extra_spam, android.R.color.darker_gray);

        personViewHolder.extra_spam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(spam) {
                    db.removeSpam(items.get(i).pwPair.getPwsResult().getRequestUrl());
                    setIconColor(personViewHolder.extra_spam, android.R.color.darker_gray);
                    spam = false;
                } else {
                    db.addSpam(items.get(i).pwPair.getPwsResult().getRequestUrl(), items.get(i).pwPair.getPwsResult().getTitle());
                    setIconColor(personViewHolder.extra_spam, R.color.www18_wine);
                    spam = true;
                }
            }
        });

    }

    private void setIconColor(ImageView view, int color) {
        view.setColorFilter(mContext.getResources().getColor(color));
    }

    private ArrayList<TreeMap<String,String>> getBeaconProperties(PwPair pw) {
        ArrayList<TreeMap<String,String>> prop = new ArrayList<TreeMap<String,String>>();

        prop.add(getProperty("Title", pw.getPwsResult().getTitle()));
        prop.add(getProperty("Description", pw.getPwsResult().getDescription()));
        prop.add(getProperty("Group ID", pw.getPwsResult().getGroupId()));
        prop.add(getProperty("Icon URL", pw.getPwsResult().getIconUrl()));
        prop.add(getProperty("Request URL", pw.getPwsResult().getRequestUrl()));
        prop.add(getProperty("Site URL", pw.getPwsResult().getSiteUrl()));

        prop.add(getProperty("Rank (Penalty Score)", String.valueOf(PswUtils.getRank(pw.getPwsResult(), pw.getUrlDevice(), mContext))));
        prop.add(getProperty("TxPower", String.valueOf(Utils.getTxPower(pw.getUrlDevice()))));
        prop.add(getProperty("RSSI", String.valueOf(Utils.getSmoothedRssi(pw.getUrlDevice()))));
        prop.add(getProperty("Distance", String.valueOf(Utils.getDistance(pw.getUrlDevice()))));
        prop.add(getProperty("Region", Utils.getRegionString(pw.getUrlDevice())));

        return prop;
    }

    private TreeMap<String,String> getProperty(String name, String value) {
        TreeMap<String,String> map = new TreeMap<String,String>();
        map.put("line1", name);
        map.put("line2", value);
        return map;
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

    public void clear() {
        if (items != null)
            items.clear();
    }

    public void setItems(List<PwPair> pwPairs) {
        //items = pwPairs;
        for(PwPair pw : pwPairs) {
            if(!isShown(pw.getPwsResult().getRequestUrl()))
                items.add(new BeaconItem(System.currentTimeMillis(), pw));
        }

    }

    private boolean isShown(String url) {
        for(BeaconItem item : items) {
            if (item.pwPair.getPwsResult().getRequestUrl().equalsIgnoreCase(url))
                return true;
        }
        return false;
    }

    private void updateItem(PwPair pwPair) {
        for (int i = 0; i < items.size(); ++i) {
            long diff = System.currentTimeMillis() - items.get(i).timestamp;
            if(items.get(i).pwPair.getPwsResult().getRequestUrl().equalsIgnoreCase(pwPair.getPwsResult().getRequestUrl())
                    && diff > 60000) {
                items.get(i).timestamp = System.currentTimeMillis();
                items.get(i).pwPair = pwPair;
                return;
            }
        }
    }

    /*public boolean containsGroupId(String groupId) {
        for (PwPair pwPair : items) {
            if (isFolderItem(pwPair)) {
                continue;
            }
            if (Utils.getGroupId(pwPair.getPwsResult()).equals(groupId)) {
                return true;
            }
        }
        return false;
    }*/

    public void addItem(PwPair pwPair) {
        if (pwPair != null) {
            if(isShown(pwPair.getPwsResult().getRequestUrl()))
                updateItem(pwPair);
            else
                items.add(new BeaconItem(System.currentTimeMillis(), pwPair));
        }
    }

    /*boolean isFolderItem(PwPair item) {
        return item.getUrlDevice() == null && item.getPwsResult().getSiteUrl() == null;
    }*/

    public static class BeaconViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView beaconTitle;
        TextView beaconUrl;
        ImageView beaconRankImg;

        LinearLayout extra;
        TextView rank;
        ImageView extra_open;
        ImageView extra_info;
        ImageView extra_fav;
        ImageView extra_spam;

        BeaconViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv);
            beaconTitle = (TextView)itemView.findViewById(R.id.beacon_title);
            beaconUrl = (TextView)itemView.findViewById(R.id.beacon_url);
            beaconRankImg = (ImageView)itemView.findViewById(R.id.beacon_rank_img);
            rank = (TextView) itemView.findViewById(R.id.rank_text);

            extra = (LinearLayout) itemView.findViewById(R.id.card_extra);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (extra.getVisibility() == View.VISIBLE)
                        extra.setVisibility(View.GONE);
                    else
                        extra.setVisibility(View.VISIBLE);
                }
            });

            extra_open = (ImageView) itemView.findViewById(R.id.extra_open);
            extra_info = (ImageView) itemView.findViewById(R.id.extra_info);
            extra_fav = (ImageView) itemView.findViewById(R.id.extra_fav);
            extra_spam = (ImageView) itemView.findViewById(R.id.extra_spam);
        }
    }

}
