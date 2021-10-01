package Fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import tool.ConnectThread;
import tool.NewListDataSQL;

import com.example.notification.BluetoothConnectCallback;
import com.example.notification.MainActivity;
import com.example.notification.MainService;
import com.example.notification.R;


public class MyFragment extends Fragment
{
    private String TAG = "mExample";
    private RecyclerView mRecyclerView;
    private MyListAdapter myListAdapter; //管理ListView每一列的資料與畫面
    private SwipeRefreshLayout swipeRefreshLayout; //使用下拉更新
    private ArrayList<String> arrayList = new ArrayList<>();
    private TextView connectStatus;
    //Broadcast
    private BroadcastReceiver receiver;
    private ProgressDialog dialog;

    public MyFragment()
    {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.frament_home, container, false);
        makeData();
        mRecyclerView = view.findViewById(R.id.recycleview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        myListAdapter = new MyListAdapter();
        mRecyclerView.setAdapter(myListAdapter);
        //下拉刷新
        swipeRefreshLayout = view.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.blue_RURI));//設定下拉刷新圖案顏色
        swipeRefreshLayout.setOnRefreshListener(() ->
        { //設定下拉監聽
            arrayList.clear();  //清除存入資料陣列
            makeData();
            myListAdapter.notifyDataSetChanged();//更新數據
            swipeRefreshLayout.setRefreshing(false);

        });
        recyclerViewAction(mRecyclerView, arrayList, myListAdapter);
        initBluetoothButton(view);
        return view;
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == Activity.RESULT_OK && requestCode==41)
        {
            Toast.makeText(getContext(), "藍芽開啟成功，請重新點連線", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getContext(), "使用著取消", Toast.LENGTH_SHORT).show();
        }
        dialog.dismiss();
    }

    private void makeData()
    {  //亂數產生資料

        NewListDataSQL helper = new NewListDataSQL(getActivity(), "time");
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select time from time", null);


        int rows_num = cursor.getCount();//取得資料表列數
        if (rows_num != 0)
        {
            cursor.moveToFirst();   //將指標移至第一筆資料
            for (int i = 0; i < rows_num; i++)
            {
                String strCr = cursor.getString(0);
                arrayList.add(strCr);
                cursor.moveToNext();//將指標移至下一筆資料
            }
        }
        cursor.close(); //關閉Cursor
        helper.close();//關閉資料庫，釋放記憶體，還需使用時不要關閉
    }

    private class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.ViewHolder>
    {  //繼承RecycleView.Adapter

        public void removeItem(int position)
        {  //從model中移除指定的項目並通知畫面更新
            NewListDataSQL helper = new NewListDataSQL(getActivity(), "time");
            SQLiteDatabase db = helper.getReadableDatabase();

            myListAdapter.notifyItemRemoved(position);
            helper.removeData(db, "time", arrayList.get(position));
            helper.close();//關閉資料庫，釋放記憶體，還需使用時不要關閉

            arrayList.remove(position);
        }


        class ViewHolder extends RecyclerView.ViewHolder
        {  //連結所需要的物件
            private TextView tvId, tvSub1;
            private View mView;

            public ViewHolder(@NonNull View itemView)
            {
                super(itemView);
                tvId = itemView.findViewById(R.id.textView_Id);
                tvSub1 = itemView.findViewById(R.id.textView_sub1);
                mView = itemView;
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {  //連接自訂意的item layout，回傳一個View
            View view;
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycle_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {  //取得元件的控制(每個item內的控制)

            holder.tvId.setText("第" + String.valueOf(position + 1) + "筆");
            holder.tvSub1.setText(arrayList.get(position));

            holder.mView.setOnClickListener((v) ->
            {
                Toast.makeText(getContext(), holder.tvSub1.getText(), Toast.LENGTH_SHORT).show();  //用toast顯示Item內容

            });

        }

        @Override
        public int getItemCount()
        {  //取得所要顯示數量
            return arrayList.size();
        }
    }

    private void recyclerViewAction(RecyclerView recyclerView, final ArrayList<String> choose, final MyListAdapter myAdapter)
    {//  實現左右滑動刪除的效果
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(myListAdapter);
        //  實現左右滑動刪除的效果
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT)
        {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1)
            {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
            {
                // 左右滑動callback
                int position = viewHolder.getAdapterPosition();
                myListAdapter.removeItem(position);
            }
        }).attachToRecyclerView(recyclerView);
    }
    private void initBluetoothButton(View view)
    {
        dialog=new ProgressDialog(getContext());
        connectStatus = (TextView) view.findViewById(R.id.connectStatus);
        Button Bluetooth = (Button) view.findViewById(R.id.button5);
        Button send = (Button) view.findViewById(R.id.button);
        send.setOnClickListener(v -> send());
        Bluetooth.setOnClickListener(v -> connect());
        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {

                // 處理 Service 傳來的訊息。
                Bundle message = intent.getExtras();
                int status = message.getInt("connectStatus");

                dialog.setTitle("連線中");
                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);

                if (status == MainService.connecting)
                    dialog.show();
                else
                    dialog.dismiss();

                if (status == MainService.connect)
                    connectStatus.setText("連線狀態:已連線");
                else if (status == MainService.disconnect)
                    connectStatus.setText("連線狀態:連線中斷");
                else if (status == BluetoothConnectCallback.nobind)
                    Toast.makeText(getContext(), "未配對，請先配對", Toast.LENGTH_SHORT).show();
                else if (status == BluetoothConnectCallback.noSearch)
                    Toast.makeText(getContext(), "請確認裝置在附近", Toast.LENGTH_SHORT).show();
                else if (status == BluetoothConnectCallback.bluetoothNoSupport)
                    Toast.makeText(getContext(), "未支援藍芽，請更換設備", Toast.LENGTH_SHORT).show();

            }
        };
        IntentFilter filter = new IntentFilter("MainService");
        // 將 BroadcastReceiver 在 Activity 掛起來。
        getActivity().registerReceiver(receiver, filter);
    }
    private void connect()
    {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 41);
        }
        else
        {
            Intent intent = new Intent("tt");
            intent.setPackage(getContext().getPackageName());
            getContext().startService(intent);
        }
    }

    private void send()
    {
        Intent intent = new Intent(getActivity(), MainService.class);
        getContext().stopService(intent);
    }
}