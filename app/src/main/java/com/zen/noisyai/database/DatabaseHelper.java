package com.zen.noisyai.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.zen.noisyai.database.model.Record;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 11;

    // Database Name
    private static final String DATABASE_NAME = "records_db";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        // create notes table
        db.execSQL(Record.CREATE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + Record.TABLE_NAME);

        // Create tables again
        onCreate(db);
    }


    public long insertRecord(Record rec) {
        // get writable database as we want to write data
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        // `id` and `timestamp` will be inserted automatically.
        // no need to add them
        values.put(Record.COLUMN_NAME, rec.getName());
        values.put(Record.COLUMN_FILE_PATH, rec.getFilePath());
        values.put(Record.COLUMN_ROOT_FILE_PATH, rec.getRootFilePath());
        values.put(Record.COLUMN_TYPE, rec.getType());
        values.put(Record.COLUMN_PERCENTAGE, rec.getPercentage());
        values.put(Record.COLUMN_DURATION, rec.getDuration());
        values.put(Record.COLUMN_TIMESTAMP, rec.getTimestamp());

        // insert row
        long id = db.insert(Record.TABLE_NAME, null, values);

        // close db connection
        db.close();

        // return newly inserted row id
        return id;
    }

    public int updateRecord(Record rec) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Record.COLUMN_NAME, rec.getName());

        // updating row
        return db.update(Record.TABLE_NAME, values, Record.COLUMN_ID + " = ?",
                new String[]{String.valueOf(rec.getId())});
    }

    public void deleteRecord(Record rec) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(Record.TABLE_NAME, Record.COLUMN_ID + " = ?",
                new String[]{String.valueOf(rec.getId())});
        db.close();
    }


    public Record getRecord(long id) {
        // get readable database as we are not inserting anything
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(Record.TABLE_NAME,
                new String[]{Record.COLUMN_ID, Record.COLUMN_NAME, Record.COLUMN_FILE_PATH, Record.COLUMN_PERCENTAGE, Record.COLUMN_DURATION, Record.COLUMN_TIMESTAMP},
                Record.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        Record rec = new Record();

        if (cursor != null){
            cursor.moveToFirst();
            // prepare note object
            rec = new Record(
                cursor.getInt(cursor.getColumnIndex(Record.COLUMN_ID)),
                cursor.getString(cursor.getColumnIndex(Record.COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndex(Record.COLUMN_FILE_PATH)),
                cursor.getString(cursor.getColumnIndex(Record.COLUMN_ROOT_FILE_PATH)),
                cursor.getInt(cursor.getColumnIndex(Record.COLUMN_TYPE)),
                cursor.getDouble(cursor.getColumnIndex(Record.COLUMN_PERCENTAGE)),
                cursor.getString(cursor.getColumnIndex(Record.COLUMN_DURATION)),
                cursor.getString(cursor.getColumnIndex(Record.COLUMN_TIMESTAMP)));
        }

        // close the db connection
        cursor.close();

        return rec;
    }

    public List<Record> getAllRecords() {
        List<Record> recs = new ArrayList<>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + Record.TABLE_NAME + " ORDER BY " +
                Record.COLUMN_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Record rec = new Record();
                rec.setId(cursor.getInt(cursor.getColumnIndex(Record.COLUMN_ID)));
                rec.setName(cursor.getString(cursor.getColumnIndex(Record.COLUMN_NAME)));
                rec.setFilePath(cursor.getString(cursor.getColumnIndex(Record.COLUMN_FILE_PATH)));
                rec.setRootFilePath(cursor.getString(cursor.getColumnIndex(Record.COLUMN_ROOT_FILE_PATH)));
                rec.setType(cursor.getInt(cursor.getColumnIndex(Record.COLUMN_TYPE)));
                rec.setPercentage(cursor.getDouble(cursor.getColumnIndex(Record.COLUMN_PERCENTAGE)));
                rec.setDuration(cursor.getString(cursor.getColumnIndex(Record.COLUMN_DURATION)));
                rec.setTimestamp(cursor.getString(cursor.getColumnIndex(Record.COLUMN_TIMESTAMP)));

                recs.add(rec);
            } while (cursor.moveToNext());
        }

        // close db connection
        db.close();

        // return notes list
        return recs;
    }

    public int getRecordsCount() {
        String countQuery = "SELECT  * FROM " + Record.TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();


        // return count
        return count;
    }
}