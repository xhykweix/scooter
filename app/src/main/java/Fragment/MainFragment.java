package Fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.notification.BluetoothConnectCallback;
import com.example.notification.Locate;
import com.example.notification.MainActivity;
import com.example.notification.MainService;
import com.example.notification.R;
import com.google.android.gms.maps.SupportMapFragment;

public class MainFragment extends Fragment implements RadioGroup.OnCheckedChangeListener
{
    private RadioGroup rg_tab_bar;
    private RadioButton rb_main;

    //Fragment Object
    private MyFragment main;
    private Locate locate;
    //private MainFragment fragment_main;
    private FragmentManager fManager;
    private View view;
    private MainActivity mainActivity;
    private boolean isPause;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_main, container, false);
        mainActivity = ((MainActivity) getActivity());
        // Inflate the layout for this fragment
        fManager = getFragmentManager();
        rg_tab_bar = (RadioGroup) view.findViewById(R.id.rg_tab_bar);
        rg_tab_bar.setOnCheckedChangeListener(this);
        rb_main = (RadioButton) view.findViewById(R.id.rb_main);
        return view;

    }

    @Override
    public void onResume()
    {
        super.onResume();
        rb_main.setChecked(true);
        mainActivity.setActionBarTitle("Home");
        Log.d("MainFragment", "onResume");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.d("MainFragment", "onPause");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        FragmentTransaction fTransaction = fManager.beginTransaction();
        hideAllFragment(fTransaction);
        switch (checkedId)
        {
            case R.id.rb_main:
                if (locate != null)
                    locate.onPause();
                if (main == null)
                {
                    main = new MyFragment();
                    fTransaction.add(R.id.ly_content, main);
                }
                else
                    fTransaction.show(main);
                break;
            case R.id.rb_locate:
                if (locate == null)
                {
                    locate = new Locate();
                    fTransaction.add(R.id.ly_content, locate);
                }
                else
                {
                    locate.onResume();
                    locate.bind();
                    fTransaction.show(locate);
                }
                break;
        }
        fTransaction.commit();

    }

    private void hideAllFragment(FragmentTransaction fTransaction)
    {
        if (main != null) fTransaction.hide(main);
        if (locate != null) fTransaction.hide(locate);
    }

}
