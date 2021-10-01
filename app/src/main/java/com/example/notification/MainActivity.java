package com.example.notification;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.ui.AppBarConfiguration;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import android.bluetooth.BluetoothAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import Fragment.MainFragment;
import Fragment.MyFragment;
import Fragment.SettingFragment;
import internet.Internet;

import static android.content.ContentValues.TAG;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{

    private SharedPreferences pref;
    private RadioGroup rg_tab_bar;
    private RadioButton rb_main;
    //Fragment Object
    private MyFragment main, locate;
    private MainFragment mainFragment;
    private SettingFragment settingFragment;
    private FragmentManager fManager;
    private int lastItemId;
    boolean lastSet = true;
    Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

    private AppBarConfiguration mAppBarConfiguration;

    private DrawerLayout drawer;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private boolean interruptIsDisable;
    private String TAG = "MainActivity";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Start service
        Log.d(TAG, "Start");

        init();
        if (pairedDevices.size() > 0)
        {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices)
            {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                System.out.println("Name: " + deviceName + " Address: " + deviceHardwareAddress);
            }
        }
        //initListener();
        //bluetoothPair();
        if (!isPurview(this))// 檢查權限是否開啟，未開啟則開啟對話框
        {
            new AlertDialog.Builder(MainActivity.this)// 跳轉自開啟權限畫面，權限開啟後通知欄擷取服務將自動啟動。
                    .setTitle("啟用通知欄權限")
                    .setMessage("請啟用通知欄擷取權限")
                    .setIcon(R.mipmap.ic_launcher_round)
                    .setCancelable(false)
                    .setPositiveButton("開啟", (d, w) -> super.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")))// 對話框按鈕事件
                    .show();
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
        {
            View view = findViewById(android.R.id.content);
            Snackbar.make(view, "This is explanation: Please give us permission", Snackbar.LENGTH_LONG)
                    .setAction("OK", new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
                        }
                    }).show();
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {//未開啟定位權限

            /*new AlertDialog.Builder(MainActivity.this)// 跳轉自開啟權限畫面，權限開啟後通知欄擷取服務將自動啟動。
                    .setTitle("請開啟定位權限")
                    .setMessage("請啟用定位權限")
                    .setIcon(R.mipmap.ic_launcher_round)
                    .setCancelable(false)
                    .setPositiveButton("開啟", (d, w) -> super.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")))// 對話框按鈕事件
                    .show();*/
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        }
        else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        }
        else
        {
            Toast.makeText(MainActivity.this, "已開啟定位權限", Toast.LENGTH_LONG).show();
        }

        drawer = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_homic);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        //ft.replace(R.id.navi_fragment,mainFragment);
        //ft.commit();

        navigationView.setCheckedItem(R.id.nav_home);
        onNavigationItemSelected(navigationView.getCheckedItem());
        /*ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
        actionBarDrawerToggle.syncState();
        drawer.addDrawerListener(actionBarDrawerToggle);*/

    }

    public void setActionBarTitle(String title)
    {
        getSupportActionBar().setTitle(title);
    }


    private boolean isPurview(Context context) // 檢查權限是否開啟 true = 開啟 ，false = 未開啟
    {

        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        if (packageNames.contains(context.getPackageName()))
        {
            return true;
        }
        return false;
    }

    private void init()
    {
        //Bluetooth = (Button) findViewById(R.id.button5);
        pref = getSharedPreferences("Login", MODE_PRIVATE);
        SharedPreferences prefPairMAC = getSharedPreferences("Bluetooth", MODE_PRIVATE);
        if(!prefPairMAC.getString("PairMAC","noPair").equals("noPair"))
        {
            Intent intent = new Intent(this, BleCallBack.class);
            startService(intent);
        }
    }


    /*@Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }*/

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                drawer.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override//側邊選單欄按下動作
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        int itemIdTemp = item.getItemId();
        int stackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        MenuItem checkItem = navigationView.getCheckedItem();

        Log.d(TAG, "" + stackEntryCount);
        switch (itemIdTemp)
        {
            case R.id.nav_home:
                if (stackEntryCount == 1)
                {
                    settingFragment.onPause();
                    mainFragment.onResume();
                    onBackPressed();
                }
                else if (mainFragment == null)
                {
                    mainFragment = new MainFragment();
                    ft.add(R.id.navi_fragment, mainFragment);
                    ft.commit();
                }
                break;
            case R.id.nav_setting:
                interruptIsDisable = true;
                if (checkItem.getItemId() != R.id.nav_setting)
                {
                    ft.addToBackStack(null);
                    if (settingFragment == null)
                        settingFragment = new SettingFragment();
                    ft.add(R.id.navi_fragment, settingFragment);
                    ft.commit();
                }
                interruptIsDisable = false;
                break;
            case R.id.nav_logout:
                lastSet = false;
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("登出")
                        .setMessage("確定要登出嗎?")
                        .setPositiveButton("Yes", (d, w) ->
                        {
                            startActivity(new Intent().setClass(MainActivity.this, LoginMain.class));
                            unsubscribe();
                            finish();
                        })
                        .setNegativeButton("No", (d, w) ->
                        {
                            navigationView.setCheckedItem(lastItemId);
                            lastSet = true;
                        })
                        .show();
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        if (lastSet)
            lastItemId = itemIdTemp;
        return true;
    }

    @Override//按下返回建關閉側邊選單欄
    public void onBackPressed()
    {
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        if (navigationView.getCheckedItem().getItemId() == R.id.nav_setting)
        {
            navigationView.setCheckedItem(R.id.nav_home);
            settingFragment.onPause();
            mainFragment.onResume();
        }
        super.onBackPressed();
    }

    //取消訂閱通知
    private boolean unsubscribe()
    {
        pref.edit().putBoolean("LoginStatus", false).commit();
        String uuid = pref.getString("uuid", "");
        if (uuid.isEmpty())
        {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("錯誤")
                    .setMessage("uuid為空，請截圖聯繫開發人員!")
                    .setPositiveButton("ok", (d, w) ->
                    {
                    })
                    .show();
            return false;
        }
        else
        {
            Task response = null;
            FirebaseMessaging.getInstance().unsubscribeFromTopic(uuid).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {
                    String msg = "unsubscription successful";
                    if (!task.isSuccessful())
                        msg = "unsubscription fail";
                    Log.d(TAG, msg);
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }
    }
}