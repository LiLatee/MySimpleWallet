package com.example.marcin.mysimplewallet;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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
    EditText textViewFrom = null;
    EditText textViewTo = null;
    public void onClickFilter(MenuItem item)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.filter, null);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        Toast.makeText(getBaseContext(), "Hejka", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ANULUJ", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getBaseContext(), "ANULUJ", Toast.LENGTH_SHORT).show();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Date date = null;
        try {date = calendar.getTime(); } catch (Exception e) {e.printStackTrace();}
        String data = formatter.format(date);


        textViewFrom = (EditText) view.findViewById(R.id.editTextFrom);
        textViewFrom.setText(data);

        textViewTo = (EditText) view.findViewById(R.id.editTextTo);
        textViewTo.setText(data);
    }

    public void onClickCalendar(View view)
    {
        final View view2 = view;
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth)
            {
                String dayS = Integer.toString(dayOfMonth);
                String monthS = Integer.toString(month + 1);

                if (month + 1 < 10)
                    monthS = "0" + monthS;
                if (dayOfMonth < 10)
                    dayS = "0" + dayS;



                if (view2.getTag().toString().equals("from"))
                {
                    textViewFrom.setText((year) + "/" + monthS + "/" + dayS);
                } else
                {
                    textViewTo.setText((year) + "/" + monthS + "/" + dayS);
                }
            }
        };

        Calendar calendar = Calendar.getInstance();

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, 0, listener, year, month, day);
        datePickerDialog.show();

        String dayS = Integer.toString(datePickerDialog.getDatePicker().getDayOfMonth());
        String monthS = Integer.toString(datePickerDialog.getDatePicker().getMonth() + 1);
        year = datePickerDialog.getDatePicker().getYear();

        if (month + 1 < 10)
            monthS = "0" + monthS;
        if (day < 10)
            dayS = "0" + dayS;



        /*if (view.getTag().toString().equals("from"))
        {
            EditText textViewFrom = (EditText) findViewById(R.id.editTextFrom);
            textViewFrom.setText((year) + "/" + monthS + "/" + dayS);
        } else
        {
            EditText textViewTo = (EditText) findViewById(R.id.editTextTo);
            textViewTo.setText((year) + "/" + monthS + "/" + dayS);
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
}
