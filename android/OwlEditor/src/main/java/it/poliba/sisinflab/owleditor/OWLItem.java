package it.poliba.sisinflab.owleditor;

import android.os.Parcel;
import android.os.Parcelable;

import org.semanticweb.owlapi.model.IRI;

import java.util.ArrayList;
import java.util.Arrays;

class OWLItem implements Parcelable {

    public final static int OWL_CLASS = 0;
    public final static int OWL_COMPLEMENT_OF = 1;
    public final static int OWL_MIN_CARDINALITY = 2;
    public final static int OWL_MAX_CARDINALITY = 3;
    public final static int OWL_ALL_VALUES_FROM = 4;
    public final static int OWL_SOME_VALUES_FROM = 5;
    public final static int OWL_INDIVIDUAL = 6;

    private int mType;
    private IRI mIri;
    private int mCardinality;
    private ArrayList<OWLItem> mFiller;

    public OWLItem(int type, IRI iri) {
        mType = type;
        mIri = iri;
        mCardinality = -1;
        mFiller = new ArrayList<>();
    }

    public OWLItem(Parcel in){
        // the order needs to be the same as in writeToParcel() method
        this.mType = in.readInt();
        this.mIri = IRI.create(in.readString());
        this.mCardinality = in.readInt();

        OWLItem[] array = (OWLItem[]) in.readParcelableArray(OWLItem.class.getClassLoader());
        mFiller = new ArrayList<OWLItem>(Arrays.asList(array));
    }

    public void setType(int type) {
        mType = type;
    }

    public void setCardinality(int cardinality) {
        mCardinality = cardinality;
    }

    public int getCardinality() {
        return mCardinality;
    }

    public int getType() {
        return mType;
    }

    public IRI getIRI() {
        return mIri;
    }

    public ArrayList<OWLItem> getFiller() {
        return mFiller;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mType);
        parcel.writeString(mIri.toString());
        parcel.writeInt(mCardinality);
        parcel.writeParcelableArray(mFiller.toArray(new OWLItem[mFiller.size()]), flags);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public OWLItem createFromParcel(Parcel in) {
            return new OWLItem(in);
        }

        public OWLItem[] newArray(int size) {
            return new OWLItem[size];
        }
    };
}
