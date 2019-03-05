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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

// new branch check
public class MainActivity extends AppCompatActivity
{


    private static final int REQUEST_CODE_OUTGO = 0;
    private static final int REQUEST_CODE_INCOME = 1;
    private static final int REQUEST_CODE_EDIT = 2;
    private static final int RC_SIGN_IN = 3;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final File BACKUP_FOLDER = new File(Environment.getExternalStorageDirectory().toString(), "MySimpleWalletBackup");
    private static final String FILENAME = "/backupMySimpleWallet";
    private static final File BACKUP_FILEPATH = new File(BACKUP_FOLDER, FILENAME);
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    private static DecimalFormat decimalFormat = (DecimalFormat) numberFormat;


    private SQLiteDatabase db;
    private TextView textViewBalance, textViewIncome, textViewOutgo;
    private SharedPreferences settings;
    private String selectedLanguage = "-";
    private FirebaseUser currentFirebaseUser;
    private StorageReference serverFileRef;
    private StorageReference localFileRef;
    private File downloadedFile = null;
    private ArrayList<Registry> registries;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        decimalFormat.applyPattern(".##");

        // Language settings.
        settings = getPreferences(MODE_PRIVATE);
        selectedLanguage = settings.getString("language", "-");


        // UI settings
        setContentView(R.layout.activity_main);
        textViewBalance = (TextView) findViewById(R.id.textViewBalanceValue);
        textViewOutgo = (TextView) findViewById(R.id.textViewOutgoValue);
        textViewIncome = (TextView) findViewById(R.id.textViewIncomeValue);
        addHeaderRow();

        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        registries = new ArrayList<Registry>();

    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Language settings.
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

