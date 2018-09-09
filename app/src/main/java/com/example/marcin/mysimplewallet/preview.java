package com.example.marcin.mysimplewallet;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class preview extends AppCompatActivity
{
    private TextView textViewTitle, textViewValue, textViewDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        String title = getIntent().getExtras().getString("title");
        String value = getIntent().getExtras().getString("value");
        String date = getIntent().getExtras().getString("date");

        textViewTitle = (TextView) findViewById(R.id.textViewTytulDodaj) ;
        textViewValue = (TextView) findViewById(R.id.textViewKwotaDodaj) ;
        textViewDate = (TextView) findViewById(R.id.textViewDataDodaj) ;

        textViewTitle.setText(title);
        textViewValue.setText(value);
        textViewDate.setText(date);
    }

    public void onClickBack(View view)
    {
        setResult(RESULT_CANCELED);
        finish();
    }
}
