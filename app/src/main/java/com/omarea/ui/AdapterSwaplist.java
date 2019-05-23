package com.omarea.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.omarea.vtools.R;

import java.util.ArrayList;
import java.util.HashMap;

public class AdapterSwaplist extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String, String>> list;

    public AdapterSwaplist(Context context, ArrayList<HashMap<String, String>> list) {
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
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = View.inflate(context, R.layout.list_item_swap, null);
            viewHolder.itemPath = convertView.findViewById(R.id.itemPath);
            viewHolder.itemType = convertView.findViewById(R.id.itemType);
            viewHolder.itemSize = convertView.findViewById(R.id.itemSize);
            viewHolder.itemUsed = convertView.findViewById(R.id.itemUsed);
            viewHolder.itemPriority = convertView.findViewById(R.id.itemPriority);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.itemPath.setText(getItem(position).get("path"));
        viewHolder.itemType.setText(getItem(position).get("type"));
        viewHolder.itemSize.setText(getItem(position).get("size"));
        viewHolder.itemUsed.setText(getItem(position).get("used"));
        viewHolder.itemPriority.setText(getItem(position).get("priority"));

        return convertView;
    }

    public class ViewHolder {
        TextView itemPath;
        TextView itemType;
        TextView itemSize;
        TextView itemUsed;
        TextView itemPriority;
    }
}