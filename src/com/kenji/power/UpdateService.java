package com.kenji.power;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

public class UpdateService extends Service {

	
	
	@Override
    public void onCreate() {
        super.onCreate();
        // REGISTER RECEIVER THAT HANDLES SCREEN ON AND SCREEN OFF LOGIC
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
//        Toast.makeText(getApplicationContext(), "YAYA", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        boolean screenOn = intent.getBooleanExtra("screen_state", false);
        if (screenOn) {
            // YOUR CODE
        	Toast.makeText(getApplicationContext(), "ScreenOn", Toast.LENGTH_SHORT).show();
        } else {
            // YOUR CODE
//        	Toast.makeText(getApplicationContext(), "ScreenOff", Toast.LENGTH_SHORT).show();
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
