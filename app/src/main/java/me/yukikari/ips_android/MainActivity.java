package me.yukikari.ips_android;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static Handler viewHandler;
    static String serialNumber;

    // Variables in updating UI
    private LinearLayout upperContentView;
    private boolean isFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewHandler = new ViewHandler(this);
        serialNumber = Build.SERIAL;
        upperContentView = findViewById(R.id.viewfiled);

        //Open Bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!adapter.isEnabled()) {
            adapter.enable();
        }

        //Check connection to server
        if (!isWiFi() && !isMobile()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Alert!");
            builder.setMessage("Cannot connect to Internet.");
            builder.setNegativeButton("OK", null);
            builder.show();
        }

        //Wait Bluetooth adapter ready
        while (!adapter.isEnabled()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                HashMap<String, Integer> mDevices = (HashMap<String, Integer>) msg.obj;
                mainActivity.updateUI(mDevices);
            }
        }
    }

    private void updateUI(HashMap<String, Integer> mDevices) {
        if (isFirst) {
            isFirst = false;
        } else {
            upperContentView.removeViewAt(0);
        }

        // Table
        TableLayout table = new TableLayout(this);
        TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT);
        table.setLayoutParams(tableParams);

        // Rows
        for (HashMap.Entry<String, Integer> entry : mDevices.entrySet()) {
            TableRow row = new TableRow(this);
            // Cols
            TextView col1 = new TextView(this);
            col1.setText(entry.getKey());
            TextView col2 = new TextView(this);
            col2.setText(String.format(Locale.getDefault(), "\t\t%d", entry.getValue()));
            row.addView(col1);
            row.addView(col2);
            table.addView(row);
        }
        upperContentView.addView(table);
    }
}