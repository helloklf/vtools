package com.omarea.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.omarea.vboot.R;

import java.util.ArrayList;
import java.util.HashMap;

public class swaplist_adapter extends BaseAdapter {

    private Context context;
    private ArrayList<HashMap<String, String>> list;

    public swaplist_adapter(Context context, ArrayList<HashMap<String, String>> list) {
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
            viewHolder = new ViewHolder();
            convertView = View.inflate(context, R.layout.swap_item, null);
            viewHolder.itemPath = (TextView) convertView.findViewById(R.id.itemPath);
            viewHolder.itemType = (TextView) convertView.findViewById(R.id.itemType);
            viewHolder.itemSize = (TextView) convertView.findViewById(R.id.itemSize);
            viewHolder.itemUsed = (TextView) convertView.findViewById(R.id.itemUsed);
            viewHolder.itemPriority = (TextView) convertView.findViewById(R.id.itemPriority);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.itemPath.setText(getItem(position).get("path").toString());
        viewHolder.itemType.setText(getItem(position).get("type").toString());
        viewHolder.itemSize.setText(getItem(position).get("size").toString());
        viewHolder.itemUsed.setText(getItem(position).get("used").toString());
        viewHolder.itemPriority.setText(getItem(position).get("priority").toString());

        return convertView;
    }

    private ViewHolder viewHolder;

    public class ViewHolder {
        public TextView itemPath;
        public TextView itemType;
        public TextView itemSize;
        public TextView itemUsed;
        public TextView itemPriority;
    }
}