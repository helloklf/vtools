package com.omarea.vboot;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.omarea.shared.AppShared;
import com.omarea.shared.cmd_shellTools;

import java.io.UnsupportedEncodingException;

public class rom2zip extends AppCompatActivity {


    AppCompatActivity thisview;
    ProgressBar progressBar;
    cmd_shellTools cmdshellTools;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        setContentView(R.layout.activity_rom2zip);
        Toolbar toolbar = (Toolbar) findViewById(R.id.rom2ziptoolbar1);
        setSupportActionBar(toolbar);
        thisview = this;
        progressBar = (ProgressBar) findViewById(R.id.rom2zipprogressBar);
        cmdshellTools = new cmd_shellTools(thisview, progressBar);

        AssetManager assetManager = getAssets();
        AppShared.WriteFile(assetManager, "rom.zip", true);
        AppShared.WriteFile(assetManager, "romvboot.zip", true);
        AppShared.WriteFile(assetManager, "zip.zip", false);

        final CheckBox rom2zip_boot = (CheckBox) findViewById(R.id.rom2zip_boot);
        final CheckBox rom2zip_sys = (CheckBox) findViewById(R.id.rom2zip_sys);
        final CheckBox rom2zip_rec = (CheckBox) findViewById(R.id.rom2zip_rec);
        final CheckBox rom2zip_other = (CheckBox) findViewById(R.id.rom2zip_other);
        final EditText rom2zip_name = (EditText) findViewById(R.id.rom2zip_name);
        final TextView rom2zip_needsize = (TextView) findViewById(R.id.rom2zip_needsize);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = 0;
                if (rom2zip_boot.isChecked()) {
                    size += 100;
                }
                if (rom2zip_rec.isChecked()) {
                    size += 100;
                }
                if (rom2zip_sys.isChecked()) {
                    size += 2750;
                }
                if (rom2zip_other.isChecked()) {
                    size += 150;
                }
                rom2zip_needsize.setText("大约需要空间：" + size + "MB");
            }
        };
        rom2zip_boot.setOnClickListener(clickListener);
        rom2zip_sys.setOnClickListener(clickListener);
        rom2zip_rec.setOnClickListener(clickListener);
        rom2zip_other.setOnClickListener(clickListener);

        final FloatingActionButton btn = (FloatingActionButton) findViewById(R.id.rom2zipCommitBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn.setEnabled(false);
                if (!cmdshellTools.IsZipInstalled()) {
                    Snackbar.make(btn, "当前系统不支持Zip命令，无法完成打包操作！", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                try {
                    cmdshellTools.Rom2Zip(rom2zip_boot.isChecked(), rom2zip_sys.isChecked(), rom2zip_rec.isChecked(), rom2zip_other.isChecked(), rom2zip_name.getText().toString().trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
