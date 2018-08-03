package com.omarea.scripts;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.omarea.scripts.action.ActionInfo;
import com.omarea.scripts.simple.shell.ExecuteCommandWithOutput;
import com.omarea.vboot.R;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ActionAdapter extends BaseAdapter {
    private ArrayList<ActionInfo> actionInfos;
    private Timer timer;

    public ActionAdapter(ArrayList<ActionInfo> actionInfos) {
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

    public void update(int index, ListView listview) {
        int visiblePosition = listview.getFirstVisiblePosition();
        View view = listview.getChildAt(index - visiblePosition);
        ViewHolder holder = (ViewHolder) view.getTag();
        ActionInfo actionInfo = ((ActionInfo) getItem(index));
        if (actionInfo.descPollingShell != null && !actionInfo.descPollingShell.isEmpty()) {
            actionInfo.desc = ExecuteCommandWithOutput.executeCommandWithOutput(false, actionInfo.descPollingShell);
        }
        if (actionInfo.descPollingSUShell != null && !actionInfo.descPollingSUShell.isEmpty()) {
            actionInfo.desc = ExecuteCommandWithOutput.executeCommandWithOutput(true, actionInfo.descPollingSUShell);
        }
        holder.itemText.setText(actionInfo.desc);
    }

    @Override
    public Object getItem(int position) {
        final ActionInfo item = actionInfos != null ? actionInfos.get(position) : null;
        return item;
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
        final ViewHolder viewHolder;
        final ActionInfo item = (ActionInfo) getItem(position);
        try {
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = View.inflate(parent.getContext(), R.layout.action_row_item, null);
                viewHolder.itemTitle = convertView.findViewById(R.id.Title);
                viewHolder.itemText = convertView.findViewById(R.id.Desc);
                convertView.setTag(viewHolder);
                viewHolder.itemTitle.setText((item.title));
                viewHolder.itemText.setText(item.desc);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                viewHolder.itemTitle.setText((item.title));
                viewHolder.itemText.setText(item.desc);
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
        return actionInfos == null;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    public void startPolling() {
        stopPolling();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            }
        }, 1000);//延时1s执行
    }

    public void stopPolling() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    protected class ViewHolder {
        TextView itemTitle = null;
        TextView itemText = null;
    }
}
