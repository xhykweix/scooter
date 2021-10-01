package com.example.notification;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.service.autofill.FieldClassification;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import internet.HttpConnect;
import internet.Internet;

public class getGpsService extends Service
{
    private TimerTask task;
    private TimerTask taskAddresses;
    private Timer timer = new Timer();
    private Timer timerAddresses = new Timer();
    private GoogleMap mMap;
    private Marker marker;
    private TextView speedMessage;
    private TextView addressesMessage;
    private HttpConnect ruc;
    private double lat;
    private double lng;
    private double lastLat;
    private double lastLng;
    private Geocoder geocoder;
    private SharedPreferences pref;                 //存資料Class
    private SharedPreferences prefUuid;
    private boolean moveCamera;
    private final String markerTitle = "您的機車";
    private String Tag = "getGpsService";
    private String uuid;

    //服務創建
    @Override
    public void onCreate()
    {
        super.onCreate();
        ruc = new HttpConnect(true);
        task = new TimerTask()
        {
            @Override
            public void run()
            {
                RegisterUser ru = new RegisterUser();/**傳送資料**/
                ru.execute(uuid);

            }
        };
        taskAddresses = new TimerTask()
        {
            @Override
            public void run()
            {
                List<Address> addresses = null;
                try
                {
                    addresses = geocoder.getFromLocation(lat, lng, 1);
                    addressesMessage.setText(addresses.get(0).getLocality() + addresses.get(0).getThoroughfare() + addresses.get(0).getFeatureName());
                }
                catch (IOException e)
                {
                    Log.e(Tag, "Geocoder get error");
                    e.printStackTrace();
                }
                catch (IllegalArgumentException e)
                {
                    Log.e(Tag, "IllegalArgumentException");
                    e.printStackTrace();
                }
                Log.d(Tag, "getGps per 0.5s");
            }
        };

    }

    // 服務啟動
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return super.onStartCommand(intent, flags, startId);
    }

    //服務銷毀
    @Override
    public void onDestroy()
    {
        stopSelf(); //自殺服務
        pref.edit().putFloat("LAT", (float) lat).putFloat("LNG", (float) lng).commit();
        timer.cancel();
        timerAddresses.cancel();
        task.cancel();
        taskAddresses.cancel();
        ruc.stop();
        Log.d(Tag, "onDestroy");
        super.onDestroy();
    }

    //綁定服務
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return new MyBinder();
    }


    public void sync(GoogleMap googleMap, TextView speedMessage, TextView addressesMessage, Geocoder geocoder)
    {
        pref = getSharedPreferences("GPS", MODE_PRIVATE);
        prefUuid = getSharedPreferences("Login", MODE_PRIVATE);
        lat = pref.getFloat("LAT", 24.14458f);
        lng = pref.getFloat("LNG", 120.72863f);
        lastLng = lng;
        lastLat = lat;

        uuid = prefUuid.getString("uuid", "");
        Toast.makeText(getApplicationContext(), uuid, Toast.LENGTH_SHORT).show();

        mMap = googleMap;
        this.speedMessage = speedMessage;
        this.addressesMessage = addressesMessage;
        this.geocoder = geocoder;

        marker = mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(markerTitle).icon(BitmapDescriptorFactory.fromResource(R.drawable.scooter_icon)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 18f));

        List<Address> addresses = null;
        try
        {
            addresses = geocoder.getFromLocation(lat, lng, 1);
            addressesMessage.setText(addresses.get(0).getLocality() + addresses.get(0).getThoroughfare() + addresses.get(0).getFeatureName());
        }
        catch (IOException e)
        {
            Log.e(Tag, "Geocoder get error");
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            Log.e(Tag, "IllegalArgumentException");
            e.printStackTrace();
        }

        timer.schedule(task, 2000, 500);
        timerAddresses.schedule(taskAddresses, 500, 1000);
    }

    public void moveCamera(boolean moveCamera)
    {
        this.moveCamera = moveCamera;
        if (moveCamera)
        {
            Projection proj = mMap.getProjection();
            Point startPoint = proj.toScreenLocation(marker.getPosition());
            final LatLng startLatLng = proj.fromScreenLocation(startPoint);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(startLatLng.latitude, startLatLng.longitude), 18f));
        }
    }

    void stop()
    {
        ruc.stop();
    }

    // IBinder是远程对象的基本接口，是为高性能而设计的轻量级远程调用机制的核心部分。但它不仅用于远程
    // 调用，也用于进程内调用。这个接口定义了与远程对象交互的协议。
    // 不要直接实现这个接口，而应该从Binder派生。
    // Binder类已实现了IBinder接口
    class MyBinder extends Binder
    {
        /**
         * 获取Service的方法 * @return 返回PlayerService
         */
        public getGpsService getService()
        {
            return getGpsService.this;
        }
    }

    private class RegisterUser extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();/**當按下創見鈕，出現提式窗**/
        }

        @Override
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);
            String speed = "0.0";
            Pattern patternDis = Pattern.compile("(\\d+\\.\\d+)");
            Matcher matcherDis = patternDis.matcher(s);

            if (matcherDis.find())
            {
                lat = Double.parseDouble(matcherDis.group());
                matcherDis.find();
                lng = Double.parseDouble(matcherDis.group());
                matcherDis.find();
                speed = matcherDis.group();
            }
            speedMessage.setText(speed);
            animateMarker(marker, new LatLng(lat, lng), false);
        }

        @Override
        protected String doInBackground(String... params)/**將資料放入hashmap，**/
        {
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("uuid", params[0]);

            String result = ruc.sendPostRequest(Internet.REGISTER_URL + "gpsPull.php", data);
            return result;
        }
    }


    public void animateMarker(final Marker marker, final LatLng toPosition, final boolean hideMarker)
    {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();

        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);


        if (lastLat != 0.0 && lastLng != 0.0 && (Math.abs(lastLat - lat) > 0.00001 || Math.abs(lastLng - lng) > 0.00001))
        {
            lastLng = lng;
            lastLat = lat;

            final long duration = 500;
            final Interpolator interpolator = new LinearInterpolator();


            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);
                    double lngTemp = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                    double latTemp = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                    marker.setPosition(new LatLng(lat, lng));
                    if (moveCamera)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latTemp, lngTemp), 18f));
                    if (t < 1.0)
                    {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    }
                    else
                    {
                        if (hideMarker)
                        {
                            marker.setVisible(false);
                        }
                        else
                        {
                            marker.setVisible(true);
                        }
                    }
                }
            });
        }
    }
}
