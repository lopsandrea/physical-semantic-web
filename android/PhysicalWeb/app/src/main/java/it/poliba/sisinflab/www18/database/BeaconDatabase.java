package it.poliba.sisinflab.www18.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class BeaconDatabase extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Beacons.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + StoredBeaconData.BeaconEntry.BEACONS_TABLE + " (" +
                    StoredBeaconData.BeaconEntry._ID + " INTEGER PRIMARY KEY," +
                    StoredBeaconData.BeaconEntry.COLUMN_NAME_TITLE + " TEXT," +
                    StoredBeaconData.BeaconEntry.COLUMN_NAME_TS + " TEXT," +
                    StoredBeaconData.BeaconEntry.COLUMN_NAME_TYPE + " TEXT," +
                    StoredBeaconData.BeaconEntry.COLUMN_NAME_URL + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + StoredBeaconData.BeaconEntry.BEACONS_TABLE;

    public BeaconDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public boolean isFavourite(String url) {
        return isPresent(url, StoredBeaconData.BeaconEntry.FAVOURITE);
    }

    public boolean isSpam(String url) {
        return isPresent(url, StoredBeaconData.BeaconEntry.SPAM);
    }

    public boolean isVisited(String url) {
        return isPresent(url, StoredBeaconData.BeaconEntry.HISTORY);
    }

    private boolean isPresent(String url, String type) {
        SQLiteDatabase db = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                StoredBeaconData.BeaconEntry._ID,
                StoredBeaconData.BeaconEntry.COLUMN_NAME_TITLE,
        };

        // Filter results
        String selection = StoredBeaconData.BeaconEntry.COLUMN_NAME_URL + " = ? AND " +
                StoredBeaconData.BeaconEntry.COLUMN_NAME_TYPE + " = ? ";
        String[] selectionArgs = {url, type};

        Cursor cursor = db.query(
                StoredBeaconData.BeaconEntry.BEACONS_TABLE,         // The table to query
                projection,                                         // The columns to return
                selection,                                          // The columns for the WHERE clause
                selectionArgs,                                      // The values for the WHERE clause
                null,                                      // don't group the rows
                null,                                       // don't filter by row groups
                null                                       // The sort order
        );

        boolean result;
        if (cursor.getCount() > 0)
            result = true;
        else
            result = false;

        cursor.close();
        return result;
    }

    public void addFavourite(String url, String title) {
        addRow(url, title, StoredBeaconData.BeaconEntry.FAVOURITE);
    }

    public void addSpam(String url, String title) {
        addRow(url, title, StoredBeaconData.BeaconEntry.SPAM);
    }

    public void addVisited(String url, String title) {
        if (!isVisited(url))
            addRow(url, title, StoredBeaconData.BeaconEntry.HISTORY);
        else
            updateTimestamp(url, StoredBeaconData.BeaconEntry.HISTORY);
    }

    private void updateTimestamp(String url, String type) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(StoredBeaconData.BeaconEntry.COLUMN_NAME_TS, System.currentTimeMillis());

        db.update(StoredBeaconData.BeaconEntry.BEACONS_TABLE, cv,
                StoredBeaconData.BeaconEntry.COLUMN_NAME_URL + " = ? AND " + StoredBeaconData.BeaconEntry.COLUMN_NAME_TYPE + " = ?",
                new String[]{url, type});
    }

    private void addRow(String url, String title, String type) {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(StoredBeaconData.BeaconEntry.COLUMN_NAME_TITLE, title);
        values.put(StoredBeaconData.BeaconEntry.COLUMN_NAME_URL, url);
        values.put(StoredBeaconData.BeaconEntry.COLUMN_NAME_TYPE, type);
        values.put(StoredBeaconData.BeaconEntry.COLUMN_NAME_TS, System.currentTimeMillis());

        // Insert the new row, returning the primary key value of the new row
        db.insert(StoredBeaconData.BeaconEntry.BEACONS_TABLE, null, values);
    }

    public void removeFavourite(String url) {
        removeRow(url, StoredBeaconData.BeaconEntry.FAVOURITE);
    }

    public void removeSpam(String url) {
        removeRow(url, StoredBeaconData.BeaconEntry.SPAM);
    }

    public void removeVisited(String url) {
        removeRow(url, StoredBeaconData.BeaconEntry.HISTORY);
    }

    private void removeRow(String url, String type) {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Define 'where' part of query.
        String selection = StoredBeaconData.BeaconEntry.COLUMN_NAME_URL + " = ? AND " +
                StoredBeaconData.BeaconEntry.COLUMN_NAME_TYPE + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {url, type};
        // Issue SQL statement.
        db.delete(StoredBeaconData.BeaconEntry.BEACONS_TABLE, selection, selectionArgs);
    }

    public ArrayList<TreeMap<String,String>> getAllFavourite() {
        return getAllRows(StoredBeaconData.BeaconEntry.FAVOURITE);
    }

    public ArrayList<TreeMap<String,String>> getAllSpam() {
        return getAllRows(StoredBeaconData.BeaconEntry.SPAM);
    }

    public ArrayList<TreeMap<String,String>> getAllVisited() {
        return getAllRows(StoredBeaconData.BeaconEntry.HISTORY);
    }

    private ArrayList<TreeMap<String,String>> getAllRows(String type) {

        ArrayList<TreeMap<String,String>> data = new ArrayList<TreeMap<String,String>>();
        SQLiteDatabase db = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                StoredBeaconData.BeaconEntry.COLUMN_NAME_TITLE,
                StoredBeaconData.BeaconEntry.COLUMN_NAME_URL,
                StoredBeaconData.BeaconEntry.COLUMN_NAME_TS,
        };

        // Filter results
        String selection = StoredBeaconData.BeaconEntry.COLUMN_NAME_TYPE + " = ? ";
        String[] selectionArgs = {type};

        String sortOrder =
                StoredBeaconData.BeaconEntry.COLUMN_NAME_TS + " DESC";

        Cursor cursor = db.query(
                StoredBeaconData.BeaconEntry.BEACONS_TABLE,         // The table to query
                projection,                                         // The columns to return
                selection,                                          // The columns for the WHERE clause
                selectionArgs,                                      // The values for the WHERE clause
                null,                                      // don't group the rows
                null,                                       // don't filter by row groups
                sortOrder                                       // The sort order
        );

        while(cursor.moveToNext()) {
            TreeMap<String,String> map = new TreeMap<String,String>();
            map.put(StoredBeaconData.BeaconEntry.COLUMN_NAME_TITLE, cursor.getString(0));
            map.put(StoredBeaconData.BeaconEntry.COLUMN_NAME_URL, cursor.getString(1));
            map.put(StoredBeaconData.BeaconEntry.COLUMN_NAME_TS, cursor.getString(2));
            data.add(map);
        }
        cursor.close();
        return data;
    }
}
