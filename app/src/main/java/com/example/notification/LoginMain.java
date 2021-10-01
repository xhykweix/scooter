package com.example.notification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import internet.HttpConnect;
import internet.Internet;
import tool.KeyStoreHelper;
import tool.SharedPreferencesHelper;

public class LoginMain extends AppCompatActivity
{
    private Button loginButton;
    private Button registerButton;
    private EditText account;
    private EditText password;
    private CheckBox rememberCheck;
    private SharedPreferencesHelper sharedPreferencesHelper;
    private KeyStoreHelper keyStoreHelper;
    private SharedPreferences pref;
    private String TAG="LoginMain";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_main);

        init();
        if(pref.getBoolean("LoginStatus",false))
        {
            Intent intent = new Intent();
            intent.setClass(this, MainActivity.class);
            startActivity(intent);
            LoginMain.this.finish();
        }
        loginButton.setOnClickListener(v -> login());
        registerButton.setOnClickListener(v ->
        {
            Intent intent =new Intent();
            intent.setClass(LoginMain.this,Register.class);
            startActivity(intent);
        });
        if(!sharedPreferencesHelper.getString("Account").equals(""))
        {
            account.setText(sharedPreferencesHelper.getString("Account"));
            password.setText(getPassword());
            rememberCheck.setChecked(true);
        }
    }
    private void login()
    {
        String userName=account.getText().toString().trim().toLowerCase();
        String password=this.password.getText().toString().trim().toLowerCase();
        singIn(userName,password);

    }
    private void singIn(String userName,String password)
    {
        HttpConnect send=new HttpConnect(false);
        class SingInUser extends AsyncTask<String,Void,String >
        {
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
            }
            @Override
            protected void onPostExecute(String s)
            {
                super.onPostExecute(s);
                String uuid=s.substring(5,s.length());
                String status=s.substring(0,5);
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                if(status.equals("登入成功!"))
                {
                    if(rememberCheck.isChecked())
                        keepPassword(userName,password);
                    else
                    {
                        sharedPreferencesHelper.clear();
                    }
                    pref.edit().putBoolean("LoginStatus",true).putString("uuid",uuid).commit();     //紀錄登入狀態
                    FirebaseMessaging.getInstance().subscribeToTopic(uuid)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override//訂閱通知
                                public void onComplete(@NonNull Task<Void> task) {
                                    String msg ="subscription successful";
                                    if (!task.isSuccessful())
                                        msg = "subscription fail";
                                    Log.d(TAG, msg);
                                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                                }
                            });
                    Intent intent = new Intent();
                    intent.setClass(LoginMain.this, MainActivity.class);
                    startActivity(intent);
                    LoginMain.this.finish();
                }
            }
            @Override
            protected String doInBackground(String... data)
            {
                HashMap<String,String> dataOut=new HashMap<>();
                dataOut.put("name",data[0]);
                dataOut.put("password",data[1]);
                return send.sendPostRequest(Internet.REGISTER_URL + "login.php",dataOut);
            }
        }
        SingInUser ru = new SingInUser();/**傳送資料**/
        ru.execute(userName, password);
    }

    private void init()
    {
        //PasswordKeep
        sharedPreferencesHelper =new SharedPreferencesHelper(getApplicationContext());
        keyStoreHelper=new KeyStoreHelper(getApplicationContext(),sharedPreferencesHelper);
        //Button
        loginButton = (Button)findViewById(R.id.button2);
        registerButton=(Button)findViewById(R.id.button1);
        //EditText
        account=(EditText)findViewById(R.id.editTextUsername);
        password=(EditText)findViewById(R.id.editTextPassword);
        //CheckBox
        rememberCheck=(CheckBox)findViewById(R.id.checkBox2);
        //sharedPreferences
        pref=getSharedPreferences("Login",MODE_PRIVATE);
    }
    private void keepPassword(String account,String password)
    {
        String passwordEnc=keyStoreHelper.encrypt(password);
        sharedPreferencesHelper.setInput(passwordEnc);
        sharedPreferencesHelper.setData("Account",account);
    }
    private String getPassword()
    {
        String passwordEnc=sharedPreferencesHelper.getInput();
        return keyStoreHelper.decrypt(passwordEnc);
    }
}
