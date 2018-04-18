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

import cz.msebera.android.httpclient.Header;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MarkerCom extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Bluetooth Adapter Instance
    private BluetoothAdapter mBluetoothAdapter;

    // Map: <MAC, RSSI>
    private final HashMap<String, Integer> mDevices = new HashMap<>();

    // Create LeScanCallback
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            mDevices.put(device.getAddress(), rssi);

            //update UI
            Message msg = new Message();
            msg.obj = mDevices;
            MainActivity.viewHandler.sendMessage(msg);
        }
    };

//    private void uploadData(final String markerId, final String date, final int from, final int to) {
//        JSONObject jsonIn = new JSONObject();
//        try {
//            jsonIn.put("userId", MainActivity.serialNumber);
//            jsonIn.put("markerId", markerId);
//            jsonIn.put("date", date);
//            jsonIn.put("from", from);
//            jsonIn.put("to", to);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        AsyncHttpClient client = new AsyncHttpClient();
//        RequestParams params = new RequestParams();
//        params.put("jsonIn", jsonIn.toString());
//        client.post(Info.ipAddr + "/IPS_Server/UploadData", params, new AsyncHttpResponseHandler() {
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
//
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, byte[] response, Throwable error) {
//                System.out.println(statusCode);
//            }
//        });
//    }
}