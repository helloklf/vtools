package com.omarea.vboot;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.omarea.shared.ServiceHelper;
import com.omarea.shared.cmd_shellTools;


public class fragment_xposed extends Fragment {

    public fragment_xposed() {
        // Required empty public constructor
    }


    TextView servicceXposedState;
    cmd_shellTools cmdshellTools = null;
    activity_main thisview = null;

    public static Fragment Create(activity_main thisView, cmd_shellTools cmdshellTools) {
        fragment_xposed fragment = new fragment_xposed();
        fragment.cmdshellTools = cmdshellTools;
        fragment.thisview = thisView;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_xposed, container, false);
    }


    /*
    String xx =  Build.BOARD;
    String xx1 =  Build.PRODUCT;
    String xx2 =  Build.BRAND;
    String xx3 =  Build.MODEL;
    */
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        servicceXposedState = ((TextView) view.findViewById(R.id.vbootxposedservice_state));
        //跳转到Xposed界面
        servicceXposedState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent("de.robv.android.xposed.installer.OPEN_SECTION");
                    intent.setPackage("de.robv.android.xposed.installer");
                    intent.putExtra("section", "modules");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Snackbar.make(getView(), "打开Xposed管理器失败，你似乎还没有安装？", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        servicceXposedState.setText(ServiceHelper.xposedIsRunning() ? "模块已激活-工作正常" : "Xposed模块未激活");
    }

}
