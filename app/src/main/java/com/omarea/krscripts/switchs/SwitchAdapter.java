package com.omarea.krscripts.switchs;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.omarea.krscripts.ScriptEnvironmen;
import com.omarea.vtools.R;

import java.util.ArrayList;

public class SwitchAdapter implements ListAdapter {
    private ArrayList<SwitchInfo> actionInfos;
    private Context context;

    public SwitchAdapter(ArrayList<SwitchInfo> actionInfos) {
        this.actionInfos = actionInfos;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return actionInfos != null ? actionInfos.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return actionInfos != null ? actionInfos.get(position) : null;
    }

    public void update(int index, ListView listview) {
        int visiblePosition = listview.getFirstVisiblePosition();
        View view = listview.getChildAt(index - visiblePosition);
        ViewHolder holder = (ViewHolder) view.getTag();
        SwitchInfo actionInfo = ((SwitchInfo) getItem(index));
        if (actionInfo.descPollingShell != null && !actionInfo.descPollingShell.isEmpty()) {
            actionInfo.desc = ScriptEnvironmen.executeResultRoot(listview.getContext(), actionInfo.descPollingShell);
        }
        if (actionInfo.getState != null && !actionInfo.getState.isEmpty()) {
            String shellResult = ScriptEnvironmen.executeResultRoot(listview.getContext(), actionInfo.getState);
            actionInfo.selected = shellResult != null && (shellResult.equals("1") || shellResult.toLowerCase().equals("true"));
        }
        holder.itemSwitch.setChecked(actionInfo.selected);
        holder.itemText.setText(actionInfo.desc);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        SwitchInfo item = (SwitchInfo) getItem(position);

        View convertView = view;
        ViewHolder viewHolder;

        if (context == null) {
            context = parent.getContext();
        }

        try {
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = View.inflate(context, R.layout.switch_row_item, null);
                viewHolder.itemSwitch = convertView.findViewById(R.id.Title);
                viewHolder.itemText = convertView.findViewById(R.id.Desc);
                viewHolder.itemSeparator = convertView.findViewById(R.id.Separator);
                viewHolder.contents = convertView.findViewById(R.id.contents);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            if (isNullOrEmpty(item.desc) && isNullOrEmpty(item.title)) {
                viewHolder.contents.setVisibility(View.GONE);
            } else {
                viewHolder.contents.setVisibility(View.VISIBLE);

                viewHolder.itemText.setText(item.desc);
                viewHolder.itemSwitch.setText(item.title);
                viewHolder.itemSwitch.setChecked(item.selected);
            }

            if (isNullOrEmpty(item.separator)) {
                viewHolder.itemSeparator.setVisibility(View.GONE);
            } else {
                viewHolder.itemSeparator.setText(item.separator);
                viewHolder.itemSeparator.setVisibility(View.VISIBLE);
            }
            convertView.setTag(viewHolder);
            viewHolder.itemSwitch.setTag(item);
        } catch (Exception ignored) {
        }

        return convertView;
    }

    private boolean isNullOrEmpty(String text) {
        return text == null || text.trim().equals("");
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return actionInfos != null;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    protected class ViewHolder {
        TextView itemSeparator = null;
        View contents = null;
        Switch itemSwitch = null;
        TextView itemText = null;
    }
}
