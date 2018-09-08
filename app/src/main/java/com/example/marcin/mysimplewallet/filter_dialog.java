package com.example.marcin.mysimplewallet;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;

public class filter_dialog extends DialogFragment
{
    private ImageButton imageButtonFrom, imageButtonTo;
    private TextView textViewOk, textViewCancel, textViewRemoveFilter;
    private RadioGroup radioGroup;
    private EditText ediTextFrom, editTextTo;
    private View dialogView;
    private SQLiteDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        dialogView = inflater.inflate(R.layout.filter, container, false);
        radioGroup = (RadioGroup) dialogView.findViewById(R.id.radioGroup);
        setDefaultDate();


        imageButtonFrom = (ImageButton) dialogView.findViewById(R.id.imageButtonFrom);
        imageButtonTo = (ImageButton) dialogView.findViewById(R.id.imageButtonTo);

        imageButtonFrom.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onClickCalendar(v);
            }
        });
        imageButtonTo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onClickCalendar(v);
            }
        });

        // Sets actions to switch selected RadioButton
        radioGroup = (RadioGroup) dialogView.findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                RadioButton selectedButton = (RadioButton) dialogView.findViewById(checkedId);
                imageButtonFrom = (ImageButton) dialogView.findViewById(R.id.imageButtonFrom);
                imageButtonTo = (ImageButton) dialogView.findViewById(R.id.imageButtonTo);

                if (selectedButton.getTag().equals("byDate"))
                {
                    setDefaultDate();
                    imageButtonFrom.setVisibility(View.VISIBLE);
                    imageButtonTo.setVisibility(View.VISIBLE);
                } else
                {
                    EditText ediTextFrom = (EditText) dialogView.findViewById(R.id.editTextFrom);
                    ediTextFrom.setText(null);

                    EditText ediTextTo = (EditText) dialogView.findViewById(R.id.editTextTo);
                    ediTextTo.setText(null);

                    imageButtonFrom.setVisibility(View.GONE);
                    imageButtonTo.setVisibility(View.GONE);
                }
            }
        });


        // Sets actions to buttons.
        textViewOk = (TextView) dialogView.findViewById(R.id.textViewOk);
        textViewOk.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Creates database if not exists.
                db = getActivity().openOrCreateDatabase("Wallet", MODE_PRIVATE, null);

                String sqlDB = "CREATE TABLE IF NOT EXISTS IncomeOutgo (" +
                        "Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "Title VARCHAR," +
                        "Value DOUBLE NOT NULL, " +
                        "Date DATE," +
                        "IncomeOrOutgo INTEGER NOT NULL)";
                db.execSQL(sqlDB);

                //
                RadioButton selectedButton = (RadioButton) dialogView.findViewById(radioGroup.getCheckedRadioButtonId());
                String sqlQuery = null;
                String value1 = ediTextFrom.getText().toString();
                String value2 = editTextTo.getText().toString();
                if (value1.isEmpty() || value2.isEmpty())
                {
                    Toast.makeText(getContext(), "Wartości nie moga być puste!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedButton.getTag().equals("byDate"))
                {
                    String regex = "^[0-9]{4}/(0[1-9]|1[0-2])/([0-2][0-9]|3[0-1])$";
                    if (!Pattern.matches(regex, value1) || !Pattern.matches(regex, value2))
                    {
                        Toast.makeText(getContext(), "Data musi być w formacie YYYY/MM/DD lub podano nieprawidłową wartość.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Date firstDate = new Date(value1);
                    Date secondDate = new Date(value2);
                    if(firstDate.after(secondDate))
                    {
                        Toast.makeText(getContext(), "Pierwsza data musi być przed drugą!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    sqlQuery = "SELECT Id, Title, Value, Date, IncomeOrOutgo FROM IncomeOutgo WHERE Date BETWEEN '" + value1 + "' AND '" + value2 + "'";
                }
                else
                {

                    String regex = "^(0|([1-9][0-9]*))(\\.[0-9]+)?$";
                    if (!Pattern.matches(regex, value1) || !Pattern.matches(regex, value2))
                    {
                        Toast.makeText(getContext(), "Dozwolone znaki to (0-9) oraz kropka (.).", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (Double.parseDouble(value1) > Double.parseDouble(value2))
                    {
                        Toast.makeText(getContext(), "Pierwsza wartość musi być mniejsza niż druga!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sqlQuery = "SELECT Id, Title, Value, Date, IncomeOrOutgo FROM IncomeOutgo WHERE " + "ABS(Value) BETWEEN " + value1 + " AND " + value2;
                }


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
                TableLayout tableLayout = (TableLayout) ((MainActivity)getActivity()).findViewById(R.id.tableLayout);
                int count = tableLayout.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = tableLayout.getChildAt(i);
                    if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
                }

                ((MainActivity)getActivity()).addHeaderRow();

                for ( Registration x : registrations)
                    ((MainActivity)getActivity()).addNewRow(x.title, x.value, x.date, x.incomeOrOutgo);

                getDialog().dismiss();
            }
        });
        textViewCancel = (TextView) dialogView.findViewById(R.id.textViewCancel);
        textViewCancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                getDialog().dismiss();
            }
        });
        textViewRemoveFilter = (TextView) dialogView.findViewById(R.id.textViewRemoveFilter);
        textViewRemoveFilter.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ((MainActivity)getActivity()).loadData();
                getDialog().dismiss();
            }
        });


        return dialogView;
    }


    public void onClickCalendar(View view)
    {
        final View calendarView = view;
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth)
            {
                String dayS = Integer.toString(dayOfMonth);
                String monthS = Integer.toString(month + 1);

                if (month + 1 < 10)
                    monthS = "0" + monthS;
                if (dayOfMonth < 10)
                    dayS = "0" + dayS;

                if (calendarView.getTag().toString().equals("from"))
                {
                    EditText EditTextFrom = (EditText) dialogView.findViewById(R.id.editTextFrom);
                    EditTextFrom.setText((year) + "/" + monthS + "/" + dayS);
                } else
                {
                    EditText EditTextTo = (EditText) dialogView.findViewById(R.id.editTextTo);
                    EditTextTo.setText((year) + "/" + monthS + "/" + dayS);
                }
            }
        };

        Calendar calendar = Calendar.getInstance();

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        DatePickerDialog datePickerDialog = new DatePickerDialog(dialogView.getContext(), 0, listener, year, month, day);
        datePickerDialog.show();

        String dayS = Integer.toString(datePickerDialog.getDatePicker().getDayOfMonth());
        String monthS = Integer.toString(datePickerDialog.getDatePicker().getMonth() + 1);
        year = datePickerDialog.getDatePicker().getYear();

        if (month + 1 < 10)
            monthS = "0" + monthS;
        if (day < 10)
            dayS = "0" + dayS;


        if (calendarView.getTag().toString().equals("from"))
        {
            EditText EditTextFrom = (EditText) dialogView.findViewById(R.id.editTextFrom);
            EditTextFrom.setText((year) + "/" + monthS + "/" + dayS);
        } else
        {
            EditText EditTextTo = (EditText) dialogView.findViewById(R.id.editTextTo);
            EditTextTo.setText((year) + "/" + monthS + "/" + dayS);
        }
    }

    public void setDefaultDate()
    {
        // Set default data.
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        String dayS = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
        String monthS = Integer.toString(calendar.get(Calendar.MONTH) + 1);

        if (month < 10)
            monthS = "0" + monthS;
        if (day < 10)
            dayS = "0" + dayS;

        ediTextFrom = (EditText) dialogView.findViewById(R.id.editTextFrom);
        ediTextFrom.setText(year + "/" + monthS + "/01");

        editTextTo = (EditText) dialogView.findViewById(R.id.editTextTo);
        editTextTo.setText(year + "/" + monthS + "/" + dayS);
    }

}
