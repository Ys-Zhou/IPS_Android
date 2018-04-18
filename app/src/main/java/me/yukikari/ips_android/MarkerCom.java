package me.yukikari.ips_android;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import cz.msebera.android.httpclient.Header;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class MarkerCom extends Service {

    // Upload frequency (Unit: second)
    private int upFrq = 15;
    // Data will not be upload if the length of array is less tha this
    private int lim = 3;

    // For thread safety, do not change these in any case
    private boolean exit = false;
    private static ReentrantLock lock = new ReentrantLock();

    // Bluetooth Adapter Instance
    private BluetoothAdapter mBluetoothAdapter;

    // Device sets
    private HashMap<String, JSONObject> mDevices = new HashMap<>(); //For updating UI
    private HashMap<String, ArrayList<Integer>> deviceData = new HashMap<>(); //For calculating average RSSI

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        startUpload();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        exit = true;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Callback: Scan devices
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            JSONObject json = new JSONObject();
            try {
                json.put("rssi", rssi);
                json.put("lastUpdate", getCurTime());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //update device status
            //For updating UI
            mDevices.put(device.getAddress(), json);

            //For calculating average RSSI
            lock.lock();
            try {
                addData(device.getAddress(), rssi);
            } finally {
                lock.unlock();
            }

            //Send message to update UI
            Message msg = new Message();
            msg.obj = mDevices;
            MainActivity.viewHandler.sendMessage(msg);
        }
    };

    // Method: Get current time
    private String getCurTime() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate);
    }

    // Method: Add data to device set
    private void addData(String mac, int rssi) {

        ArrayList<Integer> rssiList;

        if (deviceData.containsKey(mac)) {
            rssiList = deviceData.get(mac);
        } else {
            rssiList = new ArrayList<>();
        }

        rssiList.add(rssi);
        deviceData.put(mac, rssiList);
    }

    private void startUpload() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!exit) {
                        Thread.sleep(1000 * upFrq);
                        lock.lock();
                        try {
                            for (HashMap.Entry<String, ArrayList<Integer>> entry : deviceData.entrySet()) {
                                if (entry.getValue().size() >= lim) {
                                    int sumRssi = 0;
                                    for (int rssi : entry.getValue()) {
                                        sumRssi += rssi;
                                    }
                                    uploadData(entry.getKey(), sumRssi / entry.getValue().size());
                                }
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
//                    Message msg = new Message();
//                    msg.what = 0;
//                    MainActivity.ctrlHandler.sendMessage(msg);
                }
            }
        });
        thread.start();
    }

    private void uploadData(String mac, int rssi) {

        JSONObject jsonIn = new JSONObject();
        try {
            jsonIn.put("androidID", MainActivity.androidID);
            jsonIn.put("markerMac", mac);
            jsonIn.put("date", getCurTime());
            jsonIn.put("rssi", rssi);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("jsonIn", jsonIn.toString());
        client.post(Info.ipAddr + "/IPS_Server/UploadData", params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] response, Throwable error) {
//                Message msg = new Message();
//                msg.what = 0;
//                MainActivity.ctrlHandler.sendMessage(msg);
            }
        });
    }
}