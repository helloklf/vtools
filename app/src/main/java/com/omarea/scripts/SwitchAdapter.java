package com.omarea.scripts;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.omarea.scripts.simple.shell.ExecuteCommandWithOutput;
import com.omarea.scripts.switchs.SwitchInfo;
import com.omarea.vtools.R;

import java.util.ArrayList;

public class SwitchAdapter implements ListAdapter {
    private ArrayList<SwitchInfo> actionInfos;

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
            actionInfo.desc = ExecuteCommandWithOutput.executeCommandWithOutput(false, actionInfo.descPollingShell);
        }
        if (actionInfo.descPollingSUShell != null && !actionInfo.descPollingSUShell.isEmpty()) {
            actionInfo.desc = ExecuteCommandWithOutput.executeCommandWithOutput(true, actionInfo.descPollingSUShell);
        }
        if (actionInfo.getState != null && !actionInfo.getState.isEmpty()) {
            String shellResult = ExecuteCommandWithOutput.executeCommandWithOutput(actionInfo.root, actionInfo.getState);
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
        View convertView = view;
        ViewHolder viewHolder;
        SwitchInfo item = (SwitchInfo) getItem(position);
        try {
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = View.inflate(parent.getContext(), R.layout.switch_row_item, null);
                viewHolder.itemSwitch = convertView.findViewById(R.id.Title);
                viewHolder.itemText = convertView.findViewById(R.id.Desc);
                viewHolder.itemSwitch.setText((item.title));
                viewHolder.itemText.setText(item.desc);
                viewHolder.itemSwitch.setChecked(item.selected);
                viewHolder.itemSwitch.setTag(item);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                viewHolder.itemSwitch.setText((item.title));
                viewHolder.itemText.setText(item.desc);
                viewHolder.itemSwitch.setChecked(item.selected);
                viewHolder.itemSwitch.setTag(item);
            }
        } catch (Exception ignored) {

        }

        return convertView;
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
        Switch itemSwitch = null;
        TextView itemText = null;
    }
}
