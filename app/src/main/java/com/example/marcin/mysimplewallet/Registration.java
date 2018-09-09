package com.example.marcin.mysimplewallet;


public class Registration
{
    int id;
    String title;
    String value;
    String date;
    // 0 - outgo
    // 1 - income
    int incomeOrOutgo;

    public Registration(int id, String title, String value, String date, int incomeOrOutgo)
    {
        this.id = id;
        this.title = title;
        this.value = value;
        this.date = date;
        this.incomeOrOutgo = incomeOrOutgo;
    }


}