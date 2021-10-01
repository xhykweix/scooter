package tool;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.notification.MainService;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;

public class NotificationCatchForGoogleMap
{
    private static final int noDir=0;
    private static final int right=1;
    private static final int left=2;
    private static int dirStatus=noDir;
    private static Icon bitmapIcon;
    private static String string;//儲存包名、標題、內容文字
    private static String direction="無";
    private static final byte[] resolution={36,48,72,90,95,113,120,126};
    private static final byte[] value={0,0,0,0,0,113,0,0};
    private static final byte[][] feature={{28,35},{30,30},{28,29},{28,31},{32,33},{6,9},{33,30},{28,39}};
    private static int dirCentimeter;

    //接收資料
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void show(String packageName, String title, String text, Icon large, Context ct)
    {

        int Cont=0;

        bitmapIcon = large;
        Bitmap bi;
        int index;
        byte[] biArray;
        byte bitmapW=0;
        int dis=-1;
        String unit="";

        title=title.replaceAll("-.*", "").trim();

        //distance catch
        Pattern patternDis=Pattern.compile("^\\d+");
        Matcher matcherDis=patternDis.matcher(title);
        if(matcherDis.find())
            dis=Integer.parseInt(matcherDis.group().trim());

        //distance unit catch
       /* Pattern patternUnit=Pattern.compile("\\W$");
        Matcher matcherUnit=patternUnit.matcher(title);
        if(matcherUnit.find())
            unit=matcherUnit.group();*/
        unit=title.substring(title.length()-1,title.length());


        try
        {
            bi=drawableToBitmap (bitmapIcon.loadDrawable(ct));
            bitmapW=(byte)bi.getWidth();
            index=find(bitmapW);
            biArray=getBytesByBitmap(bi);


            for(int i=0;i<biArray.length;i++)
                if(biArray[i]==value[index])Cont++;

            if(dirStatus==noDir)
                if(Cont==feature[index][0])
                    direction = "右";
                else if (Cont==feature[index][1])
                    direction = "左";

            if(unit.equals("尺") && dis>0 && dis<=dirCentimeter && dirStatus==noDir)
            {
                if(Cont==feature[index][0])
                {
                    direction = "右";
                    dirStatus=right;
                    send(new byte[]{0});
                }
                else if (Cont==feature[index][1]) {
                    direction = "左";
                    dirStatus = left;
                    send(new byte[]{1});
                }
            }
            else if( (unit.equals("里") || dis>200 || dis<0) && (dirStatus==right || dirStatus==left))
            {
                send(new byte[]{2});
                dirStatus=noDir;
            }
        }
        catch (Exception e)
        {
            System.out.println("wait");
        }


        string = "\n\n" +
                "距離:" + title + "Unit"+ unit +"\n\n" +
                "下個轉彎方向:" + direction + "轉" + "Cont:" + Cont + " Resolution:" + bitmapW + "\n\n" +
                "到達時間:" + text + "\n\n";

        Log.d("google map",string);
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = Message.obtain();
                handler.sendMessage(msg);
            }
        }).start();*/
    }

    /*private static Handler handler = new Handler() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                //將資料顯示，更新至畫面
                textView.setText(string);
                largeIcon.setImageIcon(bitmapIcon);
            } catch (Exception e) {
            }
        }
    };*/
    private  static void send(byte[] i)
    {
        MyBluetoothService ii=new MyBluetoothService(MainService.getConnectThread());
        ii.write(i);
    }
    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
        {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        }
        else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
    public static byte[] getBytesByBitmap(Bitmap bitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bitmap.getAllocationByteCount());
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream.toByteArray();
    }
    public static byte find(byte i)
    {
        byte j;
        for(j=0;j<resolution.length;j++)
            if(i==resolution[j])
                break;
        return j;
    }
    public static void setDirCentimeter(int dirCentimeter)
    {
        NotificationCatchForGoogleMap.dirCentimeter=dirCentimeter;
    }
}
