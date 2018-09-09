package com.example.marcin.mysimplewallet;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.strictmode.CleartextNetworkViolation;
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

public class dodaj extends AppCompatActivity
{
    EditText editTextTitle, editTextValue, editTextDate;
    String oldTitle, oldValue, oldDateS;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj);

        editTextTitle = (EditText) findViewById(R.id.editTextTytulDodaj);
        editTextValue = (EditText) findViewById(R.id.editTextKwotaDodaj);
        editTextDate = (EditText) findViewById(R.id.editTextDataDodaj);


        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Date date = null;
        try {date = calendar.getTime(); } catch (Exception e) {e.printStackTrace();}
        String data = formatter.format(date);

        editTextDate.setText(data);

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

    public void onClickCalendarDodaj(View view)
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
                EditText textViewDate = (EditText) findViewById(R.id.editTextDataDodaj);
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

    public void onClickDodaj(View view)
    {

        if (editTextValue.getText().toString().isEmpty() || Double.parseDouble(editTextValue.getText().toString()) == 0.0)
        {
            Toast.makeText(this,"Kwota nie może być zerowa!", Toast.LENGTH_SHORT).show();
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

        i.putExtra("tytul", editTextTitle.getText().toString());
        if ( incomeOrOutgo == 0) i.putExtra( "kwota", "-" + editTextValue.getText().toString());
        else i.putExtra("kwota", editTextValue.getText().toString());
        i.putExtra("data", editTextDate.getText().toString());
        i.putExtra("incomeOrOutgo", incomeOrOutgo);
        setResult(RESULT_OK, i);
        finish();
    }

    public void onClickAnuluj(View view)
    {
        setResult(RESULT_CANCELED);
        finish();
    }

}
