package com.handhandlab.agentdroid.system;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.handhandlab.agentdroid.utils.DataUtils;

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
    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if(isSuccess){
            DataUtils.setHasInit(context);
            Toast.makeText(context,"Init Successfully!",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(context,"Init Failed!!",Toast.LENGTH_LONG).show();
        }
    }
}
