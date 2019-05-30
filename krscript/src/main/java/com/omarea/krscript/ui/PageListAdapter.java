package com.omarea.krscript.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.omarea.krscript.R;
import com.omarea.krscript.model.PageInfo;
import java.util.ArrayList;

public class PageListAdapter extends BaseAdapter {
    private ArrayList<PageInfo> actionInfos;
    private Context context;

    public PageListAdapter(ArrayList<PageInfo> actionInfos) {
        this.actionInfos = actionInfos;
    }

    @Override
    public int getCount() {
        return actionInfos != null ? actionInfos.size() : 0;
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

        final PageInfo item = (PageInfo)getItem(position);
        return renderPageItem(item);
    }

    private View renderPageItem(PageInfo item) {
        PageViewHolder viewHolder = new PageViewHolder();
        View convertView = View.inflate(context, R.layout.list_item_kr_page, null);
        viewHolder.itemTitle = convertView.findViewById(R.id.Title);
        viewHolder.itemText = convertView.findViewById(R.id.Desc);

        String desc = item.getDesc();
        String title = item.getTitle();

        if(desc.isEmpty()) {
            viewHolder.itemText.setVisibility(View.GONE);
        } else {
            viewHolder.itemText.setText(desc);
            viewHolder.itemText.setVisibility(View.VISIBLE);
        }

        if(title.isEmpty()) {
            viewHolder.itemTitle.setVisibility(View.GONE);
        } else {
            viewHolder.itemTitle.setText(title);
            viewHolder.itemTitle.setVisibility(View.VISIBLE);
        }

        convertView.setTag(viewHolder);
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

    protected class PageViewHolder {
        TextView itemTitle = null;
        TextView itemText = null;
    }
}
