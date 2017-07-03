package me.yukikari.ips_android;

import android.os.Handler;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

import org.json.JSONObject;

public class MarkerDesp {

    private Handler handler;

    public MarkerDesp(Handler iHandler) {
        handler = iHandler;
    }

    public void getDetail() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        client.post("http://192.168.1.100:8080/IPS/GetDetail", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Message msg = new Message();
                try {
                    String str = new String(response, "UTF-8");
                    JSONObject detail = new JSONObject(str);
                    msg.obj = detail;
                    msg.what = 1;
                } catch (Exception e) {
                    msg.what = 0;
                }
                handler.sendMessage(msg);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] response, Throwable error) {
                Message msg = new Message();
                msg.what = 0;
                handler.sendMessage(msg);
            }
        });
    }
}