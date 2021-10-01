package com.example.notification;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;


import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Locate extends Fragment implements OnMapReadyCallback {

    private final String Tag="Locate";
    private GoogleMap mMap;
    private TextView speedMessage;
    private TextView addressesMessage;
    private Button start;
    private SupportMapFragment mapFragment;
    private View view;
    private getGpsService getGpsService;
    ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
	    public void onServiceConnected(ComponentName name, IBinder service)
        {
            // 建立連接
            // 獲取服務的操作對象
            getGpsService.MyBinder binder = (getGpsService.MyBinder)service;
            getGpsService=binder.getService();// 獲取到的Service即MyService
            getGpsService.sync(mMap,speedMessage,addressesMessage,new Geocoder(getActivity(), Locale.TRADITIONAL_CHINESE));
        }
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            // 連接斷開

        }
    };

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(Tag,"onCreateView");

        view =  inflater.inflate(R.layout.locate, container, false);
        Intent intent = new Intent(getActivity(),getGpsService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        return view;
    }
    @Override
    public void onStart( )
    {
        super.onStart();
        Log.d(Tag,"onStart");

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        speedMessage=(TextView) view.findViewById(R.id.textView1);
        addressesMessage=(TextView) view.findViewById(R.id.textView3);
        start=(Button) view.findViewById(R.id.getScooterGps);

    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(Tag,"onResume");
    }

    public void bind()
    {
        Intent intent = new Intent(getActivity(),getGpsService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onPause()
    {
        super.onPause();
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(getActivity().ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if ("com.example.notification.getGpsService".equals(service.service.getClassName()))
                getActivity().unbindService(serviceConnection);
        Log.d(Tag,"onPause");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(Tag,"onStop");
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        Log.d(Tag,"onDestroyView");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(Tag,"onDestroy");
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        //getActivity().unbindService(serviceConnection);
        Log.d(Tag,"onDetach");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener()
        {
            @Override
            public void onMapClick(LatLng point) { getGpsService.moveCamera(false); }
        });

        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener()
        {
            @Override
            public void onCameraMoveStarted(int i)
            {
                if(i==REASON_GESTURE)
                    getGpsService.moveCamera(false);
            }
        });

        start.setOnClickListener(v -> getGpsService.moveCamera(true));
    }

}