package com.example.marcin.mysimplewallet;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity
{
    private SQLiteDatabase db;
    private TextView textViewBalance, textViewIncome, textViewOutgo;
    public Context context;
    public SharedPreferences settings;
    public String selectedLanguage = null;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Language settings.
        settings = getPreferences(MODE_PRIVATE);

        if (settings.getString("locale", "en").equals("en"))
            selectedLanguage = "en";
        else
            selectedLanguage = "pl";

        Locale locale = new Locale(selectedLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        settings.edit().putString("locale", selectedLanguage).commit();

        setContentView(R.layout.activity_main);


        // Creates backup folder.
        File root = new File(Environment.getExternalStorageDirectory().toString(), "MySimpleWalletBackup");
        if (!root.exists())
        {
            root.mkdirs(); // this will create folder.
        }

        textViewBalance = (TextView) findViewById(R.id.textViewBalanceValue);
        textViewOutgo = (TextView) findViewById(R.id.textViewOutgoValue);
        textViewIncome = (TextView) findViewById(R.id.textViewIncomeValue);


        addHeaderRow();
        sendQueryAndShow("SELECT * FROM IncomeOutgo");

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        settings = getPreferences(MODE_PRIVATE);

        if (settings.getString("locale", "en").equals("en"))
            selectedLanguage = "en";
        else
            selectedLanguage = "pl";

        Locale locale = new Locale(selectedLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        settings.edit().putString("locale", selectedLanguage).commit();
    }

    static final int REQUEST_CODE_WYDATEK = 0;
    static final int REQUEST_CODE_PRZYCHOD = 1;
    static final int REQUEST_CODE_EDIT = 2;

    public void onClickOutgo(View view)
    {
        Intent i = new Intent(this, add.class);
        // 0 - outgo
        // 1 - income
        i.putExtra("IncomeOrOutgo", 0);
        i.putExtra("edit?", "false");
        i.putExtra("language", selectedLanguage);
        startActivityForResult(i, REQUEST_CODE_WYDATEK);

    }

    public void onClickIncome(View view)
    {
        Intent i = new Intent(this, add.class);
        // 0 - outgo
        // 1 - income
        i.putExtra("IncomeOrOutgo", 1);
        i.putExtra("edit?", "false");
        i.putExtra("language", selectedLanguage);
        startActivityForResult(i, REQUEST_CODE_PRZYCHOD);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data)
    {
        String tytul = null;
        String kwota = null;
        String data = null;
        int incomeOrOutgo = 0;

        if (resultCode == RESULT_OK)
        {

            if (Data.hasExtra("tytul"))
                tytul = Data.getExtras().getString("tytul");
            if (Data.hasExtra("kwota"))
                kwota = Data.getExtras().getString("kwota");
            if (Data.hasExtra("data"))
                data = Data.getExtras().getString("data");
            if (Data.hasExtra("incomeOrOutgo"))
                incomeOrOutgo = Data.getExtras().getInt("incomeOrOutgo");

            if (requestCode == REQUEST_CODE_WYDATEK)
            {
                addNewRow(tytul, kwota, data, 0);
                addToDatabase(tytul, kwota, data, 0);

                TextView stan = (TextView) findViewById(R.id.textViewBalanceValue);
                double kwotaD = Double.parseDouble(stan.getText().toString());
                kwotaD += Double.parseDouble(kwota);
                stan.setText(Double.toString(kwotaD));

                TextView wydatki = (TextView) findViewById(R.id.textViewOutgoValue);
                double wydatkiD = Double.parseDouble(wydatki.getText().toString());
                wydatkiD += Double.parseDouble(kwota.substring(1));
                wydatki.setText(Double.toString(wydatkiD));


            } else if (requestCode == REQUEST_CODE_PRZYCHOD)
            {

                addNewRow(tytul, kwota, data, 1);
                addToDatabase(tytul, kwota, data, 1);

                TextView stan = (TextView) findViewById(R.id.textViewBalanceValue);
                double kwotaD = Double.parseDouble(stan.getText().toString());
                kwotaD += Double.parseDouble(kwota);
                stan.setText(Double.toString(kwotaD));

                TextView przychod = (TextView) findViewById(R.id.textViewIncomeValue);
                double przychodD = Double.parseDouble(przychod.getText().toString());
                przychodD += Double.parseDouble(kwota);

                przychod.setText(Double.toString(przychodD));


            } else if (requestCode == REQUEST_CODE_EDIT)
            {
                TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
                if (Data.hasExtra("edit?") || Data.getExtras().getString("edit?").equals("true"))
                {
                    String oldTytul = Data.getExtras().getString("oldTitle");
                    String oldKwota = Data.getExtras().getString("oldValue");
                    String oldData = Data.getExtras().getString("oldDate");
                    tableLayout.removeView(tableRowToEditDelete);
                    String sqlQuery = " UPDATE IncomeOutgo SET" +
                            " Title = '" + tytul + "'" +
                            ", Value = " + kwota +
                            ", Date = '" + data + "'" +
                            " WHERE Title = '" + oldTytul + "'" +
                            " AND Value = " + oldKwota +
                            " AND Date = '" + oldData + "'";

                    db.execSQL(sqlQuery);

                    // Updates main information.
                    Double oldBalance = Double.parseDouble(textViewBalance.getText().toString());
                    Double oldIncome = Double.parseDouble(textViewIncome.getText().toString());
                    Double oldOutgo = Double.parseDouble(textViewOutgo.getText().toString());

                    Double newBalance = oldBalance - Double.parseDouble(oldKwota) + Double.parseDouble(kwota);
                    textViewBalance.setText(Double.toString(newBalance));
                    Double newIncome, newOutgo;

                    if (incomeOrOutgo == 1)
                    {
                        newIncome = oldIncome - Double.parseDouble(oldKwota) + Double.parseDouble(kwota);
                        textViewIncome.setText(Double.toString(newIncome));
                    } else
                    {
                        newOutgo = oldOutgo + Double.parseDouble(oldKwota) - Double.parseDouble(kwota);
                        textViewOutgo.setText(Double.toString(newOutgo));
                    }
                    sendQueryAndShow("SELECT * FROM IncomeOutgo");
                    Toast.makeText(this, getString(R.string.info_registration_changed), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // int wydatek_przychod
    // wydatek == 0
    // przychod == 1
    public void addNewRow(String tytul, String kwota, String data, int wydatek_przychod)
    {
        final String titleF = tytul;
        final String valueF = kwota;
        final String dateF = data;
        View.OnClickListener onClickListener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent i = new Intent(getBaseContext(), preview.class);
                i.putExtra("language", selectedLanguage);
                i.putExtra("title", titleF);
                i.putExtra("value", valueF);
                i.putExtra("date", dateF);
                startActivity(i);
            }

        };

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
        } else
        {
            tableRow.setBackgroundResource(R.color.przychod);
            tableRow.setTag("1");
        }

        registerForContextMenu(tableRow);
        tableRow.setOnClickListener(onClickListener);
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

    //TODO: sprawdzić czy tu musza być cudzysłowa
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

    public void onClickClearAll(MenuItem item)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(getString(R.string.info_clear_all_question));
        builder.setPositiveButton("TAK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                deleteDatabase("Wallet");
                SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                prefsEditor.clear();
                prefsEditor.apply();
                textViewBalance.setText("0");
                textViewIncome.setText("0");
                textViewOutgo.setText("0");
                sendQueryAndShow("SELECT * FROM IncomeOutgo");

                Toast.makeText(getBaseContext(), getString(R.string.info_clear_all), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Toast.makeText(getBaseContext(), getString(R.string.info_canceled), Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public Menu menu;

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setQueryHint(getString(R.string.search_title));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String s)
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s)
            {

                String sqlQuery = "SELECT * FROM IncomeOutgo WHERE Title LIKE '%" + s + "%'";
                sendQueryAndShow(sqlQuery);

                return false;
            }
        });


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
    public boolean onContextItemSelected(MenuItem item)
    {
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
        String a = date.substring(0, 2);
        String b = date.substring(6, 8);
        date = "20" + b + date.substring(2, 6) + a; // from DD/MM/YY -> YYYY/MM/DD

        String incomeOrOutgo = tableRow.getTag().toString();

        switch (item.getItemId())
        {
            case R.id.editRow:
            {
                Intent i = new Intent(this, add.class);
                // 0 - outgo
                // 1 - income

                if (Double.parseDouble(value) < 0)
                    value = value.substring(1);
                i.putExtra("edit?", "true");
                i.putExtra("title", title);
                i.putExtra("value", value);
                i.putExtra("date", date);
                i.putExtra("IncomeOrOutgo", Integer.parseInt(incomeOrOutgo));
                startActivityForResult(i, REQUEST_CODE_EDIT);

                return true;
            }
            case R.id.deleteRow:
            {
                if (item instanceof TableRowWithContextMenuInfo)
                    ((ViewGroup) item).removeAllViews();
                String sqlQuery = "DELETE FROM IncomeOutgo " +
                        "WHERE Title = '" + title + "'" +
                        " AND Value = " + value +
                        " AND Date = '" + date + "'";
                Log.d("pies", sqlQuery);
                db.execSQL(sqlQuery);
                tableLayout.removeView(tableRow);
                Toast.makeText(this, getString(R.string.info_registration_deleted), Toast.LENGTH_SHORT).show();
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }


    public void sendQueryAndShow(String sqlQuery)
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
        for (int i = 1; i < count; i++)
        {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRowWithContextMenuInfo) ((ViewGroup) child).removeAllViews();
        }

        Double balance = 0.0, income = 0.0, outgo = 0.0;

        // Sets main information and proper rows.
        for (Registration x : registrations)
        {
            addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);
            if (x.incomeOrOutgo == 0)
                outgo -= Double.parseDouble(x.value);
            else
                income += Double.parseDouble(x.value);

            balance += Double.parseDouble(x.value);

        }
        textViewBalance.setText(Double.toString(balance));
        textViewIncome.setText(Double.toString(income));
        textViewOutgo.setText(Double.toString(outgo));


    }

    final static String FILENAME = "/backupMySimpleWallet";
    private static final int WRITE_EXTERNAL_STORAGE = 1;
    private static final int READ_EXTERNAL_STORAGE = 1;

    public void onClickSaveFile(MenuItem item) throws IOException
    {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
        } else
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

            File root = new File(Environment.getExternalStorageDirectory().toString(), "MySimpleWalletBackup");
            if (!root.exists())
            {
                root.mkdirs(); // this will create folder.
            }
            File filepath = new File(root, FILENAME);  // file path to save
            FileWriter writer = new FileWriter(filepath);

            //PrintWriter writer = new PrintWriter(file);
            for (Registration x : registrations)
                writer.write(x.id + "\n" + x.title + "\n" + x.value + "\n" + x.date + "\n" + x.incomeOrOutgo + "\n");

            writer.flush();
            writer.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.information));
            String message = getString(R.string.info_about_saving_1) + filepath.getPath().toString() +
                    getString(R.string.info_about_saving_1);

            builder.setMessage(message);

            AlertDialog dialog = builder.create();
            dialog.show();
        }


    }

    public void onClickLoadFile(MenuItem item) throws IOException
    {

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
        } else
        {
            File root = new File(Environment.getExternalStorageDirectory().toString(), "MySimpleWalletBackup");
            File filepath = new File(root, FILENAME);  // file path to save

            if (!filepath.exists())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.information));
                String message = getString(R.string.info_about_restoring_1) + filepath.getPath().toString() +
                        getString(R.string.info_about_restoring_2);

                builder.setMessage(message);

                AlertDialog dialog = builder.create();
                dialog.show();
            }
            final Scanner scanner = new Scanner(filepath);
            scanner.useDelimiter("\\n");

            // Clears all data.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle(getString(R.string.warning));
            builder.setMessage(getString(R.string.info_clear_all_question));
            builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    deleteDatabase("Wallet");
                    textViewBalance.setText("0");
                    textViewIncome.setText("0");
                    textViewOutgo.setText("0");
                    sendQueryAndShow("SELECT * FROM IncomeOutgo");


                    ArrayList<Registration> registrations = new ArrayList<Registration>();

                    while (scanner.hasNextLine())
                    {
                        int id, incomeOrOutgo;
                        String title, value, date;
                        id = Integer.parseInt(scanner.nextLine());
                        title = scanner.nextLine();
                        value = scanner.nextLine();
                        date = scanner.nextLine();
                        incomeOrOutgo = Integer.parseInt(scanner.nextLine());

                        registrations.add(new Registration(id, title, value, date, incomeOrOutgo));
                    }


                    // Sets main information and proper rows.

                    Double balance = 0.0, income = 0.0, outgo = 0.0;
                    for (Registration x : registrations)
                    {
                        addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);
                        addToDatabase(x.title, x.value, x.date, x.incomeOrOutgo);
                        if (x.incomeOrOutgo == 0)
                            outgo -= Double.parseDouble(x.value);
                        else
                            income += Double.parseDouble(x.value);

                        balance += Double.parseDouble(x.value);

                    }
                    textViewBalance.setText(Double.toString(balance));
                    textViewIncome.setText(Double.toString(income));
                    textViewOutgo.setText(Double.toString(outgo));

                    Toast.makeText(getBaseContext(), getString(R.string.info_data_loaded), Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Toast.makeText(getBaseContext(), getString(R.string.info_canceled), Toast.LENGTH_SHORT).show();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }

    public void onClickChangeLanguage(MenuItem item)
    {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setIcon(R.drawable.filter);
        builderSingle.setTitle(getString(R.string.select_language));

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        arrayAdapter.add("English");
        arrayAdapter.add("Polski");


        builderSingle.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String strName = arrayAdapter.getItem(which);

                if (strName.equals("English"))
                    selectedLanguage = "en";
                else if (strName.equals("Polski"))
                    selectedLanguage = "pl";

                Locale locale = new Locale(selectedLanguage);
                Locale.setDefault(locale);
                Configuration config = new Configuration();
                config.locale = locale;
                getBaseContext().getResources().updateConfiguration(config,
                        getBaseContext().getResources().getDisplayMetrics());
                settings.edit().putString("locale", selectedLanguage).commit();
                finish();
                Intent refresh = new Intent(getBaseContext(), MainActivity.class);
                startActivity(refresh);

            }
        });
        builderSingle.show();

    }

}
