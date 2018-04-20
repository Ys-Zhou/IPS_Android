package me.yukikari.ips_android;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static String androidID;
    static Handler viewHandler;
    static Handler ctrlHandler;

    // Variables in updating UI
    private LinearLayout upperContentView;
    private ArrayList<String> createdMac;
    private boolean nowRunning = false;
    private boolean noErr = true;

    // Other intents
    private Intent markerInt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ID
        androidID = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // Handlers
        viewHandler = new ViewHandler(this);
        ctrlHandler = new CtrlHandler(this);

        // UI
        upperContentView = findViewById(R.id.viewfiled);
        createdMac = new ArrayList<>();

        //Open Bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            popAlert("Cannot use Bluetooth service.");
        } else if (!adapter.isEnabled()) {
            adapter.enable();
        }

        //Check connection to Internet
        if (!isWiFi() && !isMobile()) {
            popAlert("Cannot connect to Internet.");
        }

        //Wait Bluetooth adapter ready
        int times = 0;
        while (!adapter.isEnabled() && times < 20) {
            try {
                Thread.sleep(500);
                times++;
            } catch (InterruptedException e) {
                popAlert("Cannot use Bluetooth service.(0)");
            }
        }
        if (!adapter.isEnabled()) {
            popAlert("Cannot use Bluetooth service.(1)");
        }

        // Start MarkerCom as a service
        if (noErr) {
            markerInt = new Intent(this, MarkerCom.class);
            startService(markerInt);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MarkerCom.dirHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            MarkerCom.dirHandler.sendMessage(msg);
        }
        nowRunning = true;
    }

    @Override
    protected void onPause() {
        nowRunning = false;
        if (MarkerCom.dirHandler != null) {
            Message msg = new Message();
            msg.arg1 = 0;
            MarkerCom.dirHandler.sendMessage(msg);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Stop MarkerCom
        stopService(markerInt);
        super.onDestroy();
    }

    //Check WiFi method
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

    //Check 4G method
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

    private void popAlert(String text) {
        // Stop handle messages and Bluetooth service
        noErr = false;
        // Pop dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Alert!");
        builder.setMessage(text);
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MainActivity.this.finish();
            }
        });
        builder.show();
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
                if (mainActivity.nowRunning && mainActivity.noErr) {
                    try {
                        JSONObject deviceInfo = (JSONObject) msg.obj;
                        mainActivity.updateUI(deviceInfo);
                    } catch (JSONException e) {
                        mainActivity.popAlert("Cannot update UI.");
                    }
                }
            }
        }
    }

    private static class CtrlHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        CtrlHandler(MainActivity mainActivityInstance) {
            weakReference = new WeakReference<>(mainActivityInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = weakReference.get();
            super.handleMessage(msg);
            if (mainActivity != null) {
                if (mainActivity.nowRunning && mainActivity.noErr) {
                    if (msg.what == 0) {
                        mainActivity.popAlert(String.format(Locale.getDefault(), "Cannot upload data.(%d)", msg.arg1));
                    }
                }
            }
        }
    }

    private void updateUI(JSONObject deviceInfo) throws JSONException {
        // Get info
        final String mac = deviceInfo.getString("mac");
        String rssi = deviceInfo.getString("rssi");
        String lastUpdate = deviceInfo.getString("lastUpdate");

        int viewIndex = createdMac.indexOf(mac);
        if (viewIndex == -1) {
            // Create
            // Second layout
            LinearLayout second = new LinearLayout(this);
            LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            secondParams.setMargins(0, 0, 0, 24);
            second.setBackgroundColor(Color.parseColor("#cccccc"));
            second.setOrientation(LinearLayout.VERTICAL);
            second.setLayoutParams(secondParams);

            // Texts
            TextView text1 = new TextView(this);
            text1.setText(String.format(Locale.getDefault(), "MAC: %s", mac));
            TextView text2 = new TextView(this);
            text2.setText(String.format(Locale.getDefault(), "RSSI: %s", rssi));
            TextView text3 = new TextView(this);
            text3.setText(String.format(Locale.getDefault(), "Time: %s", lastUpdate));
            second.addView(text1);
            second.addView(text2);
            second.addView(text3);

            // onClick
            second.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent jumpToDetail = new Intent(MainActivity.this, DetailActivity.class);
                    jumpToDetail.putExtra("MAC", mac);
                    startActivity(jumpToDetail);
                }
            });

            createdMac.add(mac);
            upperContentView.addView(second);
        } else {
            // Update
            LinearLayout second = (LinearLayout) upperContentView.getChildAt(viewIndex);
            TextView text2 = (TextView) second.getChildAt(1);
            text2.setText(String.format(Locale.getDefault(), "RSSI: %s", rssi));
            TextView text3 = (TextView) second.getChildAt(2);
            text3.setText(String.format(Locale.getDefault(), "Time: %s", lastUpdate));
        }
    }
}