package me.yukikari.ips_android;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {

    private MarkerCom markerCom;
    private LinearLayout upperContentView;
    private LinkedList<String> markerIdList;
    private JSONObject detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        upperContentView = (LinearLayout) findViewById(R.id.viewfiled);
        markerIdList = new LinkedList<>();
        detail = new JSONObject();
        String serialNumber = Build.SERIAL;

        //Open bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!adapter.isEnabled()) {
            adapter.enable();
        }

        //Connect server
        if (!isWiFi() && !isMobile()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Alert!");
            builder.setMessage("Cannot connect to Internet.");
            builder.setNegativeButton("OK", null);
            builder.show();
        }

        MarkerDesp markerDesp = new MarkerDesp(cHandler);
        markerDesp.getDetail();

        //Check
        while (!adapter.isEnabled() || detail.equals(null)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        markerCom = new MarkerCom(getApplicationContext(), handler, serialNumber);
        markerCom.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        markerCom.stop();
    }

    //WiFi
    public boolean isWiFi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    //4G
    public boolean isMobile() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return true;
        }
        return false;
    }

    private Handler cHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Alert!");
                    builder.setMessage("Cannot connect to server.");
                    builder.setNegativeButton("OK", null);
                    builder.show();
                    break;
                case 1:
                    detail = (JSONObject) msg.obj;
                    break;
            }
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String markerId = (String) msg.obj;
            switch (msg.what) {
                case 0:
                    deleteView(markerId);
                    break;
                case 1:
                    createView(markerId);
                    break;
            }
        }
    };

    private void createView(String markerId) {
        int index = markerIdList.indexOf(markerId);
        if (index != -1) return;

        String desc;
        String url;
        try {
            JSONArray array = detail.getJSONArray(markerId);
            desc = array.getString(0);
            url = array.getString(1);
        } catch (Exception e) {
            desc = "unknown";
            url = "about:blank";
        }
        final String fUrl = url;

        TextView textView = new TextView(MainActivity.this);
        textView.setText(desc);
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.BLUE);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WebActivity.class);
                intent.putExtra("url", fUrl);
                startActivity(intent);
            }
        });

        int width = upperContentView.getWidth();
        int height = (int) (width * 0.2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height);
        layoutParams.setMargins(0, 16, 0, 16);
        textView.setLayoutParams(layoutParams);

        markerIdList.add(markerId);
        upperContentView.addView(textView);
    }

    private void deleteView(String markerId) {
        int index = markerIdList.indexOf(markerId);
        if (index == -1) return;
        markerIdList.remove(index);
        upperContentView.removeViewAt(index);
    }
}