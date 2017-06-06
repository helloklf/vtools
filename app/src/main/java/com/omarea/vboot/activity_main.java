package com.omarea.vboot;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.omarea.shared.ConfigInfo;
import com.omarea.shared.Consts;
import com.omarea.shared.CrashHandler;
import com.omarea.shared.cmd_shellTools;

import java.io.File;

public class activity_main extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    AppCompatActivity thisview;
    FloatingActionButton fab;
    FragmentManager fragmentManager;
    cmd_shellTools cmdshellTools;
    boolean onToggleSys;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        //传入参数必须为Activity，否则AlertDialog将不显示。

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment = new fragment_home();
        transaction.replace(R.id.main_content, fragment);
        transaction.commit();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        thisview = this;
        progressBar = (ProgressBar) findViewById(R.id.shell_on_execute);
        cmdshellTools = new cmd_shellTools(this, progressBar);

        if (!cmdshellTools.IsRoot()) {
            Snackbar.make(fab, "抱歉，没有Root权限无法运行此应用！", Snackbar.LENGTH_LONG).show();
            return;
        }

        if (!cmdshellTools.IsBusyboxInstalled()) {
            Snackbar.make(fab, "抱歉，没有安装Busybox，无法运行此应用！", Snackbar.LENGTH_LONG).show();
            return;
        }

        //关闭selinux
        cmdshellTools.DoCmd("setenforce 0");

        if (cmdshellTools.IsDualSystem())
            fab.setVisibility(View.VISIBLE);
        else
            fab.setVisibility(View.GONE);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Menu menu = navigationView.getMenu();
        MenuItem nav_vboot = menu.findItem(R.id.nav_vboot);
        if (!cmdshellTools.IsDualSystem())
            nav_vboot.setVisible(false);
        MenuItem nav_profile = menu.findItem(R.id.nav_profile);
        String cpuName = cmdshellTools.GetCPUName();
        if (!(cpuName.contains("8996") || cpuName.contains("8892")))
            nav_profile.setVisible(false);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab.setVisibility(View.GONE);//禁用系统切换按钮
                onToggleSys = true;
                cmdshellTools.ToggleSystem();//切换系统
                Snackbar.make(view, "正在切换系统，请稍等...", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                navigationView.setVisibility(View.GONE);
                toolbar.setVisibility(View.GONE);
                findViewById(R.id.main_content).setVisibility(View.GONE);
            }
        });

        InitApp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

    }

    /*
    public boolean selfPermissionGranted(String permission)
    {
        // For Android < Android M, self permissions are always granted.
        boolean result = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (targetSdkVersion >= Build.VERSION_CODES.M)
            {
                // targetSdkVersion >= Android M, we can
                // use Context#checkSelfPermission
                result = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            }
            else
            {
                // targetSdkVersion < Android M, we have to use PermissionChecker
                result = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
            }
        }
        return result;
    }
    */

    boolean CheckSelfPermission(String permission) {
        return PermissionChecker.checkSelfPermission(activity_main.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_GRANTED;
    }

    private void InitApp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!(
                        CheckSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                                CheckSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                )) {
                    ActivityCompat.requestPermissions(activity_main.this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Manifest.permission.WAKE_LOCK
                    }, 0x11);
                }

                cmdshellTools.InstallShellTools();
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (onToggleSys) {
            return;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    //右上角菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings: {
                Intent intent = new Intent(thisview, activity_accessibility_service_settings.class);
                startActivity(intent);
                break;
            }
            case R.id.action_hotreboot: {
                cmdshellTools.DoCmd(Consts.RebootHot);
                break;
            }
            case R.id.action_reboot: {
                cmdshellTools.DoCmd(Consts.Reboot);
                break;
            }
            case R.id.action_reboot_rec: {
                cmdshellTools.DoCmd(Consts.RebootRecovery);
                break;
            }
            case R.id.action_reboot_bl: {
                cmdshellTools.DoCmd(Consts.RebootBootloader);
                break;
            }
            case R.id.action_reboot9008: {
                cmdshellTools.DoCmd(Consts.RebootOEM_EDL);
                break;
            }
            case R.id.action_shutdown: {
                cmdshellTools.DoCmd(Consts.RebootShutdown);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    //导航菜单选中
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment = null;

        //以下代码用于去除阴影
        if (Build.VERSION.SDK_INT >= 21)
            getSupportActionBar().setElevation(0);

        fab.setVisibility(View.GONE);

        if (id == R.id.nav_home) {
            fragment = new fragment_home();
            if (cmdshellTools.IsDualSystem())
                fab.setVisibility(View.VISIBLE);
        } else if (id == R.id.nav_vboot) {
            File rom2 = new File("/MainData");
            if (rom2.exists()) {
                Snackbar.make(fab, "请在系统一下进行操作！", Snackbar.LENGTH_SHORT).show();
            } else {
                fragment = fragment_vboot.Create(this, cmdshellTools);
            }
        } else if (id == R.id.nav_booster) {
            fragment = fragment_booster.Create(this, cmdshellTools);
        } else if (id == R.id.nav_applictions) {
            fragment = fragment_applistions.Create(this, cmdshellTools);
        } else if (id == R.id.nav_img) {
            fragment = fragment_img.Create(this, cmdshellTools);
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "你也来试试微工具箱吧，我觉得还不错：\r\n http://www.coolapk.com/apk/com.omarea.vboot");
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } else if (id == R.id.nav_feedback) {
            String url = "mqqwpa://im/chat?chat_type=wpa&uin=1191634433";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            /*
            String url = "http://jq.qq.com/?_wv=1027&k=2F5EVSu"; // web address
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
            */
        } else if (id == R.id.nav_profile) {
            fragment = fragment_config.Create(this, cmdshellTools);
        } else if (id == R.id.nav_additional) {
            fragment = fragment_addin.Create(this, cmdshellTools);
        } else if (id == R.id.nav_help) {
            fragment = new fragment_helpinfo();
        } else if (id == R.id.nav_xposed) {
            fragment = fragment_xposed.Create(this, cmdshellTools);
        }

        if (fragment != null) {
            transaction.replace(R.id.main_content, fragment);
            transaction.commit();
            setTitle(item.getTitle());
            item.setChecked(true);
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    enum FileSelectType {
        BootSave, BootFlash, RecSave, RecFlash;
    }

    FileSelectType fileSelectType = FileSelectType.BootSave;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继续

            Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
            String img_path = "";

            File file = null;

            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor actualimagecursor = thisview.getContentResolver().query(uri, proj, null, null, null);

            if (actualimagecursor != null) {
                int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                actualimagecursor.moveToFirst();
                img_path = actualimagecursor.getString(actual_image_column_index);
                file = new File(img_path);
            } else {
                String uriString = uri.toString();
                String a[] = new String[2];
                //判断文件是否在sd卡中
                if (uriString.indexOf(String.valueOf(Environment.getExternalStorageDirectory())) != -1) {
                    //对Uri进行切割
                    a = uriString.split(String.valueOf(Environment.getExternalStorageDirectory()));
                    //获取到file
                    file = new File(Environment.getExternalStorageDirectory(), a[1]);
                    img_path = file.getAbsolutePath();
                } else if (uriString.indexOf(String.valueOf(Environment.getDataDirectory())) != -1) { //判断文件是否在手机内存中
                    //对Uri进行切割
                    a = uriString.split(String.valueOf(Environment.getDataDirectory()));
                    //获取到file
                    file = new File(Environment.getDataDirectory(), a[1]);
                    img_path = file.getAbsolutePath();
                }

            }

            if (img_path.equals("") && file == null) {
                Snackbar.make(fab, "不支持的路径格式，非常抱歉。请向开发者反馈此问题！", Snackbar.LENGTH_SHORT).show();
                return;
            }

            String fileName = file.toString();
            switch (fileSelectType) {
                case BootSave: {

                }
                case BootFlash: {
                    if (fileName.toLowerCase().lastIndexOf(".img") != fileName.length() - 4) {
                        Snackbar.make(fab, "所选文件不是镜像格式（.img）", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    cmdshellTools.FlashBoot(fileName);
                    break;
                }
                case RecSave: {

                    break;
                }
                case RecFlash: {
                    if (fileName.toLowerCase().lastIndexOf(".img") != fileName.length() - 4) {
                        Snackbar.make(fab, "所选文件不是镜像格式（.img）", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    cmdshellTools.FlashRecovery(fileName);
                    break;
                }
                default: {
                    Snackbar.make(fab, "不支持的操作！", Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    public void onPause() {
        ConfigInfo.getConfigInfo().saveChange();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ConfigInfo.getConfigInfo().saveChange();

        super.onDestroy();
    }
}
