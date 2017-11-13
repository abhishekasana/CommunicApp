package com.sleepingteam.communicappmcproject.chatapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by abhi on 25-10-2017.
 */

public class MyDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "messages.db";
    private static final String TABLE_MESSAGE = "messages";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_MESSAGE = "message";


    public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE "+ TABLE_MESSAGE + " ( " + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + " MESSAGE " + " );";
        db.execSQL(query);
    }

    public void addMessages(String message){
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE,message);
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_MESSAGE,null,values);
        db.close();

    }

    public void removeMessages(String message){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM"+ TABLE_MESSAGE + "WHERE" + COLUMN_MESSAGE + "=\"" + message + "\";");
    }

    public String databaseToString(){
        String dbString = "";
        SQLiteDatabase db = getWritableDatabase();

        // selecting all elements from the table
        String query = "SELECT * FROM" + TABLE_MESSAGE + "WHERE 1";

        Cursor c = db.rawQuery(query,null);
        c.moveToFirst();

        while(!c.isAfterLast()){
            if(c.getString(c.getColumnIndex("message"))!=null){
                dbString += c.getString(c.getColumnIndex("message"));
                dbString += "\n";
            }
            c.moveToNext();
        }
        db.close();
        return dbString;
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS"+TABLE_MESSAGE);
        onCreate(db);

    }
}
