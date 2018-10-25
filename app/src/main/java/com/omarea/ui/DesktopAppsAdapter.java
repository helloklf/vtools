package com.omarea.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.omarea.vtools.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DesktopAppsAdapter extends BaseAdapter {
    private List<ResolveInfo> apps;
    private List<ResolveInfo> allApps;
    private Context mContent;
    private PackageManager packageManager;
    // private LruCache<String, Drawable> iconsCache = new LruCache<>(200);
    private HashMap<String, Drawable> iconsCache = new HashMap<>(200);
    private Handler handler = new Handler();
    private String keywords = "";

    public DesktopAppsAdapter(List<ResolveInfo> apps, Context mContent, String keywords) {
        this.allApps = apps;
        this.keywords = keywords.toLowerCase();
        this.mContent = mContent;
        this.packageManager = mContent.getPackageManager();
        serchApp();
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int i) {
        return apps.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ResolveInfo info = apps.get(i);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContent).inflate(R.layout.desktop_app_item, null);
            //使用dp进行参数设置。进行分辨率适配。
            convertView.setLayoutParams(new GridView.LayoutParams
            (
                (int) mContent.getResources().getDimension(R.dimen.app_width),
                (int) mContent.getResources().getDimension(R.dimen.app_height))
            );
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (convertView.getTag() == null) {
            viewHolder = new ViewHolder();
            viewHolder.image = convertView.findViewById(R.id.image);
            viewHolder.text = convertView.findViewById(R.id.text);
        }

        viewHolder.text.setText(keywordHightLight(info.loadLabel(mContent.getPackageManager()).toString()));
        String packageName = info.activityInfo.packageName;
        if (iconsCache.containsKey(packageName)) {
            final Drawable drawable = iconsCache.get(packageName);
            viewHolder.image.setImageDrawable(drawable);
        } else {
            new LoadIconThread(viewHolder.image, info).start();
        }
        convertView.setTag(viewHolder);

        return convertView;
    }

    public void setKeywords (String keywords) {
        if (keywords == null) {
            keywords = "";
        }
        this.keywords = keywords.toLowerCase();
        serchApp();
        notifyDataSetChanged();
    }

    private void serchApp () {
        List<ResolveInfo> apps = new ArrayList<>();
        for (int i=0; i< allApps.size(); i++) {
            if (keywordSearch(allApps.get(i))) {
                apps.add(allApps.get(i));
            }
        }
        this.apps = apps;
    }

    private Boolean keywordSearch(ResolveInfo item) {
        return item.activityInfo.packageName.toLowerCase().contains(keywords) || item.activityInfo.loadLabel(packageManager).toString().toLowerCase().contains(keywords);
    }

    private SpannableString keywordHightLight(String str) {
        SpannableString spannableString = new SpannableString(str);
        int index = 0;
        if (keywords.isEmpty()) {
            return spannableString;
        }
        index = str.toLowerCase().indexOf(keywords.toLowerCase());
        if (index < 0)
            return spannableString;

        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#0094ff")), index, index + keywords.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    class LoadIconThread extends Thread {
        ImageView image;
        ResolveInfo info;
        public LoadIconThread(ImageView image, ResolveInfo info) {
            this.image = image;
            this.info = info;
        }
        @Override
        public void run() {
            super.run();
            final Drawable drawable = info.activityInfo.loadIcon(packageManager);
            iconsCache.put(info.activityInfo.packageName, drawable);
            if (image == null)
                return;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    image.setImageDrawable(drawable);
                }
            });
        }
    }

    class ViewHolder {
        ImageView image;
        TextView text;
    }
}
