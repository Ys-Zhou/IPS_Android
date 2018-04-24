package me.yukikari.ips_android;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Chronometer;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    // Update UI
    static Handler viewHandler;

    // Variables in updating UI
    private int minRssi = 999;
    private int maxRssi = -999;
    private int sumRssi = 0;
    private int scanTimes = 0;

    // TextViews
    private TextView minText;
    private TextView maxText;
    private TextView avgText;
    private TextView scanText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Ini
        viewHandler = new ViewHandler(this);

        minText = findViewById(R.id.minText);
        maxText = findViewById(R.id.maxText);
        avgText = findViewById(R.id.avgText);
        scanText = findViewById(R.id.scanText);

        // Get parameter
        Intent intent = getIntent();
        String mac = intent.getStringExtra("MAC");

        // Set MAC
        TextView macText = findViewById(R.id.macText);
        macText.setText(mac);

        // Set chronometer
        Chronometer chronometer = findViewById(R.id.chronometer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        // Send message to Bluetooth service
        Message msg = new Message();
        msg.arg1 = 2;
        msg.obj = mac;
        MarkerCom.dirHandler.sendMessage(msg);
    }

    private void updateUI(int rssi) {
        if (rssi < minRssi) {
            minRssi = rssi;
            minText.setText(String.format(Locale.getDefault(), "%d", minRssi));
        }
        if (rssi > maxRssi) {
            maxRssi = rssi;
            maxText.setText(String.format(Locale.getDefault(), "%d", maxRssi));
        }
        sumRssi += rssi;
        scanTimes += 1;
        avgText.setText(String.format(Locale.getDefault(), "%d", sumRssi / scanTimes));
        scanText.setText(String.format(Locale.getDefault(), "%d", scanTimes));
    }

    private static class ViewHandler extends Handler {
        private final WeakReference<DetailActivity> weakReference;

        ViewHandler(DetailActivity detailActivityInstance) {
            weakReference = new WeakReference<>(detailActivityInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            DetailActivity detailActivity = weakReference.get();
            super.handleMessage(msg);
            if (detailActivity != null) {
                int rssi = msg.arg1;
                detailActivity.updateUI(rssi);
            }
        }
    }
}