package com.handhandlab.agentdroid.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.net.HttpCookie;
import java.net.URI;
import java.util.Map;

/**
 * Utility to store things
 * Created by Handhand on 2015/7/15.
 */
public class DataUtils {
    private static final String PREF = "SAN";
    private static final String PREF_PROX = "proxied";
    private static final String PREF_INIT = "init";
    private static final String KEY = "san_key";
    private static final String DIVIDER = "!2@#";

    /**
     * store the Subject Alternative Names in SharedPreferences
     * in the format of "name1!2@#name2!2@#...name3", where "!2@#" as the divider
     * @param context
     * @param name subject alternative names to be stored
     */
    public static final void addSanName(Context context,String name){
        if(TextUtils.isEmpty(name))return;
        SharedPreferences prefs = context.getSharedPreferences(PREF,0);
        /*String names = prefs.getString(KEY,"");
        names = names.equals("")?name:names + DIVIDER + name;
        prefs.edit().putString(KEY,names).commit();*/
        prefs.edit().putString(name,"").commit();
    }

    /**
     * @param context
     * @return get Subject Alternative Names from persistent storage,if none available, return a size 0 array
     */
    public static final String[] getSanNames(Context context){
        /*SharedPreferences prefs = context.getSharedPreferences(PREF,0);
        String names = prefs.getString(KEY,"");
        if(TextUtils.isEmpty(names))return new String[0];
        return  names.split(DIVIDER,-1);*/

        SharedPreferences prefs = context.getSharedPreferences(PREF,0);
        Map<String,?> cookieMap = prefs.getAll();
        String[] sans = new String[cookieMap.size()];
        int i = 0;
        for(Map.Entry<String,?> entry: cookieMap.entrySet()){
            String name = entry.getKey();
            sans[i++] = name;
        }
        return sans;
    }

    public static void clear(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREF,0);
        prefs.edit().clear().commit();
    }

    /**
     * get the uids of the apps that need proxy.
     *
     * @param context
     * @return
     */
    public static final Map<String,?> getProxiedApps(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREF_PROX, 0);
        return prefs.getAll();
    }

    public static void setHasInit(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_INIT,true).commit();
    }

    public static boolean isInit(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_INIT,false);
    }
}
