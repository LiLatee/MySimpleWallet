package com.example.marcin.mysimplewallet;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class add extends AppCompatActivity
{
    private EditText editTextTitle, editTextValue, editTextDate;
    private String selectedLanguage = null;
    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        // Language settings.
        selectedLanguage = getIntent().getExtras().getString("language");

        if (!selectedLanguage.equals("-"))
        {
            Locale locale = new Locale(selectedLanguage);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
        else
            selectedLanguage = Locale.getDefault().toLanguageTag();

        editTextTitle = (EditText) findViewById(R.id.editTextTitleAdd);
        editTextValue = (EditText) findViewById(R.id.editTextValueAdd);
        editTextDate = (EditText) findViewById(R.id.editTextDateAdd);


        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
        Date date = null;
        try {date = calendar.getTime(); } catch (Exception e) {e.printStackTrace();}
        String dataS = formatter.format(date);

        editTextDate.setText(dataS);
        


        // if edit mode.
        if (getIntent().getExtras().getString("edit?").equals("true"))
        {
            id = getIntent().getExtras().getString("id");
            Registry registry = MainActivity.localDB.getRegistryById(id);

            editTextTitle.setText(registry.title);
            editTextValue.setText(Float.toString(Math.abs(registry.value)));
            editTextDate.setText(registry.date);
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Language settings.
        selectedLanguage = getIntent().getExtras().getString("language");

        if (!selectedLanguage.equals("-"))
        {
            Locale locale = new Locale(selectedLanguage);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
        else
            selectedLanguage = Locale.getDefault().toLanguageTag();;

    }

    public void onClickCalendarAdd(View view)
    {
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth)
            {
                String dayS = Integer.toString(dayOfMonth);
                String monthS = Integer.toString(month + 1);

                if (month + 1 < 10)
                    monthS = "0" + monthS;
                if (dayOfMonth  < 10)
                    dayS = "0" + dayS;
                EditText editTextDate = (EditText) findViewById(R.id.editTextDateAdd);
                editTextDate.setText(dayS + "/" + monthS + "/" + (year-2000));
            }
        };

        Calendar calendar = Calendar.getInstance();

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, 0, listener,year, month, day );
        datePickerDialog.show();

        String dayS = Integer.toString(datePickerDialog.getDatePicker().getDayOfMonth());
        String monthS = Integer.toString(datePickerDialog.getDatePicker().getMonth() + 1);
        year = datePickerDialog.getDatePicker().getYear();

        if (month + 1 < 10)
            monthS = "0" + monthS;
        if (day  < 10)
            dayS = "0" + dayS;

        editTextDate.setText(dayS + "/" + monthS + "/" + (year-2000));

    }

    public void onClickAdd(View view)
    {

        if (editTextValue.getText().toString().isEmpty() || Double.parseDouble(editTextValue.getText().toString()) == 0.0)
        {
            Toast.makeText(this, getString(R.string.info_empty_value), Toast.LENGTH_SHORT).show();
            return;
        }
        if (editTextTitle.getText().toString().isEmpty())
            editTextTitle.setText("-");
        if (editTextDate.getText().toString().isEmpty())
            editTextDate.setText("-");



        String id;
        if (getIntent().getExtras().getString("edit?").equals("true"))
            id = getIntent().getExtras().getString("id");
        else
            id = MainActivity.remoteDB.generateKey();
        String title = editTextTitle.getText().toString();
        float value;
        String date = editTextDate.getText().toString();
        int incomeOrOutgo = getIntent().getExtras().getInt("IncomeOrOutgo");
        if ( incomeOrOutgo == 0) value = -1 * Float.parseFloat(editTextValue.getText().toString());
        else value = Float.parseFloat(editTextValue.getText().toString());

        ConnectivityManager cm = (ConnectivityManager)getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        Registry registryToAdd = new Registry(id, title, value, date, Long.toString(new Date().getTime()));
        if (isConnected)
        {
            //database.child(id).setValue(registryToAdd);
            MainActivity.remoteDB.addRegistry(registryToAdd);
            if (getIntent().getExtras().getString("edit?").equals("true"))
                MainActivity.localDB.editRegistry(registryToAdd);
            else
                MainActivity.localDB.addRegistry(registryToAdd);
        }
        else
        {
            if (getIntent().getExtras().getString("edit?").equals("true"))
                MainActivity.localDB.editRegistry(registryToAdd);
            else
                MainActivity.localDB.addRegistry(registryToAdd);
        }



        Intent i = new Intent();
        setResult(RESULT_OK, i);
        finish();
    }

    public void onClickCancel(View view)
    {
        setResult(RESULT_CANCELED);
        finish();
    }

}
