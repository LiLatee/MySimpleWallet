package com.example.marcin.mysimplewallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class preview extends AppCompatActivity
{
    private TextView textViewTitle, textViewValue, textViewDate;
    private String selectedLanguage = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // Language settings.
        selectedLanguage = getIntent().getExtras().getString("language");

        Locale locale = new Locale(selectedLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());


        String title = getIntent().getExtras().getString("title");
        String value = getIntent().getExtras().getString("value");
        String date = getIntent().getExtras().getString("date");

        textViewTitle = (TextView) findViewById(R.id.textViewTitleAdd) ;
        textViewValue = (TextView) findViewById(R.id.textViewValueAdd) ;
        textViewDate = (TextView) findViewById(R.id.textViewDateAdd) ;

        textViewTitle.setText(title);
        textViewValue.setText(value);
        textViewDate.setText(date);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Language settings.
        selectedLanguage = getIntent().getExtras().getString("language");

        Locale locale = new Locale(selectedLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    public void onClickBack(View view)
    {
        setResult(RESULT_CANCELED);
        finish();
    }
}
