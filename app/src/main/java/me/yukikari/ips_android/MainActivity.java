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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
//import java.util.LinkedList;
import java.util.Locale;

import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {

    static Handler viewHandler;
    static String serialNumber;

    private LinearLayout upperContentView;
    //    private LinkedList<String> markerIdList;
    private JSONObject detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewHandler = new ViewHandler(this);
        serialNumber = Build.SERIAL;

        upperContentView = (LinearLayout) findViewById(R.id.viewfiled);
//        markerIdList = new LinkedList<>();
        detail = new JSONObject();

        //Open bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!adapter.isEnabled()) {
            adapter.enable();
        }

        //Check the connection to server
        if (!isWiFi() && !isMobile()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Alert!");
            builder.setMessage("Cannot connect to Internet.");
            builder.setNegativeButton("OK", null);
            builder.show();
        }

        MarkerDesp markerDesp = new MarkerDesp(new ConnectionHandler(this));
        markerDesp.getDetail();

        //Check Bluetooth
        while (!adapter.isEnabled() || detail == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Intent intent = new Intent(this, MarkerCom.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, MarkerCom.class);
        stopService(intent);
        super.onDestroy();
    }

    //Check WiFi
    private boolean isWiFi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo;
        try {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (Exception e) {
            return false;
        }
        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    //Check 4G
    private boolean isMobile() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo;
        try {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (Exception e) {
            return false;
        }
        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    private static class ConnectionHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        ConnectionHandler(MainActivity mainActivityInstance) {
            weakReference = new WeakReference<>(mainActivityInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = weakReference.get();
            super.handleMessage(msg);
            if (mainActivity != null) {
                switch (msg.what) {
                    case 0:
                        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                        builder.setTitle("Alert!");
                        builder.setMessage("Cannot connect to server.");
                        builder.setNegativeButton("OK", null);
                        builder.show();
                        break;
                    case 1:
                        mainActivity.detail = (JSONObject) msg.obj;
                        break;
                }
            }
        }
    }

    private static class ViewHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        ViewHandler(MainActivity mainActivityInstance) {
            weakReference = new WeakReference<>(mainActivityInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = weakReference.get();
            super.handleMessage(msg);
            if (mainActivity != null) {
                String markerId = (String) msg.obj;
                switch (msg.what) {
                    case 0:
                        //mainActivity.deleteView(markerId);
                        mainActivity.createView(markerId, 0);
                        break;
                    case 1:
                        //mainActivity.createView(markerId);
                        mainActivity.createView(markerId, 1);
                        break;
                }
            }
        }
    }

    private void createView(String markerId, int type) {
//        int index = markerIdList.indexOf(markerId);
//        if (index != -1) return;

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

        //Current time
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);
        Date cDate = new Date(System.currentTimeMillis());
        String date = df.format(cDate);

        int width = upperContentView.getWidth();
        int height = (int) (width * 0.4);

        //thirdLayout
        LinearLayout thirdLayout = new LinearLayout(this);
        thirdLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams thirdLayoutParams = new LinearLayout.LayoutParams(
                height, height);
        thirdLayout.setLayoutParams(thirdLayoutParams);

        //secondLayout
        LinearLayout secondLayout = new LinearLayout(this);
        secondLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams secondLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height);
        secondLayoutParams.setMargins(0, 16, 0, 16);
        secondLayout.setLayoutParams(secondLayoutParams);

        //stateImageView
        ImageView stateImageView = new ImageView(this);
        if (type == 1) {
            stateImageView.setImageResource(R.drawable.enter);
        } else {
            stateImageView.setImageResource(R.drawable.exit);
        }
        LinearLayout.LayoutParams stateImageViewLayoutParams = new LinearLayout.LayoutParams(
                (int) (height * 0.7), (int) (height * 0.7));
        stateImageView.setLayoutParams(stateImageViewLayoutParams);

        //textView
        TextView textView1 = new TextView(this);
        textView1.setText(desc);
        textView1.setTextColor(Color.BLACK);

        TextView textView2 = new TextView(this);
        textView2.setText(date);

        //descImageView
        ImageView descImageView = new ImageView(this);
        descImageView.setImageResource(Info.getResId(desc));

        LinearLayout.LayoutParams descImageViewLayoutParams = new LinearLayout.LayoutParams(width - height, height);
        descImageView.setLayoutParams(descImageViewLayoutParams);

        //Set OnClick
        secondLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WebActivity.class);
                intent.putExtra("url", fUrl);
                startActivity(intent);
            }
        });

//        thirdLayout.addView(stateImageView);
        thirdLayout.addView(textView1);
        thirdLayout.addView(textView2);

        secondLayout.addView(descImageView);
        secondLayout.addView(thirdLayout);

//        markerIdList.add(markerId);
        upperContentView.addView(secondLayout);
    }

//    private void deleteView(String markerId) {
//        int index = markerIdList.indexOf(markerId);
//        if (index == -1) return;
//        markerIdList.remove(index);
//        upperContentView.removeViewAt(index);
//    }
}