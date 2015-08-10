package com.handhandlab.agentdroid.system;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * AsyncTask to run init script
 * Created by Handhand on 2015/7/21.
 */
public class InitAsyncTask extends AsyncTask<String,String,Boolean>{

    Context context;

    public InitAsyncTask(Context context){
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(String[] params) {
        try{
            InitScript.systemInit(context);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean o) {
        super.onPostExecute(o);
        if(o){
            Toast.makeText(context,"Init Successfully!",Toast.LENGTH_LONG).show();
        }
    }
}
