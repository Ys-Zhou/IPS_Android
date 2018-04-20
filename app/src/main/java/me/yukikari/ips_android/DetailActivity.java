package me.yukikari.ips_android;

import android.content.Intent;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Get parameter
        Intent intent = getIntent();
        String mac = intent.getStringExtra("MAC");

        // Unchanged
        TextView macText = findViewById(R.id.macText);
        macText.setText(mac);

        // Send message to Bluetooth service
        Message msg = new Message();
        msg.arg1 = 2;
        msg.obj = mac;
        MarkerCom.dirHandler.sendMessage(msg);
    }
}
