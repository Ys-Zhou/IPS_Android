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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static Handler viewHandler;
    static Handler ctrlHandler;
    static String androidID;

    // Variables in updating UI
    private LinearLayout upperContentView;
    private boolean isFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewHandler = new ViewHandler(this);
        ctrlHandler = new CtrlHandler(this);
        androidID = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        upperContentView = findViewById(R.id.viewfiled);

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
        while (!adapter.isEnabled() && times < 10) {
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
        Intent intent = new Intent(this, MarkerCom.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        // Stop MarkerCom
        Intent intent = new Intent(this, MarkerCom.class);
        stopService(intent);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                HashMap<String, JSONObject> mDevices = (HashMap<String, JSONObject>) msg.obj;
                try {
                    mainActivity.updateUI(mDevices);
                } catch (Exception e) {
                    mainActivity.popAlert("Cannot update UI.");
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
                if (msg.what == 0) {
                    //mainActivity.popAlert(String.format(Locale.getDefault(), "Cannot upload data.(%d)", msg.arg1));
                }
            }
        }
    }

    private void updateUI(HashMap<String, JSONObject> mDevices) throws JSONException {
        if (isFirst) {
            isFirst = false;
        } else {
            upperContentView.removeViewAt(0);
        }

        // First layout
        LinearLayout first = new LinearLayout(this);
        LinearLayout.LayoutParams firstParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        first.setOrientation(LinearLayout.VERTICAL);
        first.setLayoutParams(firstParams);

        for (HashMap.Entry<String, JSONObject> entry : mDevices.entrySet()) {
            // Second layout
            LinearLayout second = new LinearLayout(this);
            LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            secondParams.setMargins(0, 0, 0, 24);
            second.setBackgroundColor(Color.parseColor("#cccccc"));
            second.setOrientation(LinearLayout.VERTICAL);
            second.setLayoutParams(secondParams);

            // texts
            TextView text1 = new TextView(this);
            text1.setText(String.format(Locale.getDefault(), "MAC: %s", entry.getKey()));
            TextView text2 = new TextView(this);
            text2.setText(String.format(Locale.getDefault(), "RSSI: %s", entry.getValue().getString("rssi")));
            TextView text3 = new TextView(this);
            text3.setText(String.format(Locale.getDefault(), "Time: %s", entry.getValue().getString("lastUpdate")));
            second.addView(text1);
            second.addView(text2);
            second.addView(text3);

            // onClick
            second.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Jump
                }
            });

            first.addView(second);
        }
        upperContentView.addView(first);
    }
}