package com.kenji.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenReceiver extends BroadcastReceiver {

	private boolean screenOn;
	private boolean isFirst = true;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub

		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON) && isFirst) {
            screenOn = true;
            isFirst = false;
            
            Intent i = new Intent(context, UpdateService.class);
            i.putExtra("screen_state", screenOn);
            context.startService(i);
            
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screenOn = false;
        }
        
		
	}

}
