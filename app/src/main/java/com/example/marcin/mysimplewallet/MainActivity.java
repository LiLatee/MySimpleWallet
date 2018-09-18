package com.example.marcin.mysimplewallet;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
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

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity
{


    private static final int REQUEST_CODE_WYDATEK = 0;
    private static final int REQUEST_CODE_PRZYCHOD = 1;
    private static final int REQUEST_CODE_EDIT = 2;
    private static final int RC_SIGN_IN = 3;
    private static final int WRITE_EXTERNAL_STORAGE = 4;
    private static final int READ_EXTERNAL_STORAGE = 5;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final File BACKUP_FOLDER = new File(Environment.getExternalStorageDirectory().toString(), "MySimpleWalletBackup");
    private static final String FILENAME = "/backupMySimpleWallet";
    private static final File BACKUP_FILEPATH = new File(BACKUP_FOLDER, FILENAME);


    private SQLiteDatabase db;
    private TextView textViewBalance, textViewIncome, textViewOutgo;
    private SharedPreferences settings;
    private String selectedLanguage = "-";
    private StorageReference mStorageRef;
    private StorageReference riversRef;
    private UploadTask uploadTask;
    private Uri file;
    private FirebaseUser currentFirebaseUser;
    private File downloadedFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Language settings.
        settings = getPreferences(MODE_PRIVATE);
        selectedLanguage = settings.getString("language", "-");

        setContentView(R.layout.activity_main);

        textViewBalance = (TextView) findViewById(R.id.textViewBalanceValue);
        textViewOutgo = (TextView) findViewById(R.id.textViewOutgoValue);
        textViewIncome = (TextView) findViewById(R.id.textViewIncomeValue);
        addHeaderRow();

        // Creates backup folder.
        if (!BACKUP_FOLDER.exists())
            BACKUP_FOLDER.mkdirs(); // this will create folder.

        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        try
        {
            downloadedFile = File.createTempFile("tempFromServer", null, BACKUP_FOLDER);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        sendQueryAndShow("SELECT * FROM IncomeOutgo");


    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Language settings.
        settings = getPreferences(MODE_PRIVATE);
        selectedLanguage = settings.getString("language", "-");

        if (!selectedLanguage.equals("-"))
        {

            Locale locale = new Locale(selectedLanguage);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
            settings.edit().putString("language", selectedLanguage).apply();
        }

        // Permissions granted.
        askForPermissions();


        if (currentFirebaseUser == null && settings.getString("askForLogin", "yes").equals("yes"))
            onClickAskForLogin(null);

        if (currentFirebaseUser != null)
        {
            synchData();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {

        if (requestCode == READ_EXTERNAL_STORAGE || requestCode == WRITE_EXTERNAL_STORAGE)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                finish();
                Intent refresh = new Intent(getBaseContext(), MainActivity.class);
                startActivity(refresh);
            } else
            {
                Toast.makeText(this, "Nie przyznano wymaganych uprawnień.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

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

        // Login activity.
        if (requestCode == RC_SIGN_IN)
        {
            IdpResponse response = IdpResponse.fromResultIntent(Data);

            if (resultCode == RESULT_OK)
            {
                currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                Toast.makeText(getBaseContext(), "Zalogowano", Toast.LENGTH_SHORT).show();

                synchData();

            } else
            {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Toast.makeText(getBaseContext(), "Logowanie nie powiodło się", Toast.LENGTH_SHORT).show();

            }
        }


        // Add outgo and income activities.
        String title = null;
        String value = null;
        String date = null;
        int incomeOrOutgo = 0;

        if (resultCode == RESULT_OK)
        {
            if (Data.hasExtra("title"))
                title = Data.getExtras().getString("title");
            if (Data.hasExtra("value"))
                value = Data.getExtras().getString("value");
            if (Data.hasExtra("date"))
                date = Data.getExtras().getString("date");
            if (Data.hasExtra("incomeOrOutgo"))
                incomeOrOutgo = Data.getExtras().getInt("incomeOrOutgo");

            if (requestCode == REQUEST_CODE_WYDATEK)
            {
                addNewRow(title, value, date, 0);
                addToDatabase(title, value, date, 0);

                try
                {
                    saveAllDataToFile(); // Updates local backup.
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                sendLocalFileToServer(); // Updates server backup.


                TextView balance = (TextView) findViewById(R.id.textViewBalanceValue);
                double valueD = Double.parseDouble(balance.getText().toString());
                valueD += Double.parseDouble(value);
                balance.setText(Double.toString(valueD));

                TextView wydatki = (TextView) findViewById(R.id.textViewOutgoValue);
                double wydatkiD = Double.parseDouble(wydatki.getText().toString());
                wydatkiD += Double.parseDouble(value.substring(1));
                wydatki.setText(Double.toString(wydatkiD));


            } else if (requestCode == REQUEST_CODE_PRZYCHOD)
            {

                addNewRow(title, value, date, 1);
                addToDatabase(title, value, date, 1);

                try
                {
                    saveAllDataToFile(); // Updates local backup.
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                sendLocalFileToServer(); // Updates server backup.


                TextView balance = (TextView) findViewById(R.id.textViewBalanceValue);
                double valueD = Double.parseDouble(balance.getText().toString());
                valueD += Double.parseDouble(value);
                balance.setText(Double.toString(valueD));

                TextView przychod = (TextView) findViewById(R.id.textViewIncomeValue);
                double przychodD = Double.parseDouble(przychod.getText().toString());
                przychodD += Double.parseDouble(value);

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
                            " Title = '" + title + "'" +
                            ", Value = " + value +
                            ", Date = '" + date + "'" +
                            " WHERE Title = '" + oldTytul + "'" +
                            " AND Value = " + oldKwota +
                            " AND Date = '" + oldData + "'";

                    db.execSQL(sqlQuery);


                    // Updates main information.
                    Double oldBalance = Double.parseDouble(textViewBalance.getText().toString());
                    Double oldIncome = Double.parseDouble(textViewIncome.getText().toString());
                    Double oldOutgo = Double.parseDouble(textViewOutgo.getText().toString());

                    Double newBalance = oldBalance - Double.parseDouble(oldKwota) + Double.parseDouble(value);
                    textViewBalance.setText(Double.toString(newBalance));
                    Double newIncome, newOutgo;

                    if (incomeOrOutgo == 1)
                    {
                        newIncome = oldIncome - Double.parseDouble(oldKwota) + Double.parseDouble(value);
                        textViewIncome.setText(Double.toString(newIncome));
                    } else
                    {
                        newOutgo = oldOutgo + Double.parseDouble(oldKwota) - Double.parseDouble(value);
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
    public void addNewRow(String title, String value, String date, int wydatek_przychod)
    {
        final String titleF = title;
        final String valueF = value;
        final String dateF = date;
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
        textViewTytul.setText(title);
        textViewTytul.setLayoutParams(size2);
        textViewTytul.setTextSize(20);
        tableRow.addView(textViewTytul);

        TextView textViewKwota = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewKwota.setLayoutParams(size);
        textViewKwota.setMaxEms(2);
        textViewKwota.setText(value);
        tableRow.addView(textViewKwota);

        TextView textViewData = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewData.setMaxEms(2);

        // Changes date show format from yyyy/mm/dd to dd/mm/yy
        String a = date.substring(2, 4);
        String b = date.substring(8, 10);
        textViewData.setText(b + date.substring(4, 8) + a);
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
        tableLayout.addView(tableRow, 0);


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

        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayoutHeader);
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
            if (child instanceof TableRowWithContextMenuInfo)
                ((ViewGroup) child).removeAllViews();
        }

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
            if (child instanceof TableRowWithContextMenuInfo)
                ((ViewGroup) child).removeAllViews();
        }

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
            if (child instanceof TableRowWithContextMenuInfo)
                ((ViewGroup) child).removeAllViews();
        }

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
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener()
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

                try
                {
                    saveAllDataToFile();
                    synchData();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo
            menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    private TableRow tableRowToEditDelete;

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
                i.putExtra("language", selectedLanguage);
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
                db.execSQL(sqlQuery);
                tableLayout.removeView(tableRow);
                Toast.makeText(this, getString(R.string.info_registration_deleted), Toast.LENGTH_SHORT).show();

                try
                {
                    saveAllDataToFile(); // Updates local backup.
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                sendLocalFileToServer(); // Updates server backup.
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
        for (int i = 0; i < count; i++)
        {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRowWithContextMenuInfo)
                ((ViewGroup) child).removeAllViews();
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

    public void saveAllDataToFile() throws IOException
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

        // First line is last modified time.
        FileWriter writer = new FileWriter(BACKUP_FILEPATH);
        DateFormat dateFormat = SDF;
        Date date = new Date();
        writer.write(dateFormat.format(date) + "\n"); //2016/11/16 12:08:43

        // Rest of lines = data.
        for (Registration x : registrations)
            writer.write(x.id + "\n" + x.title + "\n" + x.value + "\n" + x.date + "\n" + x.incomeOrOutgo + "\n");

        writer.flush();
        writer.close();
    }

    public void onClickLoadFile(MenuItem item)
    {
        final Scanner scanner;
        try
        {
            scanner = new Scanner(BACKUP_FILEPATH);
            scanner.useDelimiter("\\n");
        } catch (FileNotFoundException e)
        {

            String message = getString(R.string.info_about_restoring_1) + BACKUP_FILEPATH.getPath().toString() +
                    getString(R.string.info_about_restoring_2) + "\n\n";

            TextView showText = new TextView(this);
            showText.setGravity(Gravity.CENTER);
            showText.setTextSize(15);
            showText.setTextColor(Color.BLACK);
            showText.setText(message);
            showText.setTextIsSelectable(true);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.information));
            builder.setView(showText);

            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }

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
                scanner.nextLine(); // skip line with last modified time
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

    public void onClickChangeLanguage(MenuItem item)
    {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
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

                settings = getPreferences(MODE_PRIVATE);
                Locale locale = new Locale(selectedLanguage);
                Locale.setDefault(locale);
                Configuration config = new Configuration();
                config.locale = locale;
                getBaseContext().getResources().updateConfiguration(config,
                        getBaseContext().getResources().getDisplayMetrics());
                settings.edit().putString("language", selectedLanguage).commit();

                finish();
                Intent refresh = new Intent(getBaseContext(), MainActivity.class);
                startActivity(refresh);

            }
        });
        builderSingle.show();

    }

    public void onClickAskForLogin(MenuItem item)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Czy chciałbyś się zalogować?");
        builder.setMessage("Możesz zalogować się na swoje konto Google i przechowywać dane w chmurze" +
                " lub trzymać dane tylko lokalnie na urządzniu.\n" +
                "Pamiętaj, możesz stworzyć plik z kopią zapasową i wgrać go na inne urządzenie, bez konieczności logowania.");
        builder.setPositiveButton("Połącz z kontem Google", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Choose authentication providers
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.GoogleBuilder().build());

                // Create and launch sign-in intent
                startActivityForResult(AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);

            }
        });
        builder.setNegativeButton("Pomiń", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Toast.makeText(getBaseContext(), "Opcja połączenia z kontem dostepna w ustawieniach. ", Toast.LENGTH_LONG).show();
                settings.edit().putString("askForLogin", "no").apply();
                return;
            }
        });


        AlertDialog dialog = builder.create();
        dialog.show();


    }

    public void synchData()
    {
        final File localFile = new File(Environment.getExternalStorageDirectory().toString() + "/MySimpleWalletBackup" + FILENAME);

        // Load data from server.
        mStorageRef = FirebaseStorage.getInstance().getReference();
        riversRef = mStorageRef.child(currentFirebaseUser.getUid() + FILENAME);
        riversRef.getFile(downloadedFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
                {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                    {
                        try
                        {
                            Scanner localScanner = null;
                            ArrayList<Registration> registrations = null;
                            Date dateLocalFile = null;
                            Date dateServerFile = null;

                            // Get time from server file.
                            Scanner serverScanner = new Scanner(downloadedFile);
                            serverScanner.useDelimiter("\\n");


                            registrations = new ArrayList<Registration>();
                            registrations = new ArrayList<Registration>();
                            String serverDateS = serverScanner.nextLine();

                            try
                            {
                                dateServerFile = SDF.parse(serverDateS);
                            } catch (ParseException e)
                            {
                                e.printStackTrace();
                            }

                            if (localFile.length() != 0)
                            {
                                // Get time from local file.
                                try
                                {
                                    localScanner = new Scanner(localFile);
                                    localScanner.useDelimiter("\\n");
                                } catch (FileNotFoundException e)
                                {
                                    e.printStackTrace();
                                }

                                registrations = new ArrayList<Registration>();
                                String localDateS = localScanner.nextLine();
                                dateLocalFile = null;
                                try
                                {
                                    dateLocalFile = SDF.parse(localDateS);
                                } catch (ParseException e)
                                {
                                    e.printStackTrace();
                                }

                                // If server backup is newer than local backup, use server backup, else use local backup.
                                if (dateServerFile.after(dateLocalFile))
                                {
                                    // Clears all data.
                                    deleteDatabase("Wallet");
                                    textViewBalance.setText("0");
                                    textViewIncome.setText("0");
                                    textViewOutgo.setText("0");
                                    sendQueryAndShow("SELECT * FROM IncomeOutgo");

                                    while (serverScanner.hasNextLine())
                                    {
                                        int id, incomeOrOutgo;
                                        String title, value, date;
                                        id = Integer.parseInt(serverScanner.nextLine());
                                        title = serverScanner.nextLine();
                                        value = serverScanner.nextLine();
                                        date = serverScanner.nextLine();
                                        incomeOrOutgo = Integer.parseInt(serverScanner.nextLine());

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

                                    try
                                    {
                                        saveAllDataToFile(); // updates local file
                                    } catch (IOException e)
                                    {
                                        e.printStackTrace();
                                    }
                                } else
                                {
                                    Log.d("pies", "local");

                                    sendQueryAndShow("SELECT * FROM IncomeOutgo");
                                    sendLocalFileToServer();
                                }
                            } else // If local backup is just created.
                            {
                                // Clears all data.
                                deleteDatabase("Wallet");
                                textViewBalance.setText("0");
                                textViewIncome.setText("0");
                                textViewOutgo.setText("0");
                                sendQueryAndShow("SELECT * FROM IncomeOutgo");

                                while (serverScanner.hasNextLine())
                                {
                                    int id, incomeOrOutgo;
                                    String title, value, date;
                                    id = Integer.parseInt(serverScanner.nextLine());
                                    title = serverScanner.nextLine();
                                    value = serverScanner.nextLine();
                                    date = serverScanner.nextLine();
                                    incomeOrOutgo = Integer.parseInt(serverScanner.nextLine());

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

                                try
                                {
                                    saveAllDataToFile(); // updates local file
                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                            }

                        } catch (FileNotFoundException e)
                        {
                            e.printStackTrace();
                        }
                        downloadedFile.delete();
                    }


                })
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception exception)
                    {
                        exception.printStackTrace();
                        // Probably no needed.
                    }
                });

    }

    public void onClickRefresh(MenuItem item)
    {
        // Checks if exists newer version.
        sendQueryAndShow("SELECT * FROM IncomeOutgo");

        if (currentFirebaseUser != null)
            synchData();


    }

    public void sendLocalFileToServer()
    {
        if (currentFirebaseUser != null)
        {
            // Send backup file on server.
            mStorageRef = FirebaseStorage.getInstance().getReference();
            file = Uri.fromFile(BACKUP_FILEPATH);
            riversRef = mStorageRef.child(currentFirebaseUser.getUid() + "/" + file.getLastPathSegment());
            uploadTask = riversRef.putFile(file);


            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception exception)
                {
                    // Handle unsuccessful uploads
                    exception.printStackTrace();

                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    // ...
                }
            });

            // Deletes temp file.
            //downloadedFile.delete();
        }

    }

    public void askForPermissions()
    {
        // Ask for permissions.
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
        } else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
        }

    }


}

