package com.kenji.power;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

public class TurnScreenOnActivity extends Activity {

	Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		turnScreenOn();

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				finish();
			}
		}, 300);

	}

	private void turnScreenOn() {
		// TODO Auto-generated method stub
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
	}
}
