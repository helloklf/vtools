package com.omarea.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.omarea.vboot.R;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Hello on 2017/11/21.
 */

public class task_adapter extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String, String>> list;

    public task_adapter(Context context, ArrayList<HashMap<String, String>> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public HashMap<String, String> getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            viewHolder = new task_adapter.ViewHolder();
            convertView = View.inflate(context, R.layout.task_item, null);
            viewHolder.itemPid = (TextView) convertView.findViewById(R.id.itemPid);
            viewHolder.itemType = (TextView) convertView.findViewById(R.id.itemType);
            viewHolder.itemCpu = (TextView) convertView.findViewById(R.id.itemCpu);
            viewHolder.itemName = (TextView) convertView.findViewById(R.id.itemName);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (task_adapter.ViewHolder) convertView.getTag();
        }

        viewHolder.itemPid.setText(getItem(position).get("itemPid").toString());
        viewHolder.itemType.setText(getItem(position).get("itemType").toString());
        viewHolder.itemCpu.setText(getItem(position).get("itemCpu").toString());
        viewHolder.itemName.setText(getItem(position).get("itemName").toString());

        return convertView;
    }

    private task_adapter.ViewHolder viewHolder;

    public class ViewHolder {
        public TextView itemPid;
        public TextView itemType;
        public TextView itemCpu;
        public TextView itemName;
    }
}