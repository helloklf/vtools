package com.omarea.krscripts.config;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.omarea.krscripts.model.ActionInfo;
import com.omarea.krscripts.model.ConfigItemBase;
import com.omarea.krscripts.executor.ScriptEnvironmen;
import com.omarea.krscripts.model.SwitchInfo;
import com.omarea.vtools.R;

import java.util.ArrayList;

public class ActionListAdapter extends BaseAdapter {
    private ArrayList<ConfigItemBase> actionInfos;
    private Context context;

    public ActionListAdapter(ArrayList<ConfigItemBase> actionInfos) {
        this.actionInfos = actionInfos;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) { }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) { }

    @Override
    public int getCount() {
        return actionInfos != null ? actionInfos.size() : 0;
    }

    public void update(int index, ListView listview) {
        int visiblePosition = listview.getFirstVisiblePosition();
        View view = listview.getChildAt(index - visiblePosition);
        ActionViewHolder actionViewHolder = (ActionViewHolder) view.getTag();
        if (actionViewHolder != null) {
            ActionInfo actionInfo = ((ActionInfo) getItem(index));
            if (actionInfo.descPollingShell != null && !actionInfo.descPollingShell.isEmpty()) {
                actionInfo.desc = ScriptEnvironmen.executeResultRoot(listview.getContext(), actionInfo.descPollingShell);
            }
            actionViewHolder.itemText.setText(actionInfo.desc);
        } else {
            SwitchViewHolder holder = (SwitchViewHolder) view.getTag();
            if (holder != null) {
                SwitchInfo actionInfo = ((SwitchInfo) getItem(index));
                if (actionInfo.descPollingShell != null && !actionInfo.descPollingShell.isEmpty()) {
                    actionInfo.desc = ScriptEnvironmen.executeResultRoot(listview.getContext(), actionInfo.descPollingShell);
                }
                if (actionInfo.getState != null && !actionInfo.getState.isEmpty()) {
                    String shellResult = ScriptEnvironmen.executeResultRoot(listview.getContext(), actionInfo.getState);
                    actionInfo.selected = shellResult.equals("1") || shellResult.toLowerCase().equals("true");
                }
                holder.itemSwitch.setChecked(actionInfo.selected);
                holder.itemText.setText(actionInfo.desc);
            }
        }
    }

    @Override
    public Object getItem(int position) {
        return actionInfos != null ? actionInfos.get(position) : null;
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
        if (context == null) {
            context = parent.getContext();
        }

        final Object item = getItem(position);

        try {
            try {
                ActionInfo actionInfo = (ActionInfo)item;
                if (actionInfo != null) {
                    return renderActionItem(actionInfo);
                }
            } catch (ClassCastException ex) {
                SwitchInfo switchInfo = (SwitchInfo)item;
                return renderSwitchItem(switchInfo);
            }
        } catch (Exception ex) {
            Log.e("ActionListAdapter", "" + ex.getLocalizedMessage());
        }

        return view;
    }

    private View renderActionItem(ActionInfo item) {
        ActionViewHolder viewHolder = new ActionViewHolder();
        View convertView = View.inflate(context, R.layout.list_item_kr_action, null);
        viewHolder.itemTitle = convertView.findViewById(R.id.Title);
        viewHolder.itemText = convertView.findViewById(R.id.Desc);
        viewHolder.itemSeparator = convertView.findViewById(R.id.Separator);
        viewHolder.contents = convertView.findViewById(R.id.contents);

        if (isNullOrEmpty(item.desc) && isNullOrEmpty(item.title)) {
            viewHolder.contents.setVisibility(View.GONE);
        } else {
            viewHolder.contents.setVisibility(View.VISIBLE);

            viewHolder.itemText.setText(item.desc);
            viewHolder.itemTitle.setText(item.title);
        }

        if (isNullOrEmpty(item.separator)) {
            viewHolder.itemSeparator.setVisibility(View.GONE);
        } else {
            viewHolder.itemSeparator.setText(item.separator);
            viewHolder.itemSeparator.setVisibility(View.VISIBLE);
        }
        convertView.setTag(viewHolder);
        return convertView;
    }

    private View renderSwitchItem(SwitchInfo item) {
        SwitchViewHolder viewHolder = new SwitchViewHolder();
        View convertView = View.inflate(context, R.layout.list_item_kr_switch, null);
        viewHolder.itemSwitch = convertView.findViewById(R.id.Title);
        viewHolder.itemText = convertView.findViewById(R.id.Desc);
        viewHolder.itemSeparator = convertView.findViewById(R.id.Separator);
        viewHolder.contents = convertView.findViewById(R.id.contents);
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

    protected class ActionViewHolder {
        TextView itemSeparator = null;
        View contents = null;
        TextView itemTitle = null;
        TextView itemText = null;
    }

    protected class SwitchViewHolder {
        TextView itemSeparator = null;
        View contents = null;
        Switch itemSwitch = null;
        TextView itemText = null;
    }
}
