package com.example.notification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import internet.MyFirebaseService;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tool.ConnectThread;
import tool.MyBluetoothService;

public class WebDrive extends AppCompatActivity
{

    private Toolbar toolbar;
    private String device_code;
    private String TAG = "WebDrive";
    private WebView webView;
    private ProgressDialog dialog;
    TimerTask timerTask;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        dialog=new ProgressDialog(this);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        toolbar = (Toolbar) findViewById(R.id.driveBar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("請輸入此代碼: " + getIntent().getStringExtra("user_code"));
        int interval = (getIntent().getIntExtra("interval", 5)) * 1000 + 500;

        String url = getIntent().getStringExtra("url");
        webView = (WebView) findViewById(R.id.web_drvie);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //setContentView(webview);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);

        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                onHandleWork(getIntent());
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, interval, interval);

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 40);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        timerTask.cancel();
        timer.cancel();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==40 && resultCode== Activity.RESULT_CANCELED)
        {
            tost("使用者取消");
            finish();
        }
    }

    protected void onHandleWork(@NonNull Intent intent)
    {
        int code = 0;
        device_code = intent.getStringExtra("device_code");

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", getString(R.string.client_id))
                .add("client_secret", getString(R.string.client_secret))
                .add("device_code", device_code)
                .add("grant_type", getString(R.string.grant_type))
                .build();

        final Request request = new Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(requestBody)
                .build();

        try
        {
            Response response = client.newCall(request).execute();
            code = response.code();
            if (code == 200)
                responseBody(response);
            else if (code == 428)
                Log.d(TAG, "Wait User");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void responseBody(Response response)
    {
        dialog("傳送中...");
        JSONObject jsonObject=null;
        try
        {
            jsonObject = new JSONObject(response.body().string());
            Log.i(TAG, jsonObject.toString());
        }
        catch (JSONException | IOException e)
        {
            e.printStackTrace();
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                webView.clearCache(true);
                webView.reload();
                webView.loadUrl("about:blank");
            }
        });
        byte[] jsonByte=jsonObject.toString().getBytes(StandardCharsets.UTF_8);


        BluetoothConnectThread bluetoothConnectThread=new BluetoothConnectThread(getApplicationContext(),new BluetoothConnectCallback()
        {
            @Override
            public void onSuccess(ConnectThread connectThread)
            {
                MyBluetoothService myBluetoothService=new MyBluetoothService(connectThread);
                myBluetoothService.write(jsonByte);
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                connectThread.cancel();
                tost("傳送成功，哨兵模式影片將會上傳至google雲端");
                dialog.dismiss();
                finish();
            }

            @Override
            public void onFailure(int code)
            {
                if (code == BluetoothConnectCallback.noSearch)
                {
                    tost("請確認裝置在附近");
                }
                else if (code == BluetoothConnectCallback.nobind)
                {
                   tost("請先綁定");
                }
                else if(code==BluetoothConnectCallback.bluetoothNoSupport)
                {
                    tost("未支援藍芽，請更換設備");
                }
                dialog.dismiss();
                Log.d("WebDrive","無法連線");
                finish();
            }
        });
        bluetoothConnectThread.start();
    }
    private void tost(String text)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dialog(String text)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                dialog.setTitle(text);
                dialog.show();
            }
        });
    }

}
