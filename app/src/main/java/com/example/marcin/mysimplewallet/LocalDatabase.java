package com.example.marcin.mysimplewallet;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;

public class LocalDatabase extends AppCompatActivity
{
    private String TABLE_NAME;
    private SQLiteDatabase db;
    private String timestamp;

    public LocalDatabase(SQLiteDatabase db, String TABLE_NAME)
    {
        this.db = db;
        this.TABLE_NAME = TABLE_NAME;
        this.timestamp = "0";
        createDatabaseIfNotExists();
    }

    private void createDatabaseIfNotExists()
    {
        String sqlDB = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                "Id VARCHAR PRIMARY KEY NOT NULL," +
                "Title VARCHAR," +
                "Value FLOAT NOT NULL, " +
                "Date DATE," +
                "Timestamp BIGINT NOT NULL)";
        db.execSQL(sqlDB);
    }

    public void addRegistry(Registry registry)
    {
        String sqlQuery = "INSERT INTO " + TABLE_NAME + "(Id, Title, Value, Date, Timestamp) VALUES (" +
                "\"" + registry.id + "\"," +
                "\"" + registry.title + "\"," +
                "\"" + registry.value + "\"," +
                "\"" + registry.date + "\"," +
                "\"" + registry.timestamp + "\")";


        timestamp = registry.timestamp;
        Log.d("koy", registry.id + " id");

        db.execSQL(sqlQuery);
    }

    public void removeRegistry(Registry registry)
    {
        String sqlQuery = "DELETE FROM " + TABLE_NAME + " WHERE Id = " + "\"" + registry.id + "\"";
        timestamp = Long.toString(new Date().getTime());
        db.execSQL(sqlQuery);
    }

    public void editRegistry(Registry registry)
    {
        String sqlQuery = "UPDATE " + TABLE_NAME  +
                " SET " +
                "Id = " + "\"" + registry.id + "\"," +
                "Title = " + "\"" + registry.title + "\"," +
                "Value = " + "\"" + registry.value + "\"," +
                "Date = " + "\"" + registry.date + "\"," +
                "Timestamp = " + "\"" + registry.timestamp + "\"" +
                "WHERE Id = " + "\"" + registry.id + "\"";
        timestamp = registry.timestamp;
        db.execSQL(sqlQuery);
    }

    public ArrayList<Registry> getAllRegistries()
    {
        createDatabaseIfNotExists();

        // Loads data from database.
        ArrayList<Registry> registries = new ArrayList<Registry>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst())
        {
            do
            {
                String id = cursor.getString(cursor.getColumnIndex("Id"));
                String title = cursor.getString(cursor.getColumnIndex("Title"));
                float value = cursor.getFloat(cursor.getColumnIndex("Value"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                String timestamp = cursor.getString(cursor.getColumnIndex("Timestamp"));
                registries.add(new Registry(id, title, value, date, timestamp));

            } while (cursor.moveToNext());
        }

        return registries;
    }

    public void removeAll()
    {
        String sqlQuery = "DELETE FROM " + TABLE_NAME;
        timestamp = Long.toString(new Date().getTime());
        db.execSQL(sqlQuery);
    }


    public String getTimestamp()
    {
        /*ArrayList<Registry> registries = getAllRegistries();
        Long timestamp = 0L;
        for (Registry registry : registries)
        {
            Long temp = Long.parseLong(registry.timestamp);
            if (temp > timestamp)
                timestamp = temp;
        }
        return Long.toString(timestamp);*/
        return timestamp;
    }

    public Registry getRegistryById(String ID)
    {
        String sqlQuery = "SELECT * FROM " + TABLE_NAME +
                " WHERE Id = " + "\"" + ID + "\"";

        Cursor cursor = db.rawQuery(sqlQuery, null);

        Registry registry = null;
        if (cursor.moveToFirst())
        {
            do
            {
                String id = cursor.getString(cursor.getColumnIndex("Id"));
                String title = cursor.getString(cursor.getColumnIndex("Title"));
                float value = cursor.getFloat(cursor.getColumnIndex("Value"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                String timestamp = cursor.getString(cursor.getColumnIndex("Timestamp"));
                registry = new Registry(id, title, value, date, timestamp);

            } while (cursor.moveToNext());
        }
        return registry;
    }

}
