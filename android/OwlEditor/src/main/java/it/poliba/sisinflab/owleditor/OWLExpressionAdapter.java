package it.poliba.sisinflab.owleditor;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

class OWLExpressionAdapter extends BaseAdapter {
    List<OWLItem> demos;
    Activity mActivity;

    public OWLExpressionAdapter(Activity activity) {
        mActivity = activity;
        demos = new ArrayList<>();
    }

    public void addItem(OWLItem item) {
        demos.add(item);
    }

    @Override
    public int getCount() {
        return demos.size();
    }

    @Override
    public OWLItem getItem(int i) {
        return demos.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mActivity.getLayoutInflater().inflate(R.layout.owl_list_item, viewGroup, false);
        }

        String label = demos.get(i).getIRI().getFragment();
        int icon = R.drawable.class_image;
        switch (demos.get(i).getType()){
            case OWLItem.OWL_CLASS:
                icon = R.drawable.class_image;
                break;
            case OWLItem.OWL_COMPLEMENT_OF:
                icon = R.drawable.not_image;
                break;
            case OWLItem.OWL_ALL_VALUES_FROM:
                icon = R.drawable.only_image;
                break;
            case OWLItem.OWL_MAX_CARDINALITY:
                if (demos.get(i).getCardinality() != -1)
                    label = "max " + demos.get(i).getCardinality() + " " + label;
                icon = R.drawable.ltr_image;
                break;
            case OWLItem.OWL_MIN_CARDINALITY:
                if (demos.get(i).getCardinality() != -1)
                    label = "min " + demos.get(i).getCardinality() + " " + label;
                icon = R.drawable.gtr_image;
                break;
            case OWLItem.OWL_INDIVIDUAL:
                icon = R.drawable.individual_image;
                break;
        }

        ((TextView) view.findViewById(R.id.owl_label)).setText(label);
        ((TextView) view.findViewById(R.id.owl_iri)).setText(demos.get(i).getIRI().toString());
        ((ImageView) view.findViewById(R.id.owl_icon)).setImageResource(icon);
        return view;
    }

    public void clear() {
        demos.clear();
    }

    public void removeItem(int index) {
        demos.remove(index);
    }
}
