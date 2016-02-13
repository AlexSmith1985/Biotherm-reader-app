package me.cyberise.biothem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Created by mira on 1/10/15.
 */
public class BluetoothConnection{
    private static BluetoothConnection connection = null;
    public static MainActivity ui = null;
    OutputStream outStream = null;
    String TAG = "biotherm";
    public static String currentValue = "";
    public static InputStream inStream = null;
    private Handler mHandler = new Handler();
    //private btMonitor connectionMonitor;
    private Thread btThread;
    UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothSocket btSocket = null;
    BluetoothDevice reader = null;
    BackgroundThread backgroundThread;

    boolean connected = false;
    public static BluetoothConnection getConnection(){
        if(connection == null){
            connection = new BluetoothConnection();

        }
        return connection;
    }
    private BluetoothConnection(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            //Exit("This device doesn't have bluetooth, this app will not work");
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

// If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().trim().contentEquals(ui.bt_name)){
                    reader = device;
                }
            }
        }
        if (reader == null){
            //Exit("Reader is not paired");
            Toast toast = Toast.makeText(ui.getApplicationContext(), "Could not find device, did you change the name?", Toast.LENGTH_LONG);
            toast.show();

        }


        connect();

    }




    public class BackgroundThread extends Thread {

        boolean running = false;

        void setRunning(boolean b){
            running = b;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            //super.run();
            String data = "";
            while(running){
                //sleep(10000);
                try {

                    do{
                        int result = inStream.read();
                        data += ((char)result);
                        if(data.endsWith("\n")){
                            break;
                        }

                    } while(inStream.available()>0);
                    if(data.endsWith("\n")) {
                        currentValue = data;
                        Log.d(TAG, "read: " + data);
                        data = "";
                    }
                    currentValue = currentValue.replaceAll("[^\\x00-\\x7F]", "");
                } catch (Exception e) {
                    connected = false;
                    e.printStackTrace();
                    connect();
                }
                handler.sendMessage(handler.obtainMessage());
            }
        }

    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message inputMessage) {
            try {
                TextView statusField = ((TextView)ui.findViewById(R.id.connectionStatus));
                if (connected){
                    statusField.setText(R.string.connected);
                    statusField.setTextColor(Color.GREEN);

                } else {
                    statusField.setText(R.string.disconnected);
                    statusField.setTextColor(Color.RED);
                }
                if(ui.logging_preference && currentValue.length()>0){
                    String filename = Environment.getExternalStorageDirectory()+"/implant.log";
                    File yourFile = new File(filename);
                    if(!yourFile.exists()) {
                        yourFile.createNewFile();
                    }
                    FileWriter fw = new FileWriter(yourFile,true);
                    String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                    fw.write((currentDateTimeString+":" + currentValue+"\n"));
                    fw.close();
                }
                TextView valueField = ((TextView)ui.findViewById(R.id.tempText));
                valueField.setText(currentValue);
                //currentValue="";
            } catch (Exception e){
                //not able to update status, probably UI is closed
                int i = 0;
            }

        }

    };
   private void connect(){
        try {
            btSocket = reader.createRfcommSocketToServiceRecord(MY_UUID);

        } catch (Exception e) {
            //odd not sure what happened, try again
        }

        Log.d(TAG, "...Connecting to Remote...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection established and data link opened...");
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();
        } catch (Exception e) {
            try {
                btSocket.close();
            } catch (Exception e2) {
               // Exit("In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
           // mHandler.postDelayed(this, 1000);
            return;
        }

        if (outStream != null) {
            try {
                outStream.write("test".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
       connected = true;
       handler.sendMessage(handler.obtainMessage());
       if(backgroundThread==null) {
           backgroundThread = new BackgroundThread();
           backgroundThread.setRunning(true);
           backgroundThread.start();
       }
    }

    private void Exit(String message){
        if (ui != null){
            ui.Exit(message);
        }
    }


}
