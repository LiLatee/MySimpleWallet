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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        Date date = null;
        try {date = calendar.getTime(); } catch (Exception e) {e.printStackTrace();}
        String data = formatter.format(date);

        EditText textViewDate = (EditText) findViewById(R.id.editTextDataDodaj);
        textViewDate.setText(data);

    }

    public void onClickCalendar(View view)
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


        EditText textViewDate = (EditText) findViewById(R.id.editTextDataDodaj);
        textViewDate.setText(year + "/" + monthS + "/" + dayS);
    }

    public void onClickDodaj(View view)
    {
        EditText editTextTytul = (EditText) findViewById(R.id.editTextTytulDodaj);
        EditText editTextKwota = (EditText) findViewById(R.id.editTextKwotaDodaj);
        EditText editTextData = (EditText) findViewById(R.id.editTextDataDodaj);

        if (editTextKwota.getText().toString().isEmpty() || Double.parseDouble(editTextKwota.getText().toString()) == 0.0)
        {
            Toast.makeText(this,"Kwota nie może być zerowa!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editTextTytul.getText().toString().isEmpty())
            editTextTytul.setText("-");
        if (editTextData.getText().toString().isEmpty())
            editTextData.setText("-");

        int incomeOrOutgo = getIntent().getExtras().getInt("IncomeOrOutgo");
        Intent i = new Intent();
        i.putExtra("tytul", editTextTytul.getText().toString());
        if ( incomeOrOutgo == 0) i.putExtra( "kwota", "-" + editTextKwota.getText().toString());
        else i.putExtra("kwota", editTextKwota.getText().toString());
        i.putExtra("data", editTextData.getText().toString());
        setResult(RESULT_OK, i);
        finish();
    }

    public void onClickAnuluj(View view)
    {
        setResult(RESULT_CANCELED);
        finish();
    }

}
