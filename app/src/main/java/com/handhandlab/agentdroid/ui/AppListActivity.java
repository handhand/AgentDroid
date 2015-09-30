package com.handhandlab.agentdroid.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.handhandlab.agentdroid.R;

import java.util.List;

/**
 * Created by Handhand on 2015/6/29.
 */
public class AppListActivity extends Activity {
    ListView mLvApps;
    AppListAdapter mAdapter;
    List<ApplicationInfo> packages;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mLvApps = (ListView)findViewById(R.id.app_list);
        mAdapter = new AppListAdapter();
        mLvApps.setAdapter(mAdapter);
        new GetInstalledAppTask().execute(new String[]{});
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * task to get all app installed
     */
    class GetInstalledAppTask extends AsyncTask<String,String,String>{
        @Override
        protected String doInBackground(String... params) {
            PackageManager pm = getPackageManager();
            packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * adapter
     */
    class AppListAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return packages==null?0:packages.size();
        }

        @Override
        public Object getItem(int position) {
            return packages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView = LayoutInflater.from(AppListActivity.this).inflate(R.layout.item_app,parent,false);
                ViewHolder vh = new ViewHolder();
                vh.icon = (ImageView)convertView.findViewById(R.id.app_icon);
                vh.name = (TextView)convertView.findViewById(R.id.app_name);
                vh.selected = (CheckBox)convertView.findViewById(R.id.app_selected);
                vh.selected.setOnCheckedChangeListener(vh);
                convertView.setTag(vh);
            }
            ApplicationInfo info = packages.get(position);
            ViewHolder vh = (ViewHolder)convertView.getTag();
            vh.info = info;
            Drawable d = getPackageManager().getApplicationIcon(info);
            String label = getPackageManager().getApplicationLabel(info).toString();
            vh.icon.setImageDrawable(d);
            vh.name.setText(label);
            //set checked
            SharedPreferences pref = getApplicationContext().getSharedPreferences("proxied",0);
            if(pref.getInt(info.packageName,-1)==-1){
                vh.selected.setChecked(false);
            }else{
                vh.selected.setChecked(true);
            }
            return convertView;
        }

        public class ViewHolder implements CompoundButton.OnCheckedChangeListener{
            ImageView icon;
            TextView name;
            CheckBox selected;
            ApplicationInfo info;

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    SharedPreferences pref = getApplicationContext().getSharedPreferences("proxied",0);
                    pref.edit().putInt(info.packageName,info.uid).commit();
                }else{
                    SharedPreferences pref = getApplicationContext().getSharedPreferences("proxied",0);
                    pref.edit().remove(info.packageName).commit();
                }
            }
        }
    }
}
