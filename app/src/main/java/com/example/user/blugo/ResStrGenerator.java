package com.example.user.blugo;

import android.content.Context;

/**
 * Created by user on 2016-07-06.
 */
public class ResStrGenerator {
    private static ResStrGenerator instance;

    private Context app_context;

    private ResStrGenerator()
    {
        app_context = App.getAppContext();
    }

    public static synchronized ResStrGenerator getInstance()
    {
        if (instance == null) {
            instance = new ResStrGenerator();
        }
        return instance;
    }

    public String get_res_string(int resId)
    {
        return app_context.getString(resId);
    }
}