        if (currentFirebaseUser == null && settings.getString("askForLogin", "yes").equals("yes"))
        {
            onClickAskForLogin(null);
        } else
            refreshTable();

    }

    public void onClickOutgo(View view)
    {
        Intent i = new Intent(this, add.class);
        // 0 - outgo
        // 1 - income
        i.putExtra("IncomeOrOutgo", 0);
        i.putExtra("edit?", "false");
        i.putExtra("language", selectedLanguage);
        startActivityForResult(i, REQUEST_CODE_OUTGO);

    }

    public void onClickIncome(View view)
    {
        Intent i = new Intent(this, add.class);
        // 0 - outgo
        // 1 - income
        i.putExtra("IncomeOrOutgo", 1);
        i.putExtra("edit?", "false");
        i.putExtra("language", selectedLanguage);
        startActivityForResult(i, REQUEST_CODE_INCOME);

    }

    public void refreshTable(ArrayList<Registry> registries)
    {

        Float balance = 0.0f;
        Float income = 0.0f;
        Float outgo = 0.0f;

        clearRows();
        for (Registry registry : registries)
        {
            addNewRow(registry);
            if (registry.value < 0)
                outgo -= registry.value;
            else
                income += registry.value;
            balance += registry.value;
        }

        textViewBalance.setText(decimalFormat.format(balance));
        textViewIncome.setText(decimalFormat.format(income));
        textViewOutgo.setText(decimalFormat.format(outgo));
    }



    public void refreshTable()
    {
        registries = new ArrayList<Registry>();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("users/" + currentFirebaseUser.getUid());
        database.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Float balance = 0.0f;
                Float income = 0.0f;
                Float outgo = 0.0f;

                clearRows();
                registries.clear();
                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    Registry registry = child.getValue(Registry.class);
                    addNewRow(registry);
                    registries.add(registry);
                    if (registry.value < 0)
                        outgo -= registry.value;
                    else
                        income += registry.value;
                    balance += registry.value;
                }

                textViewBalance.setText(decimalFormat.format(balance));
                textViewIncome.setText(decimalFormat.format(income));
                textViewOutgo.setText(decimalFormat.format(outgo));
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Toast.makeText(getBaseContext(), "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data)
    {

        // Login activity.
        if (requestCode == RC_SIGN_IN)
        {
            if (resultCode == RESULT_OK)
            {
                currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                Toast.makeText(getBaseContext(), R.string.logged_up, Toast.LENGTH_SHORT).show();
                refreshTable();

                //synchData();

            } else
            {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Toast.makeText(getBaseContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();

            }
        } else if (requestCode == REQUEST_CODE_OUTGO || requestCode == REQUEST_CODE_INCOME || requestCode == REQUEST_CODE_EDIT)
        {
            if (resultCode == RESULT_OK)
            {
                refreshTable();
            }
        }
    }

    public void addNewRow(Registry registry)
    {
        final String titleF = registry.title;
        final String valueF = Float.toString(registry.value);
        final String dateF = registry.date;
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

        TextView textViewTitle = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewTitle.setWidth(TableRowWithContextMenuInfo.LayoutParams.MATCH_PARENT);
        textViewTitle.setText(titleF);
        textViewTitle.setLayoutParams(size2);
        textViewTitle.setTextSize(20);
        tableRow.addView(textViewTitle);

        TextView textViewValue = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewValue.setLayoutParams(size);
        textViewValue.setMaxEms(2);
        textViewValue.setText(valueF);
        tableRow.addView(textViewValue);

        TextView textViewDate = (TextView) getLayoutInflater().inflate(R.layout.text_view_in_table, null);
        textViewDate.setMaxEms(2);
        textViewDate.setText(dateF);
        textViewDate.setLayoutParams(size);
        tableRow.addView(textViewDate);

        if (Float.parseFloat(valueF) < 0f)
            tableRow.setBackgroundResource(R.color.wydatek);
        else
            tableRow.setBackgroundResource(R.color.przychod);

        registerForContextMenu(tableRow);
        tableRow.setOnClickListener(onClickListener);
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        tableRow.setTag(registry.id);
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
        String sqlQuery = "INSERT INTO IncomeOutgo(Title, Value, Date, IncomeOrOutgo) VALUES (" +
                "\"" + title + "\"," +
                "\"" + value + "\"," +
                "\"" + date + "\"," +
                "\"" + incomeOrOutgo + "\")";

        db.execSQL(sqlQuery);

    }

    public void clearRows()
    {
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        int count = tableLayout.getChildCount();
        for (int i = 0; i < count; i++)
        {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRowWithContextMenuInfo)
                ((ViewGroup) child).removeAllViews();
        }
    }

    int titleState = 0;
    int valueState = 0;
    int dateState = 0;

    public void onClickTitle(View view)
    {

        /*String sqlQuery = null;
        if (titleState == 0)
        {
            titleState = 1;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Title ASC";
        } else
        {
            titleState = 0;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Title DESC";
        }

        createDatabaseIfNotExists();

        ArrayList<Registration> registrations = sendQuery(sqlQuery);*/

        if (titleState == 0)
        {
            titleState = 1;
            Collections.sort(registries, new Comparator<Registry>()
            {
                @Override
                public int compare(Registry o1, Registry o2)
                {
                    return o2.title.compareTo(o1.title);

                }
            });
        } else
        {
            titleState = 0;
            Collections.sort(registries, new Comparator<Registry>()
            {
                @Override
                public int compare(Registry o1, Registry o2)
                {
                    return o1.title.compareTo(o2.title);

                }
            });
        }

        clearRows();
        for (Registry x : registries)
            addNewRow(x);
        // for (Registration x : registrations)
        //    addNewRow(x.title, x.value, x.date);
    }

    public void onClickValue(View view)
    {
        /*String sqlQuery = null;
        if (valueState == 0)
        {
            valueState = 1;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Value ASC";
        } else
        {
            valueState = 0;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Value DESC";
        }

        createDatabaseIfNotExists();

        ArrayList<Registration> registrations = sendQuery(sqlQuery);*/
        if (valueState == 0)
        {
            valueState = 1;
            Collections.sort(registries, new Comparator<Registry>()
            {
                @Override
                public int compare(Registry o1, Registry o2)
                {
                    if (o1.value - o2.value > 0)
                        return 1;
                    else if (o1.value - o2.value < 0)
                        return -1;
                    else return 0;

                }
            });
        } else
        {
            valueState = 0;
            Collections.sort(registries, new Comparator<Registry>()
            {
                @Override
                public int compare(Registry o1, Registry o2)
                {
                    if (o1.value - o2.value > 0)
                        return -1;
                    else if (o1.value - o2.value < 0)
                        return 1;
                    else return 0;

                }
            });
        }

        clearRows();
        for (Registry x : registries)
            addNewRow(x);

        // for (Registration x : registrations)
        //addNewRow(x.title, x.value, x.date);
    }

    public void onClickDate(View view)
    {
        /*String sqlQuery = null;
        if (dateState == 0)
        {
            dateState = 1;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Date ASC";
        } else
        {
            dateState = 0;
            sqlQuery = "SELECT * FROM IncomeOutgo ORDER BY Date DESC";
        }

        createDatabaseIfNotExists();

        ArrayList<Registration> registrations = sendQuery(sqlQuery);*/


        if (dateState == 0)
        {
            dateState = 1;
            Collections.sort(registries, new Comparator<Registry>()
            {
                @Override
                public int compare(Registry o1, Registry o2)
                {
                    return o2.date.compareTo(o1.date);

                }
            });
        } else
        {
            dateState = 0;
            Collections.sort(registries, new Comparator<Registry>()
            {
                @Override
                public int compare(Registry o1, Registry o2)
                {
                    return o1.date.compareTo(o2.date);

                }
            });
        }
        clearRows();
        for (Registry x : registries)
            addNewRow(x);
        //for (Registration x : registrations)
        //addNewRow(x.title, x.value, x.date);
    }

    public void onClickFilter(MenuItem item)
    {
        filter_dialog dialog = new filter_dialog();
        dialog.show(getSupportFragmentManager(), "filterDialog");
    }

    public void onClickClearAll(MenuItem item)
    {/*
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
                ArrayList<Registration> registrations = sendQuery("SELECT * FROM IncomeOutgo");
                //showResults(registrations);

                try
                {
                    saveAllDataToFile();
                    //synchData();
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
*/
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

                //String sqlQuery = "SELECT * FROM IncomeOutgo WHERE Title LIKE '%" + s + "%'";
                //ArrayList<Registration> registrations = sendQuery(sqlQuery);
                //showResults(registrations);
                clearRows();
                for (Registry x : registries)
                {
                    if (x.title.matches("(.*)" + s + "(.*)"))
                        addNewRow(x);
                }

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
        final TableRowWithContextMenuInfo tableRow = (TableRowWithContextMenuInfo) menuInfo.targetView;

        tableRowToEditDelete = tableRow;
        TableLayout tableLayout = (TableLayout) tableRow.getParent();

        TextView textViewTitle = (TextView) tableRow.getChildAt(0);
        TextView textViewValue = (TextView) tableRow.getChildAt(1);
        TextView textViewDate = (TextView) tableRow.getChildAt(2);

        String title = textViewTitle.getText().toString();
        String value = textViewValue.getText().toString();

        String date = textViewDate.getText().toString();

        switch (item.getItemId())
        {
            case R.id.editRow:
            {
                Intent i = new Intent(this, add.class);
                // 0 - outgo
                // 1 - income

                if (Double.parseDouble(value) < 0)
                    i.putExtra("IncomeOrOutgo", 0);
                else
                    i.putExtra("IncomeOrOutgo", 1);

                i.putExtra("edit?", "true");
                i.putExtra("language", selectedLanguage);
                i.putExtra("id", tableRow.getTag().toString());

                startActivityForResult(i, REQUEST_CODE_EDIT);

                return true;
            }
            case R.id.deleteRow:
            {
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("users/" + currentFirebaseUser.getUid());

                database.addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        for (DataSnapshot child : dataSnapshot.getChildren())
                        {
                            if (child.getKey().equals(tableRow.getTag().toString()))
                            {
                                child.getRef().removeValue();
                                break;
                            }
                        }
                        refreshTable();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        Toast.makeText(getBaseContext(), "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }

   /*protected void showResults(ArrayList<Registration> registrations)
    {
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
            addNewRow(x.title, x.value, x.date);
            if (x.incomeOrOutgo == 0)
                outgo -= Double.parseDouble(x.value);
            else
                income += Double.parseDouble(x.value);

            balance += Double.parseDouble(x.value);

        }
        textViewBalance.setText(decimalFormat.format(balance).toString());
        textViewIncome.setText(decimalFormat.format(income));
        textViewOutgo.setText(decimalFormat.format(outgo));
    }*/

    public ArrayList<Registration> sendQuery(String sqlQuery)
    {
        createDatabaseIfNotExists();

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

        return registrations;

    }

    public void saveAllDataToFile() throws IOException
    {
        createDatabaseIfNotExists();


        // Loads data from database.
        ArrayList<Registration> registrations = sendQuery("SELECT * FROM IncomeOutgo");

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
        /*final Scanner scanner;
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
                ArrayList<Registration> registrations = sendQuery("SELECT * FROM IncomeOutgo");
                showResults(registrations);


                registrations = null;
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
                    addNewRow(x.title, x.value, x.date);
                    addToDatabase(x.title, x.value, x.date, x.incomeOrOutgo);
                    if (x.incomeOrOutgo == 0)
                        outgo -= Double.parseDouble(x.value);
                    else
                        income += Double.parseDouble(x.value);

                    balance += Double.parseDouble(x.value);
                }
                textViewBalance.setText(decimalFormat.format((balance)));
                textViewIncome.setText(decimalFormat.format((income)));
                textViewOutgo.setText(decimalFormat.format((outgo)));

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
        dialog.show();*/


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
        Log.d("koy", "...");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.do_you_want_to_log_in);
        builder.setMessage(R.string.info_about_logging);
        builder.setPositiveButton(R.string.connect_to_google, new DialogInterface.OnClickListener()
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
        builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Toast.makeText(getBaseContext(), R.string.info_about_logging_from_settings, Toast.LENGTH_LONG).show();
                settings.edit().putString("askForLogin", "no").apply();
                return;
            }
        });


        AlertDialog dialog = builder.create();
        dialog.show();


    }

    public void synchData()
    {
        /*final File localFile = new File(Environment.getExternalStorageDirectory().toString() + "/MySimpleWalletBackup" + FILENAME);

        // Creates backup folder.
        if (!BACKUP_FOLDER.exists())
            BACKUP_FOLDER.mkdirs(); // this will create folder.

        try
        {
            downloadedFile = File.createTempFile("tempFromServer", null, BACKUP_FOLDER);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        // Load data from server.
        serverFileRef = FirebaseStorage.getInstance().getReference();
        localFileRef = serverFileRef.child(currentFirebaseUser.getUid() + FILENAME);
        localFileRef.getFile(downloadedFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
                {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                    {
                        try
                        {
                            Scanner localScanner = null;
                            Date dateLocalFile = null;
                            Date dateServerFile = null;

                            // Get time from server file.
                            Scanner serverScanner = new Scanner(downloadedFile);
                            serverScanner.useDelimiter("\\n");


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

                                ArrayList<Registration> registrations = new ArrayList<Registration>();
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
                                Log.d("pies", dateLocalFile.toString());
                                Log.d("pies", dateServerFile.toString());

                                if (dateServerFile.after(dateLocalFile))
                                {

                                    // Clears all data.
                                    deleteDatabase("Wallet");
                                    textViewBalance.setText("0");
                                    textViewIncome.setText("0");
                                    textViewOutgo.setText("0");
                                    ArrayList<Registration> registrations1 = sendQuery("SELECT * FROM IncomeOutgo");
                                    showResults(registrations1);

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
                                        addNewRow(x.title, x.value, x.date);
                                        addToDatabase(x.title, x.value, x.date, x.incomeOrOutgo);
                                        if (x.incomeOrOutgo == 0)
                                            outgo -= Double.parseDouble(x.value);
                                        else
                                            income += Double.parseDouble(x.value);

                                        balance += Double.parseDouble(x.value);

                                    }
                                    textViewBalance.setText(decimalFormat.format((balance)));
                                    textViewIncome.setText(decimalFormat.format((income)));
                                    textViewOutgo.setText(decimalFormat.format((outgo)));

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

                                    ArrayList<Registration> registrations1 = sendQuery("SELECT * FROM IncomeOutgo");
                                    showResults(registrations1);
                                    sendLocalFileToServer();
                                }
                            } else // If local backup is just created.
                            {
                                // Clears all data.
                                deleteDatabase("Wallet");
                                textViewBalance.setText("0");
                                textViewIncome.setText("0");
                                textViewOutgo.setText("0");
                                ArrayList<Registration> registrations = sendQuery("SELECT * FROM IncomeOutgo");
                                showResults(registrations);

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
                                    addNewRow(x.title, x.value, x.date);
                                    addToDatabase(x.title, x.value, x.date, x.incomeOrOutgo);
                                    if (x.incomeOrOutgo == 0)
                                        outgo -= Double.parseDouble(x.value);
                                    else
                                        income += Double.parseDouble(x.value);

                                    balance += Double.parseDouble(x.value);

                                }
                                textViewBalance.setText(decimalFormat.format((balance)));
                                textViewIncome.setText(decimalFormat.format((income)));
                                textViewOutgo.setText(decimalFormat.format((outgo)));

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
*/
    }

    public void onClickRefresh(MenuItem item)
    {/*
        ArrayList<Registration> registrations = sendQuery("SELECT * FROM IncomeOutgo");
        showResults(registrations);

        // Checks if exists newer version.
        if (currentFirebaseUser != null)
            synchData();

*/
    }

    public void sendLocalFileToServer()
    {
        if (currentFirebaseUser != null)
        {
            // Send backup file on server.
            serverFileRef = FirebaseStorage.getInstance().getReference();
            localFileRef = serverFileRef.child(currentFirebaseUser.getUid() + FILENAME);
            UploadTask uploadTask = localFileRef.putFile(Uri.fromFile(BACKUP_FILEPATH));


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
        }

    }

    private void createDatabaseIfNotExists()
    {
        db = openOrCreateDatabase("Wallet", MODE_PRIVATE, null);
        String sqlDB = "CREATE TABLE IF NOT EXISTS IncomeOutgo (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "Title VARCHAR," +
                "Value DOUBLE NOT NULL, Date DATE," +
                "IncomeOrOutgo INTEGER NOT NULL)";
        db.execSQL(sqlDB);
    }


}

