package com.example.notification;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import internet.HttpConnect;
import java.util.HashMap;
import internet.Internet;

public class Register extends AppCompatActivity
{
    private EditText editTextName;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private EditText editTextEmail;
    private EditText editTextTel;
    private Button buttonRegister;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_main);
        editTextName = (EditText) findViewById(R.id.name);
        editTextUsername = (EditText) findViewById(R.id.username);
        editTextPassword = (EditText) findViewById(R.id.password);
        editTextEmail = (EditText) findViewById(R.id.email);
        editTextTel= (EditText) findViewById(R.id.tel);
        buttonRegister = (Button) findViewById(R.id.create);
        buttonRegister.setOnClickListener(buttonListener);
    }
    private Button.OnClickListener buttonListener = new Button.OnClickListener()
    {/**監聽創見帳號鈕是否被按下**/
        @Override
        public void onClick(View v)
        {
            if(v == buttonRegister)
            {
                registerUser();/**呼叫這函式進行使用者資料獲取**/
            }
        }
    };
    private void registerUser()
    {/**讀取使用者輸入數據**/
        String name = editTextName.getText().toString().trim().toLowerCase();
        String username = editTextUsername.getText().toString().trim().toLowerCase();
        String password = editTextPassword.getText().toString().trim().toLowerCase();
        String email = editTextEmail.getText().toString().trim().toLowerCase();
        String tel = editTextTel.getText().toString().trim().toLowerCase();
        register(name,username,password,email,tel);/**獲取資料成功後，開始進行傳送**/
    }
    private void register(String name, String username, String password, String email, String tel)
    {
        class RegisterUser extends AsyncTask<String, Void, String>
        {
            HttpConnect ruc = new HttpConnect(false);/**使用Creatmem.class的功能**/
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();/**當按下創見鈕，出現提式窗**/
            }
            @Override
            protected void onPostExecute(String s)
            {
                super.onPostExecute(s);
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                if(s.equals("帳號創建成功"))/**當字串比對成功返回登入頁面**/
                {
                    //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.setClass(Register.this,LoginMain.class);
                    startActivity(intent);
                    Register.this.finish();
                }
            }
            @Override
            protected String doInBackground(String... params)/**將資料放入hashmap，測試call by value or call br address**/
            {
                HashMap<String, String> data = new HashMap<String,String>();
                data.put("name",params[0]);
                data.put("password",params[1]);
                data.put("username",params[2]);
                data.put("email",params[3]);
                data.put("tel",params[4]);
                String result = ruc.sendPostRequest(Internet.REGISTER_URL+"Create.php",data);
                return  result;
            }
        }
        RegisterUser ru = new RegisterUser();/**傳送資料**/
        ru.execute(name, password, username, email,tel);
    }
}
/*public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
*/