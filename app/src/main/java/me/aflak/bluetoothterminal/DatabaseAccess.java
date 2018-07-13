package me.aflak.bluetoothterminal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pallavinishanth on 7/11/18.
 */

public class DatabaseAccess {

    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase database;
    private static DatabaseAccess instance;

    /**
     * Private constructor to avoid object creation from outside classes.
     *
     * @param context
     */
    private DatabaseAccess(Context context) {
        this.openHelper = new DatabaseOpenHelper(context);
    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param context the Context
     * @return the instance of DabaseAccess
     */
    public static DatabaseAccess getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    /**
     * Open the database connection.
     */
    public void open() {
        this.database = openHelper.getReadableDatabase();
    }

    /**
     * Close the database connection.
     */
    public void close() {
        if (database != null) {
            this.database.close();
        }
    }

    /**
     * Read all punch data from the database.
     *
     * @return a List of quotes
     */
    public List<Integer> read_632_Horizontal(String table_name) {

        List<Integer> list = new ArrayList<>();

        Cursor cursor = database.rawQuery("SELECT * FROM " +"'"+table_name+"'" ,null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            for(int i=0;i<cursor.getColumnCount();i++){
                list.add(cursor.getInt(i));
            }

            cursor.moveToNext();
        }
        cursor.close();

        return list;

    }

}
