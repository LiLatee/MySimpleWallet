package com.example.marcin.mysimplewallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
{
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadData();

        // Loads account balance, income and outgo.
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String stan = sharedPreferences.getString("stan", "0");
        String wydatki = sharedPreferences.getString("wydatki", "0");
        String przychod = sharedPreferences.getString("przychod", "0");

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
        i.putExtra("edit?", "false");
        startActivityForResult(i, REQUEST_CODE_WYDATEK);

    }

    public void onClickPrzychod(View view)
    {
        Intent i = new Intent(this, dodaj.class);
        // 0 - outgo
        // 1 - income
        i.putExtra("IncomeOrOutgo", 1);
        i.putExtra("edit?", "false");
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

            if (!kwota.isEmpty())
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

            if (!kwota.isEmpty())
            {

                addNewRow(tytul, kwota, data, 1);
                addToDatabase(tytul, kwota, data, 1);

                TextView stan = (TextView) findViewById(R.id.textViewStanIlosc);
                double kwotaD = Double.parseDouble(stan.getText().toString());
                kwotaD += Double.parseDouble(kwota);
                stan.setText(Double.toString(kwotaD));

                TextView przychod = (TextView) findViewById(R.id.textViewPrzychodIlosc);
                double przychodD = Double.parseDouble(przychod.getText().toString());
                przychodD += Double.parseDouble(kwota);
                ;
                przychod.setText(Double.toString(przychodD));
            }

        }
        else if (resultCode == RESULT_OK)
        {
            String edit;
            TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
            if (Data.hasExtra("edit?") || Data.getExtras().getString("edit?").equals("true"))
            {
                String oldTytul = Data.getExtras().getString("oldTitle");
                String oldKwota = Data.getExtras().getString("oldValue");
                String oldData = Data.getExtras().getString("oldDate");
                tableLayout.removeView(tableRowToEditDelete);
                String sqlQuery = " UPDATE IncomeOutgo SET" +
                        " Title = " + tytul +
                        ", Value = " + kwota +
                        ", Date = '" + data + "'" +
                        "WHERE Title = " + oldTytul +
                        " AND Value = " + oldKwota +
                        " AND Date = '" + oldData + "'";

                db.execSQL(sqlQuery);

                Toast.makeText(this, "Pozycja została zmieniona.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // int wydatek_przychod
    // wydatek == 0
    // przychod == 1
    public void addNewRow(String tytul, String kwota, String data, int wydatek_przychod)
    {
        TableRowWithContextMenuInfo tableRow = new TableRowWithContextMenuInfo(this);
        TableRowWithContextMenuInfo.LayoutParams size = new TableRowWithContextMenuInfo.LayoutParams();
        size.weight = 1;
        TableRowWithContextMenuInfo.LayoutParams size2 = new TableRowWithContextMenuInfo.LayoutParams();
        size2.weight = 2;

        TextView textViewTytul = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewTytul.setWidth(TableRowWithContextMenuInfo.LayoutParams.MATCH_PARENT);
        textViewTytul.setText(tytul);
        textViewTytul.setLayoutParams(size2);
        textViewTytul.setTextSize(20);
        tableRow.addView(textViewTytul);

        TextView textViewKwota = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewKwota.setLayoutParams(size);
        textViewKwota.setMaxEms(2);
        textViewKwota.setText(kwota);
        tableRow.addView(textViewKwota);

        TextView textViewData = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewData.setMaxEms(2);

        // Changes data show format from yyyy/mm/dd to dd/mm/yy
        String a = data.substring(2, 4);
        String b = data.substring(8, 10);
        textViewData.setText(b + data.substring(4, 8) + a);
        textViewData.setLayoutParams(size);
        tableRow.addView(textViewData);


        if (wydatek_przychod == 0)
        {
            tableRow.setBackgroundResource(R.color.wydatek);
            tableRow.setTag("0");
        }
        else
        {
            tableRow.setBackgroundResource(R.color.przychod);
            tableRow.setTag("1");
        }

        registerForContextMenu(tableRow);
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        tableLayout.addView(tableRow, 1);


    }

    public void addHeaderRow()
    {
        TableRowWithContextMenuInfo.LayoutParams size = new TableRowWithContextMenuInfo.LayoutParams();
        size.weight = 1;
        TableRowWithContextMenuInfo.LayoutParams size2 = new TableRowWithContextMenuInfo.LayoutParams();
        size2.weight = 2;

        TableRowWithContextMenuInfo tableRow = new TableRowWithContextMenuInfo(this);
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
        tableLayout.addView(tableRow, 0);
    }

    public void addToDatabase(String title, String value, String date, int incomeOrOutgo)
    {
        String sqlQuery = null;
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
        } else
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
        for (int i = 0; i < count; i++)
        {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRowWithContextMenuInfo) ((ViewGroup) child).removeAllViews();
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
        } else
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
        for (int i = 0; i < count; i++)
        {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRowWithContextMenuInfo) ((ViewGroup) child).removeAllViews();
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
        } else
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
        for (int i = 0; i < count; i++)
        {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRowWithContextMenuInfo) ((ViewGroup) child).removeAllViews();
        }

        addHeaderRow();
        for (Registration x : registrations)
            addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);
    }

    public void onClickFilter(MenuItem item)
    {
        filter_dialog dialog = new filter_dialog();
        dialog.show(getSupportFragmentManager(), "filterDialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    TableRow tableRowToEditDelete;
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TableRowWithContextMenuInfo.TableRowContextMenuInfo menuInfo = (TableRowWithContextMenuInfo.TableRowContextMenuInfo) item.getMenuInfo();
        TableRowWithContextMenuInfo tableRow = (TableRowWithContextMenuInfo) menuInfo.targetView;

        tableRowToEditDelete = tableRow;
        TableLayout tableLayout = (TableLayout) tableRow.getParent();

        TextView textViewTitle = (TextView) tableRow.getChildAt(0);
        TextView textViewValue = (TextView) tableRow.getChildAt(1);
        TextView textViewDate = (TextView) tableRow.getChildAt(2);

        String title = textViewTitle.getText().toString();
        String value = textViewValue.getText().toString();

        String date = textViewDate.getText().toString();
        String a = date.substring(0,2) ;
        String b = date.substring(6,8) ;
        date = "20" + b + date.substring(2,6) + a ; // from DD/MM/YY -> YYYY/MM/DD

        String incomeOrOutgo = tableRow.getTag().toString();

        switch (item.getItemId()) {
            case R.id.editRow:
            {
                Intent i = new Intent(this, dodaj.class);
                // 0 - outgo
                // 1 - income

                if (Double.parseDouble(value) < 0)
                    value = value.substring(1);
                i.putExtra("edit?", "true");
                i.putExtra("title", title);
                i.putExtra("value", value);
                i.putExtra("date", date);
                i.putExtra("IncomeOrOutgo",  Integer.parseInt(incomeOrOutgo) );
                startActivityForResult(i, Integer.parseInt(incomeOrOutgo));

                return true;
            }
            case R.id.deleteRow:
            {
                if (item instanceof TableRowWithContextMenuInfo) ((ViewGroup) item).removeAllViews();
                String sqlQuery = "DELETE FROM IncomeOutgo " +
                        "WHERE Title = " + title +
                        " AND Value = " + value +
                        " AND Date = '" + date + "'";
                Log.d("pies", sqlQuery);
                db.execSQL(sqlQuery);
                tableLayout.removeView(tableRow);
                Toast.makeText(this, "Pozycja została usunięta.", Toast.LENGTH_SHORT).show();
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }

        public void loadData()
        {
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

                    registrations.add(new Registration(id, title, value, date, incomeOrOutgo));

                } while (cursor.moveToNext());
            }


            // Clears all table's rows.
            TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
            int count = tableLayout.getChildCount();
            for (int i = 0; i < count; i++)
            {
                View child = tableLayout.getChildAt(i);
                if (child instanceof TableRowWithContextMenuInfo) ((ViewGroup) child).removeAllViews();
            }

            addHeaderRow();
            for (Registration x : registrations)
                addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);
        }
    }
