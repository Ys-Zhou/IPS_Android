package me.yukikari.ips_android;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.uctec.ucdroid.util.ble.BleUcodeManager;

import cz.msebera.android.httpclient.Header;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MarkerCom {

    private BleUcodeManager mBleUcodeMgr;
    private Context applicationContext;
    private Handler handler;
    private String serialNumber;

    private static final String TAGS_APIKEY = null;
    private static final String HEXES = "0123456789ABCDEF";

    public MarkerCom(Context iApplicationContext, Handler iHandler, String iSerialNumber) {
        applicationContext = iApplicationContext;
        handler = iHandler;
        serialNumber = iSerialNumber;
    }

    public void start() {
        mBleUcodeMgr = new BleUcodeManager(applicationContext, TAGS_APIKEY);

        // Set default communicating level for unknown markers
        mBleUcodeMgr.setDefaultDistance(0.5, 5);
        mBleUcodeMgr.setDefaultDistance(1.0, 4);
        mBleUcodeMgr.setDefaultDistance(2.0, 3);
        mBleUcodeMgr.setDefaultDistance(4.0, 2);
        mBleUcodeMgr.setDefaultDistance(6.0, 1);
        mBleUcodeMgr.setDefaultDistance(8.0, 0);
        mBleUcodeMgr.registerListener(mBleUcodeManagerListener);
        mBleUcodeMgr.start();
    }

    public void stop() {
        mBleUcodeMgr.unregisterListener(mBleUcodeManagerListener);
        mBleUcodeMgr.stop();
    }

    private final BleUcodeManager.Listener mBleUcodeManagerListener = new BleUcodeManager.Listener() {
        @Override
        public void didChangeUcodeLevel(byte[] bucode, int from, int to) {

            //Current time
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);
            Date cDate = new Date(System.currentTimeMillis());
            String date = df.format(cDate);

            //Bluetooth marker ID
            String markerSer = byteArrayToString(bucode);
            String markerId = markerSer.substring(markerSer.length() - 5, markerSer.length());

            //upload data to server
            uploadData(markerId, date, from, to);

            //update UI
            Message msg = new Message();
            msg.obj = markerId;
            if (from < to && to >= 3) msg.what = 1;
            else if (from > to && to <= 2) msg.what = 0;
            handler.sendMessage(msg);
        }
    };

    private void uploadData(final String markerId, final String date, final int from, final int to) {
        JSONObject jsonIn = new JSONObject();
        try {
            jsonIn.put("userId", serialNumber);
            jsonIn.put("markerId", markerId);
            jsonIn.put("date", date);
            jsonIn.put("from", from);
            jsonIn.put("to", to);
        } catch (Exception e) {
            e.printStackTrace();
        }

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("jsonIn", jsonIn.toString());
        client.post("http://192.168.1.100:8080/IPS/UploadData", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                String str = "";
                try {
                    str = new String(response, "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] response, Throwable error) {
                System.out.println(statusCode);
            }
        });
    }

    private String byteArrayToString(byte[] byteArray) {
        final StringBuilder hex = new StringBuilder(32);
        for (final byte b : byteArray) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}