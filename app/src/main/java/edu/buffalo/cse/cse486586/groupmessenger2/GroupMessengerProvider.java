package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.content.ContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    public static Uri CONTENT_URI;
    public static MyDB mydb;
    public static SQLiteDatabase db;
    public static long ID;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        db = mydb.getWritableDatabase();
        ID = db.insert(mydb.TABLE_NAME, null, values);
        if (ID <= 0) {
            Log.v("Inserting Failed", values.toString());
            return null;
        }
        Uri u = Uri.withAppendedPath(CONTENT_URI, "/"+Long.toString(ID));
        //Notify registered observers that a row was updated and attempt to sync changes to the network.
        getContext().getContentResolver().notifyChange(u, null);

        Log.v("Inserted key value ::", values.toString());
        return u;
    }

    @Override
    public boolean onCreate() {
        CONTENT_URI = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider" + "/" + mydb.TABLE_NAME);
        mydb = new MyDB(getContext());
        db = mydb.getWritableDatabase();
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        db = mydb.getReadableDatabase();
        Cursor cur = db.rawQuery("select * from " + MyDB.TABLE_NAME + " where key ='" + selection + "' LIMIT 1", null);
        Log.v("query", selection);
        return cur;
    }


    private class MyDB extends SQLiteOpenHelper {

        public static final String DATABASE_NAME = "USERS";
        public static final int DATABASE_VERSION = 2;
        public static final String TABLE_NAME = "UserKeyValue";
        public static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME +
                        " ( key TEXT PRIMARY KEY , value TEXT REPLACE);";

        MyDB(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }


        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
            onCreate(db);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE);}
    }
}