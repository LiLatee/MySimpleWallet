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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class add extends AppCompatActivity
{
    private EditText editTextTitle, editTextValue, editTextDate;
    private String oldTitle, oldValue, oldDateS;
    private String selectedLanguage = null;

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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Date date = null;
        try {date = calendar.getTime(); } catch (Exception e) {e.printStackTrace();}
        String dataS = formatter.format(date);

        editTextDate.setText(dataS);

        // Do it if edit mode.
        if (getIntent().getExtras().getString("edit?").equals("true"))
        {
            oldTitle = getIntent().getExtras().getString("title");
            oldValue = getIntent().getExtras().getString("value");
            oldDateS = getIntent().getExtras().getString("date");

            editTextTitle.setText(oldTitle);
            editTextValue.setText(oldValue);
            editTextDate.setText(oldDateS);
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
                EditText textViewDate = (EditText) findViewById(R.id.editTextDateAdd);
                textViewDate.setText((year) + "/" + monthS + "/" + dayS);
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

        editTextDate.setText(year + "/" + monthS + "/" + dayS);
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

        int incomeOrOutgo = getIntent().getExtras().getInt("IncomeOrOutgo");
        Intent i = new Intent();

        if (getIntent().getExtras().getString("edit?").equals("true"))
        {
            i.putExtra("edit?", "true");
            i.putExtra("oldTitle", oldTitle);
            if ( incomeOrOutgo == 0)
                i.putExtra( "oldValue", "-" + oldValue);
            else
                i.putExtra("oldValue", oldValue);
            i.putExtra("oldDate", oldDateS);
        }
        else
            i.putExtra("edit?", "false");

        i.putExtra("title", editTextTitle.getText().toString());
        if ( incomeOrOutgo == 0) i.putExtra( "value", "-" + editTextValue.getText().toString());
        else i.putExtra("value", editTextValue.getText().toString());
        i.putExtra("date", editTextDate.getText().toString());
        i.putExtra("incomeOrOutgo", incomeOrOutgo);
        setResult(RESULT_OK, i);
        finish();
    }

    public void onClickCancel(View view)
    {
        setResult(RESULT_CANCELED);
        finish();
    }

}
