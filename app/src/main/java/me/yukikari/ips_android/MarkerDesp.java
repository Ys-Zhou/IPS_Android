package me.yukikari.ips_android;

import android.os.Handler;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

import org.json.JSONObject;

class MarkerDesp {

    private Handler handler;
    private String server_ip = "";

    MarkerDesp(Handler iHandler) {
        handler = iHandler;
    }

    void getDetail() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        client.post(ServerInfo.ipAddr + "/IPS_Server/GetDetail", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Message msg = new Message();
                try {
                    String str = new String(response, "UTF-8");
                    msg.obj = new JSONObject(str);
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