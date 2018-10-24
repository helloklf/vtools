package com.omarea.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.omarea.vtools.R;
import java.util.List;

public class DesktopAppsAdapter extends BaseAdapter {
    private List<ResolveInfo> apps;
    private Context mContent;
    private PackageManager packageManager;
    private LruCache<String, Drawable> iconsCache = new LruCache<>(200);
    private Handler handler = new Handler();

    public DesktopAppsAdapter(List<ResolveInfo> apps, Context mContent) {
        this.apps = apps;
        this.mContent = mContent;
        this.packageManager = mContent.getPackageManager();
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
    public View getView(int i, View view, ViewGroup viewGroup) {
        ResolveInfo info = apps.get(i);

        View convertView = LayoutInflater.from(mContent).inflate(R.layout.desktop_app_item, null);
        ImageView image = convertView.findViewById(R.id.image);
        TextView text = convertView.findViewById(R.id.text);
        text.setText(info.loadLabel(mContent.getPackageManager()));
        String packageName = info.activityInfo.packageName;
        final Drawable drawable = iconsCache.get(packageName);
        if (drawable != null) {
            image.setImageDrawable(drawable);
        } else {
            new LoadIconThread(image, info).start();
        }
        // image.setImageDrawable(info.activityInfo.loadIcon(mContent.getPackageManager()));
        // convertView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        //使用dp进行参数设置。进行分辨率适配。
        convertView.setLayoutParams(new GridView.LayoutParams
        (
            (int) mContent.getResources().getDimension(R.dimen.app_width),
            (int) mContent.getResources().getDimension(R.dimen.app_height))
        );
        return convertView;
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
}
