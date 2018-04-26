package me.yukikari.ips_android;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

import org.json.JSONArray;
import org.json.JSONException;
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

    // Thread sync
    private boolean exit = false;
    private ReentrantLock lock;

    // Bluetooth Adapter Instance
    private BluetoothAdapter mBluetoothAdapter;

    static String destMac;
    private boolean testMode = false;

    // Device set
    private HashMap<String, ArrayList<Integer>> deviceData;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lock = new ReentrantLock();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceData = new HashMap<>();
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

            String mac = device.getAddress();
            //For updating UI
            if (MainActivity.dir == 1 && (MainActivity.macList.contains(mac) || testMode)) {
                sendInfo(mac, rssi);
            } else if (MainActivity.dir == 2 && destMac.equals(mac)) {
                Message msg = new Message();
                msg.arg1 = rssi;
                DetailActivity.viewHandler.sendMessage(msg);
            }

            //For calculating average RSSI
            lock.lock();
            try {
                if (MainActivity.flag != null && MainActivity.macList.contains(mac)) {
                    addData(mac, rssi);
                }
            } finally {
                lock.unlock();
            }
        }
    };

    // Method: Get current time
    private String getCurTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return formatter.format(new Date());
    }

    // Method: Send message to update UI
    private void sendInfo(String mac, int rssi) {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.put("mac", mac);
            deviceInfo.put("rssi", rssi);
            deviceInfo.put("lastUpdate", getCurTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Message msg = new Message();
        msg.obj = deviceInfo;
        MainActivity.viewHandler.sendMessage(msg);
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

    // Method: Check device set and upload data (Async)
    private void startUpload() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!exit) {
                        Thread.sleep(1000 * upFrq);
                        lock.lock();
                        try {
                            if (MainActivity.flag != null) {
                                uploadData();
                                deviceData.clear();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Message msg = new Message();
                    msg.what = 100;
                    msg.arg1 = 1;
                    MainActivity.ctrlHandler.sendMessage(msg);
                }
            }
        });
        thread.start();
    }

    // Method: Http Request (Async)
    private void uploadData() throws JSONException {
        JSONObject jsonIn = new JSONObject();
        jsonIn.put("androidID", MainActivity.androidID);
        jsonIn.put("date", getCurTime());
        jsonIn.put("flag", MainActivity.flag);

        JSONArray beaconArray = new JSONArray();
        for (HashMap.Entry<String, ArrayList<Integer>> entry : deviceData.entrySet()) {
            JSONObject beacon = new JSONObject();
            beacon.put("mac", entry.getKey());
            JSONArray rssis = new JSONArray(entry.getValue());
            beacon.put("rssis", rssis);
            beaconArray.put(beacon);
        }
        jsonIn.put("beacons", beaconArray);

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("jsonIn", jsonIn.toString());
        client.post(getResources().getString(R.string.server_context) + "UploadData", params, mTextHttpResponseHandler);
    }

    // Handler: Http Response
    private static TextHttpResponseHandler mTextHttpResponseHandler = new TextHttpResponseHandler() {

        @Override
        public void onSuccess(int statusCode, Header[] headers, String response) {


            try {
                JSONObject jsonOut = new JSONObject(response);
                int requestStat = jsonOut.getInt("addLogStmt");
                if (requestStat == 100) {
                    Message msg = new Message();
                    msg.what = 100;
                    msg.arg1 = statusCode;
                    MainActivity.ctrlHandler.sendMessage(msg);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String response, Throwable error) {
            Message msg = new Message();
            msg.what = 100;
            msg.arg1 = statusCode;
            MainActivity.ctrlHandler.sendMessage(msg);
        }
    };
}