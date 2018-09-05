package com.example.marcin.mysimplewallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity
{
    private SQLiteDatabase db;
    private class Registration
    {
        int id;
        String title;
        String value;
        String date;
        // 0 - outgo
        // 1 - income
        int incomeOrOutgo;

        public Registration(int id, String title, String value, String date, int incomeOrOutgo)
        {
            this.id = id;
            this.title = title;
            this.value = value;
            this.date = date;
            this.incomeOrOutgo = incomeOrOutgo;
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creates database if not exists.
        db = openOrCreateDatabase("Wallet", MODE_PRIVATE, null);
        String sqlDB = "CREATE TABLE IF NOT EXISTS IncomeOutgo (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "Title VARCHAR," +
                "Value DOUBLE NOT NULL, " +
                "Date DATE," +
                "IncomeOrOutgo INTEGER NOT NULL)";
        db.execSQL(sqlDB);


        // Loads data from database.
        ArrayList<Registration> registrations = new ArrayList<Registration>();
        Cursor cursor = db.rawQuery("SELECT * FROM IncomeOutgo", null);

        if (cursor.moveToFirst())
        {
            do
            {
                int id = cursor.getInt(cursor.getColumnIndex("Id"));
                String title = cursor.getString(cursor.getColumnIndex("Title"));
                String value = cursor.getString(cursor.getColumnIndex("Value"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                int incomeOrOutgo = cursor.getInt(cursor.getColumnIndex("IncomeOrOutgo"));


                /*String OLD_FORMAT = "E MMM dd HH:mm:ss z yyyy";
                String NEW_FORMAT = "dd/MM/yyyy";
                SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
                Date d = null;
                try
                {
                    d = sdf.parse(date);
                }
                catch (Exception e)
                {

                }
                sdf.applyPattern(NEW_FORMAT);
                String newDateString = sdf.format(d);*/


                registrations.add(new Registration(id, title, value, date, incomeOrOutgo));

            } while (cursor.moveToNext());
        }

        for ( Registration x : registrations)
            addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);

        // Loads account balance, income and outgo.
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String stan = sharedPreferences.getString("stan","0");
        String wydatki = sharedPreferences.getString("wydatki","0");
        String przychod = sharedPreferences.getString("przychod","0");

        TextView textViewStan = (TextView) findViewById(R.id.textViewStanIlosc);
        textViewStan.setText(stan);
        TextView textViewWydatki = (TextView) findViewById(R.id.textViewWydatkiIlosc);
        textViewWydatki.setText(wydatki);
        TextView textViewPrzychod = (TextView) findViewById(R.id.textViewPrzychodIlosc);
        textViewPrzychod.setText(przychod);
    }

    @Override
    protected void onPause()
    {
        // Saves account balance, income and outgo.
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();

        TextView textViewStan = (TextView) findViewById(R.id.textViewStanIlosc);
        prefsEditor.putString("stan", textViewStan.getText().toString());
        TextView textViewWydatki = (TextView) findViewById(R.id.textViewWydatkiIlosc);
        prefsEditor.putString("wydatki", textViewWydatki.getText().toString());
        TextView textViewPrzychod = (TextView) findViewById(R.id.textViewPrzychodIlosc);
        prefsEditor.putString("przychod", textViewPrzychod.getText().toString());

        prefsEditor.commit();

        super.onPause();
    }

    static final int REQUEST_CODE_WYDATEK = 0;
    static final int REQUEST_CODE_PRZYCHOD = 1;

    public void onClickWydatek(View view)
    {
        Intent i = new Intent(this, dodaj.class);
        // 0 - outgo
        // 1 - income
        i.putExtra("IncomeOrOutgo", 0);
        startActivityForResult(i, REQUEST_CODE_WYDATEK);

    }

    public void onClickPrzychod(View view)
    {
        Intent i = new Intent(this, dodaj.class);
        // 0 - outgo
        // 1 - income
        i.putExtra("IncomeOrOutgo", 1);
        startActivityForResult(i, REQUEST_CODE_PRZYCHOD);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data)
    {
        String tytul = null;
        String kwota = null;
        String data = null;
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_WYDATEK)
        {
            if (Data.hasExtra("tytul"))
                tytul = Data.getExtras().getString("tytul");
            if (Data.hasExtra("kwota"))
                kwota = Data.getExtras().getString("kwota");
            if (Data.hasExtra("data"))
                data = Data.getExtras().getString("data");

            if(!kwota.isEmpty())
            {

                addNewRow(tytul, kwota, data, 0);
                addToDatabase(tytul, kwota, data, 0);

                TextView stan = (TextView) findViewById(R.id.textViewStanIlosc);
                double kwotaD = Double.parseDouble(stan.getText().toString());
                kwotaD += Double.parseDouble(kwota);
                stan.setText(Double.toString(kwotaD));

                TextView wydatki = (TextView) findViewById(R.id.textViewWydatkiIlosc);
                double wydatkiD = Double.parseDouble(wydatki.getText().toString());
                wydatkiD += Double.parseDouble(kwota.substring(1));
                wydatki.setText(Double.toString(wydatkiD));

            }
        }
        else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PRZYCHOD)
        {
            if (Data.hasExtra("tytul"))
                tytul = Data.getExtras().getString("tytul");
            if (Data.hasExtra("kwota"))
                kwota = Data.getExtras().getString("kwota");
            if (Data.hasExtra("data"))
                data = Data.getExtras().getString("data");

            if(!kwota.isEmpty())
            {

                addNewRow(tytul, kwota, data, 1);
                addToDatabase(tytul, kwota, data, 1);

                TextView stan = (TextView) findViewById(R.id.textViewStanIlosc);
                double kwotaD = Double.parseDouble(stan.getText().toString());
                kwotaD += Double.parseDouble(kwota);
                stan.setText(Double.toString(kwotaD));

                TextView przychod = (TextView) findViewById(R.id.textViewPrzychodIlosc);
                double przychodD = Double.parseDouble(przychod.getText().toString());
                przychodD += Double.parseDouble(kwota);;
                przychod.setText(Double.toString(przychodD));
            }
        }
    }
    // int wydatek_przychod
    // wydatek == 0
    // przychod == 1
    private void addNewRow(String tytul, String kwota, String data, int wydatek_przychod)
    {
        TableRow tableRow = new TableRow(this);
        TableRow.LayoutParams size = new TableRow.LayoutParams();
        size.weight = 1;
        TableRow.LayoutParams size2 = new TableRow.LayoutParams();
        size2.weight = 2;

        TextView textViewTytul = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewTytul.setWidth(TableRow.LayoutParams.MATCH_PARENT);
        textViewTytul.setText(tytul);
        textViewTytul.setLayoutParams(size2);
        textViewTytul.setTextSize(20);
        tableRow.addView(textViewTytul);

        TextView textViewKwota = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewKwota.setLayoutParams(size);
        textViewKwota.setMaxEms(2);
        textViewKwota.setText(kwota);
        tableRow.addView(textViewKwota );

        TextView textViewData = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewData.setMaxEms(2);

        // Changes data show format from yyyy/mm/dd to dd/mm/yy
        String a = data.substring(2,4);
        String b = data.substring(8,10);
        textViewData.setText(b + data.substring(4,8) + a);
        textViewData.setLayoutParams(size);
        tableRow.addView(textViewData);


        if (wydatek_przychod == 0)
            tableRow.setBackgroundResource(R.color.wydatek);
        else
            tableRow.setBackgroundResource(R.color.przychod);

        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        tableLayout.addView(tableRow,1);


    }

    public void addToDatabase(String title, String value, String date, int incomeOrOutgo)
    {
        String sqlQuery =  null;
        sqlQuery = "INSERT INTO IncomeOutgo(Title, Value, Date, IncomeOrOutgo) VALUES (" +
                "\"" + title + "\"," +
                "\"" + value + "\"," +
                "\"" + date + "\"," +
                "\"" + incomeOrOutgo + "\")";

        db.execSQL(sqlQuery);

    }

    public void addHeaderRow()
    {
        TableRow.LayoutParams size = new TableRow.LayoutParams();
        size.weight = 1;
        TableRow.LayoutParams size2 = new TableRow.LayoutParams();
        size2.weight = 2;

        TableRow tableRow = new TableRow(this);
        TextView textViewTitle = (TextView) getLayoutInflater().inflate(R.layout.text_view_header_title, null);
        textViewTitle.setLayoutParams(size2);
        tableRow.addView(textViewTitle);

        TextView textViewValue = (TextView) getLayoutInflater().inflate(R.layout.text_view_header_value, null);
        textViewValue.setLayoutParams(size);
        tableRow.addView(textViewValue);

        TextView textViewDate = (TextView) getLayoutInflater().inflate(R.layout.text_view_header_date, null);
        textViewDate.setLayoutParams(size);
        tableRow.addView(textViewDate);

        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        tableLayout.addView(tableRow,0);
    }

    int titleState = 0;
    int valueState = 0;
    int dateState = 0;
    public void onClickTitle(View view)
    {

        String sqlQuery = null;
        if (titleState == 0)
        {
            titleState = 1;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Title ASC";
        }
        else
        {
            titleState = 0;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Title DESC";
        }
        // Creates database if not exists.
        db = openOrCreateDatabase("Wallet", MODE_PRIVATE, null);
        String sqlDB = "CREATE TABLE IF NOT EXISTS IncomeOutgo (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "Title VARCHAR," +
                "Value DOUBLE NOT NULL, Date DATE," +
                "IncomeOrOutgo INTEGER NOT NULL)";
        db.execSQL(sqlDB);


        // Loads data from database.
        ArrayList<Registration> registrations = new ArrayList<Registration>();
        Cursor cursor = db.rawQuery(sqlQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                int id = cursor.getInt(cursor.getColumnIndex("Id"));
                String title = cursor.getString(cursor.getColumnIndex("Title"));
                String value = cursor.getString(cursor.getColumnIndex("Value"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                int incomeOrOutgo = cursor.getInt(cursor.getColumnIndex("IncomeOrOutgo"));

                registrations.add(new Registration(id, title, value, date, incomeOrOutgo));

            } while (cursor.moveToNext());
        }

        // Clears all table's rows.
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        int count = tableLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
        }

        addHeaderRow();
        for (Registration x : registrations)
            addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);
    }

    public void onClickValue(View view)
    {
        String sqlQuery = null;
        if (valueState == 0)
        {
            valueState = 1;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Value ASC";
        }
        else
        {
            valueState = 0;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Value DESC";
        }
        // Creates database if not exists.
        db = openOrCreateDatabase("Wallet", MODE_PRIVATE, null);
        String sqlDB = "CREATE TABLE IF NOT EXISTS IncomeOutgo (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "Title VARCHAR," +
                "Value DOUBLE NOT NULL, Date DATE," +
                "IncomeOrOutgo INTEGER NOT NULL)";
        db.execSQL(sqlDB);

        // Loads data from database.
        ArrayList<Registration> registrations = new ArrayList<Registration>();
        Cursor cursor = db.rawQuery(sqlQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                int id = cursor.getInt(cursor.getColumnIndex("Id"));
                String title = cursor.getString(cursor.getColumnIndex("Title"));
                String value = cursor.getString(cursor.getColumnIndex("Value"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                int incomeOrOutgo = cursor.getInt(cursor.getColumnIndex("IncomeOrOutgo"));

                registrations.add(new Registration(id, title, value, date, incomeOrOutgo));

            } while (cursor.moveToNext());
        }

        // Clears all table's rows.
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        int count = tableLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
        }

        addHeaderRow();
        for (Registration x : registrations)
            addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);
    }

    public void onClickDate(View view)
    {
        String sqlQuery = null;
        if (dateState == 0)
        {
            dateState = 1;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Date ASC";
        }
        else
        {
            dateState = 0;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Date DESC";
        }
        // Creates database if not exists.
        db = openOrCreateDatabase("Wallet", MODE_PRIVATE, null);
        String sqlDB = "CREATE TABLE IF NOT EXISTS IncomeOutgo (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "Title VARCHAR," +
                "Value DOUBLE NOT NULL, Date DATE," +
                "IncomeOrOutgo INTEGER NOT NULL)";
        db.execSQL(sqlDB);


        // Loads data from database.
        ArrayList<Registration> registrations = new ArrayList<Registration>();
        Cursor cursor = db.rawQuery(sqlQuery, null);

        if (cursor.moveToFirst())
        {
            do
            {
                int id = cursor.getInt(cursor.getColumnIndex("Id"));
                String title = cursor.getString(cursor.getColumnIndex("Title"));
                String value = cursor.getString(cursor.getColumnIndex("Value"));
                String date = cursor.getString(cursor.getColumnIndex("Date"));
                int incomeOrOutgo = cursor.getInt(cursor.getColumnIndex("IncomeOrOutgo"));

                registrations.add(new Registration(id, title, value, date, incomeOrOutgo));

            } while (cursor.moveToNext());
        }

        // Clears all table's rows.
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        int count = tableLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
        }

        addHeaderRow();
        for (Registration x : registrations)
            addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);
    }

    private static Map<Integer, String> sortByValue(Map<Integer, String> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<Integer, String>> list =
                new LinkedList<Map.Entry<Integer, String>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<Integer, String>>() {
            public int compare(Map.Entry<Integer, String> o1,
                               Map.Entry<Integer, String> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<Integer, String> sortedMap = new LinkedHashMap<Integer, String>();
        for (Map.Entry<Integer, String> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        /*
        //classic iterator example
        for (Iterator<Map.Entry<Integer, String>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<Integer, String> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }*/


        return sortedMap;
    }
}
