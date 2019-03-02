package com.example.marcin.mysimplewallet;

import java.util.Date;


public class Registry
{
    String id;
    String title;
    float value;
    String date;

    public Registry(){}

    public Registry(String id, String title, float value, String date)
    {
        this.id = id;
        this.title = title;
        this.value = value;
        this.date = date;
    }


}
