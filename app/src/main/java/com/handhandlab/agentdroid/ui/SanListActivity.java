package com.handhandlab.agentdroid.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.handhandlab.agentdroid.R;
import com.handhandlab.agentdroid.utils.DataUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Handhand on 2015/7/15.
 */
public class SanListActivity extends Activity implements View.OnClickListener{
    ListView mListViewNames;
    List<String> mSanList = new ArrayList<>();
    SanListAdapter mAdapter;
    EditText mEditName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_san_names);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.add_name).setOnClickListener(this);
        mEditName = (EditText)findViewById(R.id.san_name);
        mListViewNames = (ListView)findViewById(R.id.san_list);
        //load data
        String[] nameArray = DataUtils.getSanNames(this);
        for(String san:nameArray){
            mSanList.add(san);
        }
        //show data
        mAdapter = new SanListAdapter();
        mListViewNames.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        String san = mEditName.getText().toString();
        if(TextUtils.isEmpty(san)==false && isSet(san)==false){
            mSanList.add(san);
            mEditName.setText("");
            //add to persistence
            DataUtils.addSanName(getApplicationContext(),san);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * check if the san name already in dataset
     * @param san
     * @return
     */
    private boolean isSet(String san){
        for(String str: mSanList){
            if(TextUtils.equals(str,san)){
                return true;
            }
        }
        return false;
    }

    /**
     * adapter
     */
    class SanListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mSanList ==null?0: mSanList.size();
        }

        @Override
        public String getItem(int position) {
            return mSanList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_san,parent,false);
                ViewHolder vh = new ViewHolder();
                vh.tvName = (TextView)convertView.findViewById(R.id.san_name);
                convertView.setTag(vh);
            }
            String san = getItem(position);
            ViewHolder vh = (ViewHolder)convertView.getTag();
            vh.tvName.setText(san);
            return convertView;
        }

        public class ViewHolder{
            TextView tvName;
        }
    }
}
