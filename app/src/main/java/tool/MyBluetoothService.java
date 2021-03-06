package tool;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MyBluetoothService
{
    private static final String TAG = "MyBluetoothService";
    private Handler handler; // handler that gets info from Bluetooth service
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private Bundle message=new Bundle();
    private Intent intent=new Intent("MyBluetoothService");
    private boolean isConnected;

    public MyBluetoothService(ConnectThread connectThread)
    {
        mmSocket = connectThread.getMmSocket();
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = mmSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    private class readThread extends Thread
    {
        public void run()
        {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true)
            {
                try
                {
                    // Read from the InputStream.

                    numBytes = mmInStream.read(mmBuffer);
                    Log.e(TAG,String.valueOf(numBytes));
                    if(mmBuffer[0]==37)
                        isConnected=true;
                    else
                        isConnected=false;

                    // Send the obtained bytes to the UI activity.
                   /* Message readMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();*/
                }
                catch (IOException e)
                {
                    isConnected=false;
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
    }

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants
    {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }


    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes)
    {
        try
        {
            mmOutStream.write(bytes);
                /*--Share the sent message with the UI activity.--*/
                /*Message writtenMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, bytes);
                writtenMsg.sendToTarget();*/
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg = handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                        "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }

    public void enableReadData()
    {
        readThread i=new readThread();
        i.start();
    }
    public boolean isConnected()
    {
        return isConnected;
    }

    // Call this method from the main activity to shut down the connection.
    public void cancel()
    {
           try
           {
               mmSocket.close();
           }
           catch (IOException e)
           {
               Log.e(TAG, "Could not close the connect socket", e);
           }
    }
}
