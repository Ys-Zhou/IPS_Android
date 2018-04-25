package me.yukikari.ips_android;

import android.annotation.SuppressLint;
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
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    // Handlers
    private static Handler iniHandler;
    static Handler viewHandler;
    static Handler ctrlHandler;

    // May be used in other classes
    static String androidID;
    static ArrayList<String> macList;
    static int dir = 0;
    static String flag;

    // Variables in updating UI
    private LinearLayout upperContentView;
    private ArrayList<String> createdMac;

    // Buttons
    Button startButton;
    Button stopButton;

    // Service intent
    private Intent markerInt;

    @Override
    @SuppressLint("HardwareIds")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handlers
        iniHandler = new IniHandler(this);
        viewHandler = new ViewHandler(this);
        ctrlHandler = new CtrlHandler(this);

        // UI
        upperContentView = findViewById(R.id.viewfiled);
        createdMac = new ArrayList<>();

        //Service
        androidID = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        macList = new ArrayList<>();
        markerInt = new Intent(this, MarkerCom.class);

        // First step of initialization
        ProgressBar pb = new ProgressBar(this);
        upperContentView.addView(pb);
        checkInternet();

        final EditText flagText = findViewById(R.id.flagText);

        startButton = findViewById(R.id.button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = flagText.getText().toString();
                if (!input.equals("")) {
                    flagText.setEnabled(false);
                    startButton.setEnabled(false);
                    flag = input;
                    dir = 1;
                    stopButton.setEnabled(true);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Flag cannot be empty.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        });

        stopButton = findViewById(R.id.button2);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopButton.setEnabled(false);
                flag = null;
                dir = 0;
                createdMac.clear();
                upperContentView.removeAllViews();
                flagText.setEnabled(true);
                startButton.setEnabled(true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (flag != null) {
            dir = 1;
        }
    }

    @Override
    protected void onPause() {
        dir = 0;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Stop service
        stopService(markerInt);
        super.onDestroy();
    }

    private void popAlert(String text) {
        // Stop service
        stopService(markerInt);
        // Pop dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(text);
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MainActivity.this.finish();
            }
        });
        builder.show();
    }

    // Ini step 1: Check Internet
    private void checkInternet() {
        if (hasInternet()) {
            checkBluetooth();
        } else {
            popAlert("Cannot connect to Internet.");
        }
    }

    // Ini step 1.1: Check Internet method
    private boolean hasInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    // Ini step 2: Check Bluetooth (in new thread)
    private void checkBluetooth() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = 2;
                if (openBluetooth()) {
                    msg.arg1 = 1;
                } else {
                    msg.arg1 = 0;
                }
                MainActivity.iniHandler.sendMessage(msg);
            }
        });
        thread.start();
    }

    // Ini step 2.1: Check & open Bluetooth method
    private boolean openBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return false;
        }
        if (!adapter.isEnabled()) {
            adapter.enable();
        }
        int times = 0;
        while (!adapter.isEnabled() && times < 10) {
            try {
                Thread.sleep(500);
                times++;
            } catch (InterruptedException e) {
                break;
            }
        }
        return adapter.isEnabled();
    }

    // Handle thread message
    private static class IniHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        IniHandler(MainActivity mainActivityInstance) {
            weakReference = new WeakReference<>(mainActivityInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = weakReference.get();
            super.handleMessage(msg);
            if (mainActivity != null) {
                switch (msg.what) {
                    case 2:
                        if (msg.arg1 == 0) {
                            mainActivity.popAlert("Cannot open Bluetooth adapter.");
                        } else if (msg.arg1 == 1) {
                            mainActivity.getMacList();
                        }
                        break;
                }
            }
        }
    }

    // Ini step 3: Get beacon MAC list
    private void getMacList() {

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        client.post(getResources().getString(R.string.server_context) + "GetMacList", params, mTextHttpResponseHandler);
    }

    // Ini step 3.1: Handle Http Response
    private TextHttpResponseHandler mTextHttpResponseHandler = new TextHttpResponseHandler() {

        @Override
        public void onSuccess(int statusCode, Header[] headers, String response) {
            try {
                JSONArray macArray = new JSONArray(response);
                for (int i = 0; i < macArray.length(); i++) {
                    macList.add(macArray.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Ini step 4: Start service
            startService(markerInt);
            upperContentView.removeViewAt(0);
            startButton.setEnabled(true);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String response, Throwable error) {
            popAlert("Cannot get beacon list.");
        }
    };

    // Handle UI update messages
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
                try {
                    JSONObject deviceInfo = (JSONObject) msg.obj;
                    mainActivity.updateUI(deviceInfo);
                } catch (JSONException e) {
                    e.printStackTrace();
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

    // Handle error messages
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
                    mainActivity.popAlert(String.format(Locale.getDefault(), "Cannot upload data.(%d)", msg.arg1));
                }
            }
        }
    }
}