package com.omarea.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.omarea.vtools.R;

public class AdapterString extends BaseAdapter {
    private String[] arr;
    private Context context;

    public AdapterString(Context context, String[] arr) {
        this.arr = arr;
        this.context = context;
    }

    @Override
    public int getCount() {
        return arr.length;
    }

    @Override
    public Object getItem(int position) {
        return arr[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.list_item_text, null);
        }
        String text = getItem(position).toString();
        ((TextView) convertView).setText(text);
        return convertView;
    }
}
