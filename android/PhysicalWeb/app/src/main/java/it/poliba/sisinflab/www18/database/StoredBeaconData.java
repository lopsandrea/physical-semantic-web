package it.poliba.sisinflab.www18.database;

import android.provider.BaseColumns;

public final class StoredBeaconData {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private StoredBeaconData() {}

    /* Inner class that defines the table contents */
    public static class BeaconEntry implements BaseColumns {
        public static final String BEACONS_TABLE = "beacons";

        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_TS = "timestamp";
        public static final String COLUMN_NAME_TYPE = "type";

        public static final String FAVOURITE = "F";
        public static final String SPAM = "S";
        public static final String HISTORY = "H";
    }
}
