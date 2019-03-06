package com.example.marcin.mysimplewallet;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
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
    private Registry editRegistry;
    private DatabaseReference database;
    private FirebaseUser currentFirebaseUser;

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

        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance().getReference("users/" + currentFirebaseUser.getUid());

        // if edit mode.
        if (getIntent().getExtras().getString("edit?").equals("true"))
        {
            id = getIntent().getExtras().getString("id");

            // search for proper registry by id
            database.addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    for (DataSnapshot child : dataSnapshot.getChildren())
                    {
                        if (child.getKey().equals(id))
                        {
                            editRegistry = child.getValue(Registry.class);
                            break;
                        }
                    }

                    editTextTitle.setText(editRegistry.title);
                    editTextValue.setText(Float.toString(Math.abs(editRegistry.value)));
                    editTextDate.setText(editRegistry.date);
                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {
                    Toast.makeText(getBaseContext(), "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
                }
            });


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
            id = database.push().getKey();
        String title = editTextTitle.getText().toString();
        float value;
        String date = editTextDate.getText().toString();
        int incomeOrOutgo = getIntent().getExtras().getInt("IncomeOrOutgo");
        if ( incomeOrOutgo == 0) value = -1 * Float.parseFloat(editTextValue.getText().toString());
        else value = Float.parseFloat(editTextValue.getText().toString());

        Registry registryToAdd = new Registry(id, title, value, date, Long.toString(new Date().getTime()));
        database.child(id).setValue(registryToAdd);
        MainActivity.addToDatabase(registryToAdd);
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
