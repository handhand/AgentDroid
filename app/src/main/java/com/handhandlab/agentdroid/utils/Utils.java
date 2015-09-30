package com.handhandlab.agentdroid.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class Utils {

    /**
     * copy file from asset to app private directory (/data/data/com.handhandlab.agentdroid/files)
     * @param fileName
     */
    public static void copyFile(Context context,String fileName) throws IOException{
        InputStream dbInput = context.getAssets().open(fileName);
        File outFile = new File(context.getFilesDir(), fileName);
        OutputStream output = new FileOutputStream(outFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = dbInput.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        output.flush();
        output.close();
        dbInput.close();
    }

    /**
     * get millis secs of one day
     * @return
     */
    public static long getOneDay(){
    	return 1000 * 60 * 60 * 24;
    }

    /**
     * check a given string is in url form
     * @param str
     * @return
     */
    public static boolean isUrl(String str){
        if(str==null || str.trim().equals(""))return false;
        try{
            new URL(str);
        }catch (MalformedURLException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public static int getUID(Context context){
        PackageManager pm = context.getPackageManager();
        try{
            ApplicationInfo info = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return info.uid;
        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return 0;
    }
}
