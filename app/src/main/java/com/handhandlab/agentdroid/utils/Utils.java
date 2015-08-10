package com.handhandlab.agentdroid.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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


}
