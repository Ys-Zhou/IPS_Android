package me.yukikari.ips_android;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class MarkerCom extends Service {
    // Upload frequency (Unit: second)
    private int upFrq = 15;
    // Data will not be upload if the number of data is less than this
    private int lim = 3;

    // Thread sync
    private boolean exit = false;
    private ReentrantLock lock;

    // Bluetooth Adapter Instance
    private BluetoothAdapter mBluetoothAdapter;

    // Handler: DirHandler
    static Handler dirHandler;
    private int dir = 1;
    private String filter;

    @Override
    public void onCreate() {
        super.onCreate();
        dirHandler = new DirHandler(this);
        lock = new ReentrantLock();
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

    // Device set
    private HashMap<String, ArrayList<Integer>> deviceData = new HashMap<>();

    // Callback: Scan devices
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            String mac = device.getAddress();
            //For updating UI
            if (dir == 1 && MainActivity.macList.contains(mac)) {
                sendInfo(mac, rssi);
            } else if (dir == 2 && filter.equals(mac)) {
                Message msg = new Message();
                msg.arg1 = rssi;
                DetailActivity.viewHandler.sendMessage(msg);
            }

            //For calculating average RSSI
            lock.lock();
            try {
                addData(mac, rssi);
            } finally {
                lock.unlock();
            }
        }
    };

    // Method: Get current time
    private String getCurTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate);
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
                    Message msg = new Message();
                    msg.what = 0;
                    msg.arg1 = 0;
                    MainActivity.ctrlHandler.sendMessage(msg);
                }
            }
        });
        thread.start();
    }

    // Method: Http Request (Async)
    private void uploadData(String mac, int rssi) {

        JSONObject jsonIn = new JSONObject();
        try {
            jsonIn.put("androidID", MainActivity.androidID);
            jsonIn.put("markerMac", mac);
            jsonIn.put("date", getCurTime());
            jsonIn.put("rssi", rssi);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("jsonIn", jsonIn.toString());
        client.post(Info.ipAddr + "/IPS_Server/UploadData", params, mTextHttpResponseHandler);
    }

    // Handler: Http Response
    private TextHttpResponseHandler mTextHttpResponseHandler = new TextHttpResponseHandler() {

        @Override
        public void onSuccess(int statusCode, Header[] headers, String response) {

            Message msg = new Message();
            try {
                JSONObject jsonOut = new JSONObject(response);
                int requestStat = jsonOut.getInt("addLogStmt");
                if (requestStat == 0) {
                    msg.what = 0;
                    msg.arg1 = statusCode;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MainActivity.ctrlHandler.sendMessage(msg);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String response, Throwable error) {
            Message msg = new Message();
            msg.what = 0;
            msg.arg1 = statusCode;
            MainActivity.ctrlHandler.sendMessage(msg);
        }
    };

    // Handler: Message sending direction
    private static class DirHandler extends Handler {
        private final WeakReference<MarkerCom> weakReference;

        DirHandler(MarkerCom markerComInstance) {
            weakReference = new WeakReference<>(markerComInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            MarkerCom markerCom = weakReference.get();
            super.handleMessage(msg);
            if (markerCom != null) {
                markerCom.dir = msg.arg1;
                markerCom.filter = (String) msg.obj;
            }
        }
    }
}