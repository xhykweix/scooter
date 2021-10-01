package Fragment;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.example.notification.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Time;
import java.util.Timer;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class GoogleDriveConnectService extends JobIntentService
{
    private static Context context;
    private String device_code;
    private int interval;
    private String TAG="GoogleDriveConnectService";

    /** * Unique job ID for this service. */
    static final int JOB_ID = 1000;

    /** * Convenience method for enqueuing work in to this service. */
    static void enqueueWork(Context context, Intent work)
    {
        GoogleDriveConnectService.context=context;
        enqueueWork(context, GoogleDriveConnectService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent)
    {
        int code=0;
        device_code=intent.getStringExtra("device_code");
        interval=(intent.getIntExtra("interval",5)+1)*1000;

        do
        {
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder()
                    .add("client_id",getString(R.string.client_id))
                    .add("client_secret",getString(R.string.client_secret))
                    .add("device_code",device_code)
                    .add("grant_type",getString(R.string.grant_type))
                    .build();

            final Request request = new Request.Builder()
                    .url("https://oauth2.googleapis.com/token")
                    .post(requestBody)
                    .build();

            try {
                Response response=client.newCall(request).execute();
                code=response.code();
                if (code==200)
                    responseBody(response);
                else if(code==428)
                    Log.d(TAG,"Wait User");
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }while (code!=200);
    }

    private void responseBody(Response response)
    {
        try {
            JSONObject jsonObject = new JSONObject(response.body().string());
            Log.i(TAG,jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
