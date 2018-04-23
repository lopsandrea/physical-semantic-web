package it.poliba.sisinflab.www18.data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.physical_web.collection.PwPair;
import org.physical_web.collection.UrlDevice;
import org.physical_web.physicalweb.R;
import org.physical_web.physicalweb.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import it.poliba.sisinflab.owleditor.OWLEditorActivity;
import it.poliba.sisinflab.owleditor.OWLIndividualFragment;
import it.poliba.sisinflab.psw.PswDevice;
import it.poliba.sisinflab.psw.PswUtils;
import it.poliba.sisinflab.www18.DemoFragmentActivity;
import it.poliba.sisinflab.www18.DemoInfoFragment;
import it.poliba.sisinflab.www18.DemoWineActivity;
import it.poliba.sisinflab.www18.database.BeaconDatabase;

public class BeaconAdapter extends RecyclerView.Adapter<BeaconAdapter.BeaconViewHolder>{

    class BeaconItem implements Comparable<BeaconItem> {
        long timestamp;
        PwPair pwPair;
        Context context;

        public BeaconItem(long ts, PwPair pwPair, Context ctx) {
            timestamp = ts;
            this.pwPair = pwPair;
            this.context = ctx;
        }

        @Override
        public int compareTo(@NonNull BeaconItem comparedItem) {
            //descending order
            double comparedRank = PswUtils.getRank(comparedItem.pwPair.getPwsResult(), comparedItem.pwPair.getUrlDevice(), context);
            double currentRank = PswUtils.getRank(this.pwPair.getPwsResult(), this.pwPair.getUrlDevice(), context);

            return Math.round((float)(comparedRank - currentRank)*100);
        }
    }

    private Comparator<BeaconItem> beaconItemComparator = new Comparator<BeaconItem>() {
        public int compare(BeaconItem item1, BeaconItem item2) {
            //descending order
            return item2.compareTo(item1);
        }
    };

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

    private void showExplanation(String individual) {
        String owlAnnotation = DemoWineActivity.getKBManager().getExplaination(individual);
        Intent intent = new Intent(mContext, OWLEditorActivity.class);
        intent.putExtra(mContext.getString(R.string.owl_string_key), owlAnnotation);
        intent.putExtra(mContext.getString(R.string.owl_fragment_key), OWLIndividualFragment.class.getSimpleName());
        mContext.startActivity(intent);
    }

    private void openURL(String url, String title, View view) {
        db.addVisited(url, title);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.getContext().startActivity(browserIntent);
    }

    @Override
    public void onBindViewHolder(BeaconViewHolder personViewHolder, int i) {
        personViewHolder.beaconTitle.setText(items.get(i).pwPair.getPwsResult().getTitle());
        personViewHolder.beaconUrl.setText(items.get(i).pwPair.getPwsResult().getDescription());

        double rank = (1.0 - PswUtils.getRank(items.get(i).pwPair.getPwsResult(), items.get(i).pwPair.getUrlDevice(), mContext))*100;
        if (rank > 90)
            personViewHolder.beaconRankImg.setImageResource(R.drawable.wine_full);
        else if (rank > 75)
            personViewHolder.beaconRankImg.setImageResource(R.drawable.wine_high);
        else if (rank > 50)
            personViewHolder.beaconRankImg.setImageResource(R.drawable.wine_medium);
        else if (rank > 25)
            personViewHolder.beaconRankImg.setImageResource(R.drawable.wine_med_low);
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
                    arrayAdapter.add("Open URL");
                    arrayAdapter.add("Beacon Description");
                    arrayAdapter.add("Rank Explaination");

                    builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            db.addVisited(items.get(i).pwPair.getPwsResult().getRequestUrl(), items.get(i).pwPair.getPwsResult().getTitle());
                            if (which == 0) {
                                try {
                                    openURL(items.get(i).pwPair.getPwsResult().getExtraString(PswDevice.PSW_BEACON_URL_KEY),
                                            items.get(i).pwPair.getPwsResult().getTitle(), view);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else if (which == 1) {
                                showAnnotation(items.get(i).pwPair.getUrlDevice());
                            } else {
                                try {
                                    showExplanation(items.get(i).pwPair.getPwsResult().getExtraString(PswDevice.PSW_IRI_KEY));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    builderSingle.show();
                } else {
                    openURL(items.get(i).pwPair.getPwsResult().getRequestUrl(), items.get(i).pwPair.getPwsResult().getTitle(), view);
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

        if (PswUtils.isPswUidDevice(pw.getUrlDevice())) {
            try {
                prop.add(getProperty("Site URL", pw.getPwsResult().getExtraString(PswDevice.PSW_BEACON_URL_KEY)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else
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

    public void setItems(List<PwPair> pwPairs, Context c) {
        //items = pwPairs;
        for(PwPair pw : pwPairs) {
            if(!isShown(pw.getPwsResult().getRequestUrl()))
                items.add(new BeaconItem(System.currentTimeMillis(), pw, c));
        }

        Collections.sort(items, beaconItemComparator);
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
            //long diff = System.currentTimeMillis() - items.get(i).timestamp;
            if(items.get(i).pwPair.getPwsResult().getRequestUrl().equalsIgnoreCase(pwPair.getPwsResult().getRequestUrl())) {
                items.get(i).timestamp = System.currentTimeMillis();
                items.get(i).pwPair = pwPair;
                return;
            }
        }
    }

    private void removeItem(PwPair pwPair) {
        for (int i = 0; i < items.size(); ++i) {
            //long diff = System.currentTimeMillis() - items.get(i).timestamp;
            if(items.get(i).pwPair.getPwsResult().getRequestUrl().equalsIgnoreCase(pwPair.getPwsResult().getRequestUrl())) {
                items.remove(i);
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

    public void addItem(PwPair pwPair, Context c) {
        if (pwPair != null) {
            if (PswUtils.isPswDevice(pwPair.getUrlDevice()) && !PswUtils.isPswEnabled(c))
                removeItem(pwPair);
            else if(isShown(pwPair.getPwsResult().getRequestUrl()))
                updateItem(pwPair);
            else
                items.add(new BeaconItem(System.currentTimeMillis(), pwPair, c));
        }
    }

    public void sortItems() {
        Collections.sort(items, beaconItemComparator);
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